# 📋 개발 인수인계 문서 (Handover Document - v1.1.0-dev)

본 문서는 **Simple Bullet Journal** 프로젝트의 아키텍처 설계 배경, 주요 구현 상세, 그리고 **v1.1+ 개발자가 즉시 작업을 이어갈 수 있도록 필요한 기술적 맥락과 로드맵**을 상세히 기술합니다.

> 2026-09-19 기준으로 실제 소스 코드(`app/src`)를 전수 대조하여 최신화했습니다. 이전 버전 문서에 있던 오래된 서술(DB v3, 별도 `RepositoryModule` 파일 등)은 실제 코드 기준으로 정정했습니다.

---

## 1. 아키텍처 개요 및 설계 원칙

### 1.1 계층 분리 (Clean MVVM)
* **Data 계층**: `Room` + `TaskRepository` 추상화
  * DAO를 직접 ViewModel에서 참조하지 않고 반드시 `TaskRepository` 인터페이스를 거칩니다.
  * 모든 쿼리는 날짜(`date`, `yyyy-MM-dd`) 기준으로 필터링되며, `isPriority DESC, orderIndex ASC, createdAt ASC` 순으로 정렬됩니다.
  * **설정값 저장(2026-09-20 추가)**: 구조화 데이터(Room)와 별도로 key-value 설정값은 `androidx.datastore:datastore-preferences` + `UserPreferencesRepository`(`UserPreferencesRepositoryImpl`)로 관리합니다. `UserPreferences(isAdRemoved, themeMode)`를 `Flow`로 노출하며, 8절 로드맵(광고 제거 상태, 테마 선택)의 저장소로 쓰일 예정입니다. 아직 ViewModel/UI 어디에서도 소비하지 않는 상태 — 다음 작업은 이 Repository를 실제로 주입해 쓰는 설정 화면입니다.
* **DI 계층**: `Google Hilt`
  * `DatabaseModule` 한 파일에서 `AppDatabase`, `TaskDao`, `TaskRepository`(`@Provides`로 `TaskRepositoryImpl` 반환)를 모두 제공합니다. (과거 문서에 있던 별도 `RepositoryModule`/`@Binds` 구조는 실제 코드에 없습니다.)
  * `DataStoreModule`에서 `DataStore<Preferences>`(`preferencesDataStore(name = "user_preferences")`)와 `UserPreferencesRepository`를 싱글톤으로 제공합니다.
* **Presentation 계층**: `Jetpack Compose` + `TaskViewModel`
  * `TaskViewModel`은 `@HiltViewModel` + `AndroidViewModel(application)`을 상속하며, 위젯 갱신을 위해 `GlanceAppWidgetManager`/`updateAll`을 호출합니다.
  * 단위 테스트 환경에서 Android Framework 의존성 충돌을 방지하기 위해 `widgetUpdater: suspend () -> Unit` 람다 프로퍼티를 주입 가능하게 설계하였습니다.
* **Widget 계층**: `Jetpack Glance`
  * 앱이 종료된 상태에서도 원격 뷰를 통해 데이터 조회/수정/이월이 가능하도록 `ToggleTaskAction`, `MigrateTasksAction`을 `ActionCallback`으로 구현했습니다.
  * ⚠️ 위젯 쪽은 Hilt 그래프를 타지 않습니다. `provideGlance`/`ToggleTaskAction`/`MigrateTasksAction` 각각에서 `TaskRepositoryImpl(AppDatabase.getInstance(context).taskDao())`를 직접 `new`합니다. 자세한 내용은 3절과 7절("확인된 이슈") 참고.

---

## 2. 데이터베이스 스키마 및 마이그레이션 현황

* **엔티티 (`Task.kt`)**:
  ```kotlin
  @Entity(tableName = "tasks", indices = [Index(value = ["date"])])
  data class Task(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      val date: String,             // yyyy-MM-dd 포맷
      val content: String,          // 할 일 내용
      val isCompleted: Boolean = false,
      val isPriority: Boolean = false,  // 중요 여부 (별표)
      val isMigrated: Boolean = false,  // 이월 처리된 원본인지 여부 (중복 이월 방지용)
      val orderIndex: Int = 0,          // 수동 순서 조정용 인덱스 (아직 UI 미구현, 5절 "과제 1" 참고)
      val createdAt: Long = System.currentTimeMillis()
  )
  ```
