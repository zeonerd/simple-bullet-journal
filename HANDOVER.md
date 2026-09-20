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

---

## 6. 확인된 이슈 (Known Issues)

소스 코드를 문서와 대조하는 과정에서 발견한, 다음 세션에서 우선 검토가 필요한 항목입니다.

### 이슈 1: `MainScreen`이 Hilt가 아닌 기본 `viewModel()`로 `TaskViewModel`을 생성함 — ✅ 2026-09-20 해결
* **해결 내용**: `androidx.hilt:hilt-navigation-compose` 의존성을 추가하고, [`MainScreen.kt`](app/src/main/java/com/simple/bulletjournal/ui/MainScreen.kt)의 `fun MainScreen(viewModel: TaskViewModel = viewModel())`를 `hiltViewModel()`로 교체했습니다. `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` 모두 정상 컴파일/실행 확인.
* **미검증 항목**: 이 머신에는 AVD(에뮬레이터)가 하나도 설치되어 있지 않아, 실제 기기/에뮬레이터에서 화면이 뜨는지(런타임 크래시 여부)는 아직 눈으로 확인하지 못했습니다. 다음 세션에서 Android Studio로 실행해 최종 확인할 것.
* (과거 서술 보존) `TaskViewModel`은 `@Inject constructor(application: Application, private val repository: TaskRepository)` 형태라 `repository`를 해석하려면 Hilt 팩토리가 필요했고, 기본 Compose `viewModel()`이 쓰는 `SavedStateViewModelFactory`는 이를 몰라 런타임 인스턴스화가 실패할 수 있는 구조였습니다. 단위 테스트는 생성자를 직접 호출해 이 경로를 타지 않아 지금까지 드러나지 않았습니다.

### 이슈 2: `orderIndex`는 저장만 되고 갱신 로직이 없음
* **현황**: `Task.orderIndex`는 항상 기본값 `0`으로 생성되며, 이를 변경하는 코드가 `TaskViewModel`, `MainScreen` 어디에도 없습니다.
* **영향**: 정렬 쿼리의 2순위 기준(`orderIndex ASC`)이 사실상 항상 동률이라 3순위 기준(`createdAt ASC`)으로 정렬되는 것과 동일합니다. 5절 "과제 1: 할 일 수동 순서 변경"이 구현되기 전까지는 의도된 동작입니다.

### 이슈 3: Glance 위젯은 Hilt 그래프를 사용하지 않음
* **현황**: 3절에서 설명한 대로 위젯 관련 3개 지점 모두 `TaskRepositoryImpl`을 직접 생성합니다. 기능상 문제는 없지만, 향후 Repository에 Hilt로만 주입 가능한 의존성(예: DataStore, 원격 API)이 추가되면 위젯 쪽 코드도 반드시 함께 손봐야 합니다.