* **현재 DB 버전**: `version = 4`
  * v1: 기본 Task 엔티티 (인덱스 없음)
  * v2: `date` 컬럼 인덱싱 추가 (`@Index(["date"])`)
  * v3: `isPriority`, `orderIndex` 필드 추가
  * v4: `isMigrated` 컬럼 추가 — 어제 태스크를 오늘로 이월할 때 원본에 `isMigrated = true`를 표시해 같은 태스크가 중복으로 여러 번 이월되는 것을 막습니다.
  * `Migration(3, 4)`가 `AppDatabase.kt`에 정식으로 작성되어 있으며(`ALTER TABLE tasks ADD COLUMN isMigrated ...`), `addMigrations(MIGRATION_3_4)`로 등록되어 있습니다.
  * 다만 `fallbackToDestructiveMigration(dropAllTables = true)`가 함께 켜져 있어, 등록되지 않은 버전 점프(예: v2 → v4 등 미래에 마이그레이션이 누락된 경우)에서는 여전히 **전체 테이블 삭제**로 처리됩니다. 신규 스키마 변경 시 반드시 `Migration(N, N+1)`을 추가하고, 배포 전 실제 업그레이드 경로(구버전 설치 → 업데이트)를 테스트하는 것을 권장합니다.

---

## 3. 홈 화면 위젯(Glance) 연동 주의사항

* 위젯은 Compose UI와 문법이 유사하나, **Jetpack Glance 전용 컴포넌트**(`androidx.glance.*`)만 사용해야 합니다.
* 앱 내부에서 태스크 변경 시 ViewModel에서 `updateWidget()`을 통해 모든 활성 위젯을 자동 갱신합니다.
* 위젯에서 할 일 체크 또는 이월 클릭 시에는 `ActionCallback`(`ToggleTaskAction`, `MigrateTasksAction`)에서 DB를 직접 조작한 뒤 `BulletJournalWidget().update(context, glanceId)`를 호출합니다.
* 위젯 색상은 `ui/theme/Color.kt`의 라이트/다크 팔레트를 `ColorProvider(day = ..., night = ...)`로 그대로 재사용해 앱 본체와 톤을 맞춥니다.
* `BulletJournalWidget.provideGlance`, `ToggleTaskAction`, `MigrateTasksAction` 세 곳 모두 `TaskRepositoryImpl(AppDatabase.getInstance(context).taskDao())`을 직접 생성해서 사용합니다(Hilt 미사용). Repository 생성자 시그니처가 바뀌면 이 세 곳을 모두 함께 고쳐야 합니다.

---

## 4. 단위 테스트 작성 및 유지보수 규칙

* 단위 테스트는 `app/src/test/java/com/simple/bulletjournal/`에 위치합니다.
* 테스트 시 Room DB 대신 **`FakeTaskRepository`**를 사용합니다.
* **주의**: `TaskDao`의 정렬 쿼리(`isPriority DESC, orderIndex ASC, createdAt ASC`)가 변경되면, `FakeTaskRepository`의 `sortedWith` 로직도 반드시 동일하게 맞춰주어야 테스트 일관성이 유지됩니다.
* 테스트 실행 명령어:
  ```bash
  ./gradlew testDebugUnitTest
  ```

---

## 5. v1.1+ 권장 개발 과제 (Next Roadmap)

다음 세션 개발자가 우선적으로 구현하기 좋은 추천 백로그입니다.

> ⚠️ **우선순위 조정 (2026-09-20)**: 스토어 출시(8절 참고)가 목표로 확정되면서, 아래 4개 과제 중 **과제 2(테마 수동 선택)**는 8절 Phase 0에서 신설하는 설정 화면과 함께 구현하도록 우선순위를 올렸습니다. 과제 1/3/4는 스토어 1차 출시와 직접 관련이 없으므로 **출시 이후로 순연**을 권장합니다.

### 과제 1: 할 일 수동 순서 변경 (Manual Reordering)
* **현황**: `Task` 엔티티에 이미 `orderIndex: Int` 필드와 쿼리(`orderIndex ASC`)가 준비되어 있습니다.
* **구현 방향**:
  * **방안 A (추천)**: `TaskOnLine` 우측 옵션 메뉴 또는 위/아래 이동 버튼(▲/▼)을 통해 인접 항목과 `orderIndex`를 스왑(`swapOrder(task1, task2)`).
  * **방안 B**: `MainScreen`의 `Column + verticalScroll`을 `LazyColumn` + `reorderable` 제스처 라이브러리로 마이그레이션하여 길게 눌러 드래그 앤 드롭 구현.