### 이슈 4: `migrateYesterdayTasks` 단위 테스트가 간헐적으로 타임아웃 실패함 (2026-09-20 발견)
* **현황**: `TaskViewModelTest.migrateYesterdayTasks_copiesOnlyUncompletedTasksToCurrentDate` 테스트가 `./gradlew testDebugUnitTest` 실행 시 `app.cash.turbine.TurbineAssertionError: No value produced in 3s`로 실패하는 것을 확인했습니다(13개 중 1개 실패).
* **비고**: 이번 세션에서 만진 `MainScreen`/Hilt 관련 변경과는 무관한 기존 결함으로 보입니다. `migrateYesterdayTasks()`가 `insertTask`/`updateTask`를 태스크 수만큼 순차 `launch` 내에서 여러 번 호출하는데, `FakeTaskRepository`의 Flow 방출 타이밍과 Turbine의 `awaitItem()` 기대 횟수가 어긋나 있을 가능성이 있습니다.
* **권장 조치**: 다음 소스 업데이트 시 `FakeTaskRepository.kt`와 해당 테스트의 `awaitItem()` 호출 횟수를 대조해 원인을 확인하고 수정할 것.

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
| **6절 이슈 1 해결** | ✅ 완료(2026-09-20): `MainScreen`이 `hiltViewModel()`을 쓰도록 수정, 빌드/단위테스트 통과 확인. **다만 실기기/에뮬레이터 화면 검증은 아직 남아 있음** — 다음 세션에서 최우선 확인. |
| **설정 화면(SettingsScreen) 신설** | ✅ 완료(2026-09-20): `SettingsScreen` + `SettingsViewModel` 추가, `MainScreen` 상단에 톱니바퀴 아이콘으로 진입. 화면 전환은 `NavHost` 없이 `MainActivity`의 로컬 상태(`mutableStateOf<Boolean>`)로 `MainScreen` ↔ `SettingsScreen` 토글(화면이 2개뿐이라 `NavHost`는 과함). 테마 라디오 3개(시스템/라이트/다크)는 `MainActivity`가 `SettingsViewModel.userPreferences`를 구독해 `BulletJournalTheme(darkTheme=...)`에 실시간 반영. 광고 제거 버튼은 UI/상태 배선만 완료 — 클릭 시 `setAdRemoved(true)`를 **직접** 호출하는 임시 구현이며, Phase 2에서 Play Billing 구매 콜백으로 교체 예정(코드에 `TODO(Phase 2)` 표시). 빌드/단위테스트(`SettingsViewModelTest` 3건) 통과 확인, 실기기 화면 검증은 미실시. |
| **DataStore Preferences 도입** | ✅ 완료(2026-09-20): `UserPreferencesRepository`/`DataStoreModule` 추가, 단위 테스트 3개 통과. `isAdRemoved`/`themeMode` 저장 가능. **아직 UI/ViewModel에서 실제로 쓰이진 않음** — 설정 화면에서 주입해 소비하는 게 다음 작업. |

### Phase 1 — 광고 (AdMob 배너)

1. `com.google.android.gms:play-services-ads` 의존성 추가, `AndroidManifest`에 AdMob `APPLICATION_ID` meta-data 등록
2. `INTERNET`, `ACCESS_NETWORK_STATE` 권한 추가 — **이 앱 최초의 네트워크 권한**이며, Data Safety 신고 대상입니다. (현재 `AndroidManifest.xml`에는 아무 권한도 선언되어 있지 않은 완전 오프라인 구조)
3. Compose 화면 하단에 `AndroidView`로 `AdView`를 래핑해서 배치 (Compose에는 네이티브 배너 컴포넌트가 없음)
4. **UMP(User Messaging Platform) SDK로 GDPR/EEA 동의 배너** 연동 — AdMob 심사 필수 요건이며, 누락 시 계정 정지 사유가 될 수 있습니다.
5. `isAdRemoved == true`일 때 배너를 렌더링하지 않도록 조건부 처리 (ViewModel/DataStore 상태 구독)
6. 개발 중에는 반드시 **테스트 광고 단위 ID**를 사용하고, 실제 광고 단위 ID는 출시 직전에만 교체합니다 (본인이 자기 광고를 클릭하면 계정 정지 위험).
7. Glance 위젯에는 광고를 넣지 않습니다 (Glance는 배너 SDK 렌더링 불가 — 앱 화면에만 적용).

### Phase 2 — 인앱결제 (광고 제거)

1. `com.android.billingclient:billing-ktx` 추가, Play Console에 **비소모성(non-consumable) 상품** "광고 제거"를 등록합니다.
2. `BillingClient.queryPurchasesAsync()`로 앱 시작 시 구매 이력을 재조회합니다 → 재설치·기기 변경 시에도 상태가 복원됩니다 (DataStore 로컬 캐시만으로는 유실됨).
3. 구매 완료 시 `acknowledgePurchase()`를 반드시 호출합니다 (호출하지 않으면 Play 정책상 3일 뒤 자동 환불됩니다).
4. 설정 화면에 "광고 제거" 구매 버튼과 "구매 복원" 버튼을 배치합니다.
5. 결제 성공 → `isAdRemoved = true` → 광고 즉시 숨김 → 앱 재실행 후에도 유지되는지 QA로 확인합니다.