### 과제 2: 앱 내 테마 수동 선택 옵션
* **현황**: 현재는 `isSystemInDarkTheme()`을 기본값으로 하여 OS 시스템 설정만을 따릅니다.
* **구현 방향**:
  * `DataStore Preferences`를 추가하여 [시스템 기본 / 라이트 고정 / 다크 고정] 모드를 저장.
  * 상단 헤더 영역에 테마 토글 버튼 또는 설정 다이얼로그 추가.

### 과제 3: 정통 불렛 저널 기호 체계 확장
* **현황**: 현재는 완료 여부(`isCompleted`)와 중요도(`isPriority`)만 지원.
* **구현 방향**:
  * 태스크 타입을 구분하는 `TaskType` enum 도입:
    * `TASK` (`•`) : 할 일
    * `EVENT` (`○`) : 약속/이벤트
    * `NOTE` (`-`) : 단순 메모/기록
  * 불렛 아이콘 클릭 시 타입을 순환 변경하거나 선택할 수 있는 UX 제공.

### 과제 4: 데이터 백업 및 복원
* **구현 방향**:
  * 사용자가 작성한 전체 저널 데이터를 JSON 또는 CSV 파일로 로컬 저장소/드라이브에 내보내기 및 복원(Import/Export) 기능.

### 과제 5: 라이트 테마 상태바 아이콘 대비 개선 (버그 수정, 2026-09-20 접수) — 우선순위 높음
* **현황**: 6절 "이슈 6" 참고. OS 다크모드 + 앱 내 테마를 "라이트"로 선택한 조합에서 상태바 아이콘이 흰색에 가까워 식별이 거의 불가능한 사용성 버그.
* **구현 방향**: `BulletJournalRoot`에서 `darkTheme` 상태 변화에 반응해 `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme`를 호출하도록 수정. 스토어 심사와는 무관하지만 실사용 품질에 직접 영향을 주므로, 다른 로드맵 과제보다 먼저 처리 권장.

---

## 6. 확인된 이슈 (Known Issues)

소스 코드를 문서와 대조하는 과정에서 발견한, 다음 세션에서 우선 검토가 필요한 항목입니다.

### 이슈 1: `MainScreen`이 Hilt가 아닌 기본 `viewModel()`로 `TaskViewModel`을 생성함 — ✅ 2026-09-20 해결
* **해결 내용**: `androidx.hilt:hilt-navigation-compose` 의존성을 추가하고, [`MainScreen.kt`](app/src/main/java/com/simple/bulletjournal/ui/MainScreen.kt)의 `fun MainScreen(viewModel: TaskViewModel = viewModel())`를 `hiltViewModel()`로 교체했습니다. `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` 모두 정상 컴파일/실행 확인.
* **실기기 검증**: ✅ 완료(2026-09-20). Samsung SM-S711N 실기기(release 서명 빌드)에서 정상 렌더링 확인. (검증 과정에서 이슈 5의 완전히 별개인 렌더링 버그를 발견 — 아래 참고.)
* (과거 서술 보존) `TaskViewModel`은 `@Inject constructor(application: Application, private val repository: TaskRepository)` 형태라 `repository`를 해석하려면 Hilt 팩토리가 필요했고, 기본 Compose `viewModel()`이 쓰는 `SavedStateViewModelFactory`는 이를 몰라 런타임 인스턴스화가 실패할 수 있는 구조였습니다. 단위 테스트는 생성자를 직접 호출해 이 경로를 타지 않아 지금까지 드러나지 않았습니다.

### 이슈 2: `orderIndex`는 저장만 되고 갱신 로직이 없음
* **현황**: `Task.orderIndex`는 항상 기본값 `0`으로 생성되며, 이를 변경하는 코드가 `TaskViewModel`, `MainScreen` 어디에도 없습니다.
* **영향**: 정렬 쿼리의 2순위 기준(`orderIndex ASC`)이 사실상 항상 동률이라 3순위 기준(`createdAt ASC`)으로 정렬되는 것과 동일합니다. 5절 "과제 1: 할 일 수동 순서 변경"이 구현되기 전까지는 의도된 동작입니다.

### 이슈 3: Glance 위젯은 Hilt 그래프를 사용하지 않음
* **현황**: 3절에서 설명한 대로 위젯 관련 3개 지점 모두 `TaskRepositoryImpl`을 직접 생성합니다. 기능상 문제는 없지만, 향후 Repository에 Hilt로만 주입 가능한 의존성(예: DataStore, 원격 API)이 추가되면 위젯 쪽 코드도 반드시 함께 손봐야 합니다.

### 이슈 4: `migrateYesterdayTasks` 단위 테스트가 간헐적으로 타임아웃 실패함 (2026-09-20 발견)
* **현황**: `TaskViewModelTest.migrateYesterdayTasks_copiesOnlyUncompletedTasksToCurrentDate` 테스트가 `./gradlew testDebugUnitTest` 실행 시 `app.cash.turbine.TurbineAssertionError: No value produced in 3s`로 실패하는 것을 확인했습니다(19개 중 1개 실패).
* **비고**: 이번 세션에서 만진 `MainScreen`/Hilt 관련 변경과는 무관한 기존 결함으로 보입니다. `migrateYesterdayTasks()`가 `insertTask`/`updateTask`를 태스크 수만큼 순차 `launch` 내에서 여러 번 호출하는데, `FakeTaskRepository`의 Flow 방출 타이밍과 Turbine의 `awaitItem()` 기대 횟수가 어긋나 있을 가능성이 있습니다.
* **권장 조치**: 다음 소스 업데이트 시 `FakeTaskRepository.kt`와 해당 테스트의 `awaitItem()` 호출 횟수를 대조해 원인을 확인하고 수정할 것.

### 이슈 5: `BulletJournalApp`이라는 이름의 최상위 컴포저블이 `Application` 클래스와 충돌해 화면이 완전히 빈 채로 렌더링됨 — ✅ 2026-09-20 해결
* **증상**: 앱을 실기기에 설치해서 실행하면 크래시 없이 정상적으로 "Displayed"까지 되지만, 화면에는 배경색만 채워지고 어떤 UI도 그려지지 않았습니다(설정 화면 진입용 톱니바퀴 아이콘도 당연히 안 보였습니다). `uiautomator dump`로 확인해도 `android:id/content` 아래에 자식 뷰가 단 하나도 없었고, `logcat`(main/system/crash 버퍼 전부) 어디에도 예외나 크래시 로그가 없었습니다.
* **근본 원인**: [`MainActivity.kt`](app/src/main/java/com/simple/bulletjournal/MainActivity.kt)에서 `setContent { BulletJournalApp() }`로 최상위 컴포저블을 호출하고 있었는데, 이 컴포저블 함수의 이름이 **같은 패키지의 `BulletJournalApp.kt`에 있는 `class BulletJournalApp : Application()`과 완전히 동일**했습니다. `Application` 클래스는 암묵적으로 인자 없는 public 생성자를 가지므로, `BulletJournalApp()`이라는 호출식이 우리가 만든 컴포저블 함수(파라미터 `settingsViewModel: SettingsViewModel = hiltViewModel()`에 기본값이 있어 인자 없이도 호출 가능)와 `Application`의 생성자 호출 사이에서 **오버로드 해석 시 컴파일 에러나 경고 없이 조용히 생성자 쪽으로 resolve**되었습니다. 즉 매번 새 `BulletJournalApp` 인스턴스를 만들어서 버리기만 했을 뿐, 실제 UI 컴포저블은 단 한 번도 실행되지 않았습니다.
* **디버깅 방법**: `Log.d`를 `onCreate()`와 컴포저블 최상단에 순서대로 심어서 어디까지 로그가 찍히는지 이분탐색했습니다. `setContent`의 람다 진입 로그는 찍혔지만, 컴포저블 함수 본문의 첫 줄 로그는 전혀 찍히지 않는 것으로 좁혀졌고, 이는 "함수가 아예 호출되지 않았다"는 뜻이었습니다.
* **해결**: 컴포저블 함수 이름을 `BulletJournalApp` → `BulletJournalRoot`로 변경. 실기기(Samsung SM-S711N, release 서명 빌드)에서 메인 화면·설정 화면·뒤로가기까지 전부 정상 동작 확인했습니다.
* **교훈**: 같은 패키지 안에서 클래스명과 최상위 함수명이 겹치면(특히 그 클래스가 인자 없는 생성자를 가진 경우) Kotlin이 경고 없이 엉뚱한 쪽을 호출할 수 있습니다. 향후 최상위 컴포저블 함수는 `XxxApp`처럼 `Application` 서브클래스와 헷갈릴 수 있는 이름을 피하고, 이번처럼 `XxxRoot` 또는 `XxxScreen` 계열로 명명할 것.