> **개발자 본인 사용 관련 결정 (2026-09-20)**: 개발자 본인은 광고를 보지 않고 쓰고 싶다는 요구가 있었으나, 별도 build flavor(예: `applicationIdSuffix`로 개인용 패키지 분리)나 코드 분기는 **채택하지 않기로 결정**했습니다. 이유: 코드 분기가 늘어날수록 유지보수 비용이 커지고(버그 수정을 두 곳에 반영해야 함), 정작 실제 결제 플로우를 검증할 방법이 따로 필요해집니다. 대신 **Google Play Console의 License Testing**을 사용합니다:
> 1. 앱을 Play Console에 등록하고 "광고 제거" IAP 상품을 만든 뒤, Internal Testing 트랙에 한 번 업로드합니다(공개 배포 아님, Play Console에 앱/상품을 인식시키기 위한 최소 조건).
> 2. 개발자 본인의 Google 계정을 License Tester로 등록합니다.
> 3. 이후로는 Android Studio에서 평소처럼 빌드 → 기기에 직접 실행(USB/무선 디버깅)하면 됩니다. License Testing은 Play 스토어에서 설치한 빌드나 정식 서명 키로 서명된 빌드를 요구하지 않으므로, `applicationId`만 Play Console에 등록한 것과 동일하면 디버그 서명 빌드에서도 "광고 제거" 버튼이 **실제 과금 없이 테스트 결제**로 동작합니다.
> 4. 따라서 앱은 **단일 코드베이스, 단일 `applicationId`**로 유지합니다. Play Console 등록/License Tester 등록은 반복 작업이 아니라 최초 1회 설정입니다.

### Phase 3 — 스토어 심사 통과를 위한 필수 준비물

| 항목 | 내용 |
|---|---|
| **개인정보처리방침 URL** | 광고·결제가 있는 앱은 필수입니다. 웹에 게시하고 Play Console 등록정보에 링크합니다. |
| **Data Safety 설문** | 광고 ID, 기기정보 수집 항목을 AdMob/Billing 기준으로 정확히 기재합니다. |
| **콘텐츠 등급 설문** | Play Console IARC 설문을 진행합니다. |
| **타겟 API 레벨 재확인** | 현재 `targetSdk 34` — 제출 시점에 Play가 신규/업데이트 앱에 요구하는 최소 타겟 API 레벨을 다시 확인합니다. |
| **릴리즈 빌드 난독화** | 현재 `app/build.gradle.kts`의 release 빌드는 `isMinifyEnabled = false`입니다. 스토어 배포 품질이라면 R8을 활성화하고, `proguard-rules.pro`에 Room/Hilt/AdMob/Billing에 필요한 keep 규칙이 빠지지 않았는지 검증합니다. |
| **크래시 리포팅** | Firebase Crashlytics 등 도입을 검토합니다 (현재 크래시 가시성이 전무합니다). |
| **스토어 등록 에셋** | 아이콘/스크린샷(폰), 짧은/긴 설명, 512x512 아이콘, 그래픽 배너 이미지를 준비합니다. |
| **Play App Signing** | 기존 keystore 백업 정책(7절)에 더해 Play App Signing 사용을 권장합니다. |

### Phase 4 — 기존 5절 로드맵과의 우선순위 조정

* **과제 2(테마 수동 선택)**: Phase 0에서 만들 설정 화면과 자연히 묶여서 함께 구현 → 우선순위 상향
* **과제 1(수동 순서 변경) / 3(TaskType 확장) / 4(백업·복원)**: 스토어 1차 출시와 직접적인 관련이 없으므로 출시 이후로 순연 권장