### 이슈 6: 라이트 테마에서 상태바 아이콘이 흰색에 가까워 식별 불가능함 (2026-09-20 발견, 미해결 — 백로그)
* **증상**: 설정에서 테마를 "라이트"로 선택하면 앱 배경은 밝은 줄공책 색(`LightPaper`)으로 바뀌지만, 상단 상태바(시계/배터리/네트워크 아이콘)는 여전히 흰색에 가까운 밝은 색으로 남아 있어 사람 눈으로 거의 식별이 안 됩니다.
* **근본 원인(추정)**: [`MainActivity.kt`](app/src/main/java/com/simple/bulletjournal/MainActivity.kt)의 `onCreate()`에서 `enableEdgeToEdge()`를 인자 없이 한 번만 호출합니다. 이 함수는 호출 시점의 **시스템(OS) 다크모드 여부**를 기준으로 상태바 아이콘 밝기(라이트/다크 아이콘)를 자동 결정하는데, 이는 우리 앱이 `SettingsViewModel`을 통해 자체적으로 계산하는 `darkTheme`(사용자가 고른 시스템/라이트/다크 설정)과 **서로 다른 값**입니다. 예를 들어 폰의 OS 자체는 다크모드인데 앱 내에서 "라이트"를 선택한 경우, `enableEdgeToEdge()`는 OS 기준으로 "어두운 배경이니 밝은 아이콘"으로 한 번 결정하고 끝나버리고, 이후 `BulletJournalTheme`이 실제로는 밝은 배경을 그려도 상태바 아이콘 색은 갱신되지 않습니다.
* **영향 범위**: 폰 OS가 다크모드 + 앱 내 테마를 "라이트"로 선택한 조합에서만 발생합니다. "시스템 기본"을 쓰거나 OS와 앱 테마가 같은 방향이면 우연히 맞아떨어져 문제가 드러나지 않습니다.
* **권장 조치**: `BulletJournalRoot`에서 `darkTheme` 값이 바뀔 때마다 `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme`를 호출하도록 `SideEffect`/`LaunchedEffect(darkTheme)`를 추가해, 상태바 아이콘 밝기를 앱의 실제 테마 상태에 반응형으로 맞출 것. `enableEdgeToEdge()`의 최초 1회 자동 감지에만 의존하지 않도록 수정.

---

## 7. 빌드 & 배포 주의사항

1. **JDK 환경**:
   * macOS 기준 Android Studio 내장 JBR 경로: `/Applications/Android Studio.app/Contents/jbr/Contents/Home`
   * 터미널 실행 시:
     ```bash
     JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew <tasks>
     ```
   * **참고**: 새로 클론한 환경에는 `local.properties`(`sdk.dir=...`)가 없어 Gradle이 Android SDK 위치를 찾지 못하고 실패할 수 있습니다. Android Studio로 열면 자동 생성되며, CLI 전용 환경이라면 직접 `local.properties`에 `sdk.dir` 경로를 추가해야 합니다.
2. **릴리즈 서명 키 관리**:
   * 서명 키 파일: `keystore/release.jks` (alias: `bulletjournal`)
   * 설정 파일: `keystore.properties` (템플릿: `keystore.properties.example`)
   * **경고**: 이 키스토어 파일이 유실되면 기존 사용자가 앱 데이터를 유지한 채 업데이트할 수 없습니다. 안전한 곳에 백업을 유지하세요.
3. **버전 번호 관리**:
   * 새 버전 릴리즈 시 `app/build.gradle.kts`의 `versionCode`를 1씩 증가시키고 `versionName`을 갱신합니다. (현재 `versionCode = 2`, `versionName = "1.1.0-dev"`)

---

## 8. 수익화 및 스토어 출시 로드맵 (2026-09-20 수립)

**목표**: 앱을 Play 스토어에 정식 등록할 수 있는 품질로 끌어올리고, 다음 수익 모델을 도입합니다.
1. 앱 화면 하단 배너 광고
2. 인앱결제로 광고 제거

아래 순서(Phase 0 → 3)대로 진행하는 것을 권장하며, Phase 4는 기존 5절 로드맵과의 우선순위 조정입니다.

### Phase 0 — 선행 필수 수정 (다른 작업보다 먼저)

| 항목 | 내용 |
|---|---|
| **6절 이슈 1 해결** | ✅ 완료(2026-09-20): `MainScreen`이 `hiltViewModel()`을 쓰도록 수정, 빌드/단위테스트/실기기 화면 검증 모두 통과. |
| **설정 화면(SettingsScreen) 신설** | ✅ 완료(2026-09-20): `SettingsScreen` + `SettingsViewModel` 추가, `MainScreen` 상단에 톱니바퀴 아이콘으로 진입. 화면 전환은 `NavHost` 없이 `MainActivity`의 로컬 상태(`mutableStateOf<Boolean>`)로 `MainScreen` ↔ `SettingsScreen` 토글(화면이 2개뿐이라 `NavHost`는 과함). 테마 라디오 3개(시스템/라이트/다크)는 `MainActivity`가 `SettingsViewModel.userPreferences`를 구독해 `BulletJournalTheme(darkTheme=...)`에 실시간 반영. 광고 제거 버튼은 UI/상태 배선만 완료 — 클릭 시 `setAdRemoved(true)`를 **직접** 호출하는 임시 구현이며, Phase 2에서 Play Billing 구매 콜백으로 교체 예정(코드에 `TODO(Phase 2)` 표시). 빌드/단위테스트/실기기 화면 검증(테마 전환, 설정 진입·뒤로가기) 모두 통과. 실기기 검증 과정에서 발견된 별개의 렌더링 버그는 6절 이슈 5 참고. |
| **DataStore Preferences 도입** | ✅ 완료(2026-09-20): `UserPreferencesRepository`/`DataStoreModule` 추가, 단위 테스트 3개 통과. `isAdRemoved`/`themeMode` 저장 가능. **아직 UI/ViewModel에서 실제로 쓰이진 않음** — 설정 화면에서 주입해 소비하는 게 다음 작업. |

### Phase 1 — 광고 (AdMob 배너) — ✅ 완료(2026-09-20, 테스트 광고 ID 기준)

1. ✅ `com.google.android.gms:play-services-ads`(23.6.0) + `com.google.android.ump:user-messaging-platform`(3.1.0) 의존성 추가. `AndroidManifest.xml`에 AdMob `APPLICATION_ID` meta-data 등록(현재 Google 공식 테스트 App ID `ca-app-pub-3940256099942544~3347511713` — `TODO(Phase 1 출시 전)` 주석으로 실제 ID 교체 지점 표시).
2. ✅ `INTERNET`, `ACCESS_NETWORK_STATE` 권한 추가 — **이 앱 최초의 네트워크 권한**이며, Data Safety 신고 대상입니다.
3. ✅ [`ui/ads/BannerAd.kt`](app/src/main/java/com/simple/bulletjournal/ui/ads/BannerAd.kt): `AndroidView`로 `AdView`를 래핑한 컴포저블. `remember`로 `AdView` 인스턴스를 보관하고 `DisposableEffect`로 `loadAd`/`destroy` 생명주기를 관리합니다. 테스트 배너 광고 단위 ID(`ca-app-pub-3940256099942544/6300978111`) 사용 중 — 실제 ID로 교체할 지점에 `TODO(Phase 1 출시 전)` 주석 표시.
4. ✅ **UMP(User Messaging Platform) 동의 플로우**: [`BulletJournalApp.kt`](app/src/main/java/com/simple/bulletjournal/BulletJournalApp.kt)의 `requestConsentAndInitializeAds(activity, onReady)`가 `ConsentInformation.requestConsentInfoUpdate` → 필요 시 `loadAndShowConsentFormIfRequired` → 동의 완료(`canRequestAds() == true`) 후에만 `MobileAds.initialize()`를 호출합니다. `MainActivity.onCreate()`에서 이 함수를 호출하고, 완료 콜백에서 `adsReady = true`(Compose `mutableStateOf`)로 배너 노출을 트리거합니다.
5. ✅ `MainScreen(showAds: Boolean)` 파라미터로 조건부 렌더링: `BulletJournalRoot`(`MainActivity.kt`)에서 `adsReady && !preferences.isAdRemoved`를 계산해 내려줍니다. 설정 화면에서 "광고 제거"를 누르면 `UserPreferencesRepository`의 `isAdRemoved`가 `true`가 되고, 배너가 즉시 사라지며 앱을 완전히 재시작해도 유지됩니다(DataStore 영속성). 실기기(Samsung SM-S711N)에서 테스트 배너 노출 → 광고 제거 클릭 → 배너 즉시 소멸 → 재실행 후에도 유지 전부 확인.
6. ✅ 개발 중에는 테스트 광고 단위 ID 사용 중 — 실제 광고 단위 ID는 출시 직전에만 교체 예정(본인이 자기 광고를 클릭하면 계정 정지 위험이므로 **절대 미리 교체하지 말 것**).
7. ✅ Glance 위젯에는 광고를 넣지 않았습니다 (Glance는 배너 SDK 렌더링 불가 — 앱 화면에만 적용).

> **다음 세션 확인 사항**: 실제 AdMob App ID/배너 광고 단위 ID를 받으면 `AndroidManifest.xml`의 `APPLICATION_ID` meta-data와 `BannerAd.kt`의 `TEST_BANNER_AD_UNIT_ID`를 교체할 것(두 곳 모두 `TODO(Phase 1 출시 전)` 주석으로 표시해 둠).
>
> **실제 AdMob ID 확보됨 (2026-09-20, 아직 미적용 — 스토어 출시 직전에만 교체할 것)**:
> - App ID: `ca-app-pub-6630095840984426~4111120412`
> - 배너 광고 단위 ID: `ca-app-pub-6630095840984426/8566115600`
>
> 개발 중에 미리 적용하면 본인이 실수로 자기 광고를 클릭해 AdMob 계정이 정지될 위험이 있으므로, 지금은 코드에 반영하지 않고 이 문서에만 기록해 둡니다. 출시 직전 체크리스트(8절 Phase 3)에서 위 두 값으로 교체할 것.

### Phase 2 — 인앱결제 (광고 제거) — ✅ 코드 구현 완료(2026-09-20), 실 구매 플로우 검증은 Play Console 상품 등록 후 가능

1. ✅ `com.android.billingclient:billing-ktx`(7.1.1) 추가.
2. ✅ **인앱상품 ID 확정**: `remove_ads_sbj` (`data/BillingRepositoryImpl.kt`의 `REMOVE_ADS_PRODUCT_ID` 상수). **Play Console에 이 문자열과 정확히 일치하는 비소모성(non-consumable) 상품을 등록해야 동작합니다.** 아직 등록 전.
3. ✅ [`data/BillingRepository.kt`](app/src/main/java/com/simple/bulletjournal/data/BillingRepository.kt) / [`BillingRepositoryImpl.kt`](app/src/main/java/com/simple/bulletjournal/data/BillingRepositoryImpl.kt): `BillingClient` 연결 → `queryProductDetailsAsync`로 상품 정보 캐싱 → 연결 성공 시 자동으로 `restorePurchases()`(=`queryPurchasesAsync`) 호출해 기존 구매 이력을 동기화합니다. 재설치·기기 변경 시에도 상태 복원됨.
4. ✅ 구매 완료(`onPurchasesUpdated`) 시 `isAdRemoved = true`로 `UserPreferencesRepository`에 반영 + 미승인 구매는 즉시 `acknowledgePurchase()` 호출(3일 내 승인 안 하면 Play가 자동 환불).
5. ✅ [`SettingsScreen.kt`](app/src/main/java/com/simple/bulletjournal/ui/SettingsScreen.kt): "광고 제거" 버튼(미구매 시에만 노출) + "구매 복원" 버튼(미구매 상태일 때만 노출, 다른 기기 구매 이력 확인용) 배치. `LocalContext.current as? Activity`로 결제 플로우에 필요한 Activity 확보.
6. ✅ `SettingsViewModel`에 `purchaseAdRemoval(activity)` / `restorePurchases()` 위임 메서드 추가, 기존 `setAdRemoved` 직접 호출(Phase 0의 임시 구현)은 제거. `di/BillingModule.kt`로 Hilt 싱글톤 제공(`DatabaseModule`/`DataStoreModule`과 동일 패턴).
7. ✅ 단위테스트: `FakeBillingRepository` 추가, `SettingsViewModelTest`에 위임 호출 검증 2건 추가(총 20개 중 기존 flaky 1개 제외 전부 통과).
8. ⚠️ **실기기 검증 범위**: 크래시 없이 빌드/설치/실행되는 것, 기존 `isAdRemoved=true` 상태에서 UI 분기가 올바른 것까지는 확인했습니다. **"광고 제거" 버튼을 눌러 실제 Play 결제 다이얼로그가 뜨는지는 Play Console에 `remove_ads_sbj` 상품이 등록되기 전까지 검증 불가**(상품이 없으면 `queryProductDetailsAsync` 결과가 비어 있어 버튼을 눌러도 아무 일도 일어나지 않고 재조회만 시도함 — 크래시는 안 나지만 결제창도 안 뜸). Play Console에 상품 등록 후 다음 세션에서 최우선으로 재검증할 것.

> **다음 세션 확인 사항 (Play Console 작업, 사용자 액션 필요)**:
> 1. Play Console에 앱 등록 (최소 Internal Testing 트랙 업로드)
> 2. 비소모성 인앱상품 생성, 상품 ID를 정확히 `remove_ads_sbj`로 설정
> 3. 개발자 본인 Google 계정을 License Tester로 등록 (실비용 없는 테스트 결제, 아래 참고)

> **개발자 본인 사용 관련 결정 (2026-09-20)**: 개발자 본인은 광고를 보지 않고 쓰고 싶다는 요구가 있었으나, 별도 build flavor(예: `applicationIdSuffix`로 개인용 패키지 분리)나 코드 분기는 **채택하지 않기로 결정**했습니다. 이유: 코드 분기가 늘어날수록 유지보수 비용이 커지고(버그 수정을 두 곳에 반영해야 함), 정작 실제 결제 플로우를 검증할 방법이 따로 필요해집니다. 대신 **Google Play Console의 License Testing**을 사용합니다:
> 1. 앱을 Play Console에 등록하고 "광고 제거" IAP 상품을 만든 뒤, Internal Testing 트랙에 한 번 업로드합니다(공개 배포 아님, Play Console에 앱/상품을 인식시키기 위한 최소 조건).
> 2. 개발자 본인의 Google 계정을 License Tester로 등록합니다.
> 3. 이후로는 Android Studio에서 평소처럼 빌드 → 기기에 직접 실행(USB/무선 디버깅)하면 됩니다. License Testing은 Play 스토어에서 설치한 빌드나 정식 서명 키로 서명된 빌드를 요구하지 않으므로, `applicationId`만 Play Console에 등록한 것과 동일하면 디버그 서명 빌드에서도 "광고 제거" 버튼이 **실제 과금 없이 테스트 결제**로 동작합니다.
> 4. 따라서 앱은 **단일 코드베이스, 단일 `applicationId`**로 유지합니다. Play Console 등록/License Tester 등록은 반복 작업이 아니라 최초 1회 설정입니다.

### Phase 3 — 스토어 심사 통과를 위한 필수 준비물

| 항목 | 내용 |
|---|---|
| **개인정보처리방침 URL** | ✅ 완료(2026-09-20): GitHub Pages로 게시됨 — https://zeonerd.github.io/simple-bullet-journal/privacy-policy.html (소스: [`docs/privacy-policy.html`](docs/privacy-policy.html), 원본 마크다운: [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md)). Play Console 등록 시 이 URL을 그대로 입력하면 됩니다. |
| **Data Safety 설문** | ✅ 답변 초안 작성 완료 — [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md)의 "Data Safety 설문 답변 초안" 섹션 참고. Play Console 계정 생성 후 실제 폼에 옮겨 적을 것. |
| **콘텐츠 등급 설문** | Play Console 계정 필요 — 사용자 액션 대기. 참고용 예상 답변: 폭력/선정성/도박 콘텐츠 없음, 사용자 생성 콘텐츠 없음(개인 할 일 목록은 기기 로컬 저장), 광고 있음 → 전체 이용가(PEGI 3 / Everyone) 등급 예상. |
| **타겟 API 레벨 재확인** | ✅ 완료(2026-09-20): `targetSdk 36`(Android 16)으로 상향. Play 정책상 2026-08-31부로 신규 앱은 API 36 이상 필수로 확인됨. 실기기(Android 16) 검증 완료 — 6절 참고. |
| **릴리즈 빌드 난독화** | ✅ 완료(2026-09-20): `isMinifyEnabled = true`, `isShrinkResources = true` 적용. Glance 위젯 ActionCallback(리플렉션 위험)과 Room Entity에 keep 규칙 추가, 실기기에서 위젯 토글까지 전체 플로우 재검증 완료. |
| **크래시 리포팅** | 미착수. Firebase Crashlytics 도입 검토 — Firebase 프로젝트 생성이 필요합니다(사용자 액션, Play Console과는 별개로 무료 생성 가능). |
| **스토어 등록 에셋** | ✅ 완료(2026-09-20): 짧은/긴 설명 초안([`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) 하단), 512x512 Hi-res 아이콘([`store_assets/hi_res_icon_512.png`](store_assets/hi_res_icon_512.png)), 실기기 스크린샷 5장([`store_assets/screenshots/`](store_assets/screenshots)) 모두 준비됨. ⚠️ 스크린샷 원본 비율(1080:2340)이 Play 권장 최대 비율(2:1)을 살짝 초과 — 업로드 시 거부되면 크롭 필요(`store_assets/README.md` 참고). 그래픽 배너 이미지(1024x500)는 아직 미준비. |
| **Play App Signing** | Play Console 앱 등록 시 함께 설정 — 사용자 액션 대기. |

### Phase 4 — 기존 5절 로드맵과의 우선순위 조정

* **과제 2(테마 수동 선택)**: Phase 0에서 만들 설정 화면과 자연히 묶여서 함께 구현 → 우선순위 상향
* **과제 1(수동 순서 변경) / 3(TaskType 확장) / 4(백업·복원)**: 스토어 1차 출시와 직접적인 관련이 없으므로 출시 이후로 순연 권장
