# 📋 개발 인수인계 문서 (Handover Document - v1.0.0)

본 문서는 **오늘노트(TodayNote)** 프로젝트(구 Simple Bullet Journal — 13절)의 아키텍처 설계 배경, 주요 구현 상세, 그리고 **v1.1+ 개발자가 즉시 작업을 이어갈 수 있도록 필요한 기술적 맥락과 로드맵**을 상세히 기술합니다.

> 2026-09-19~20에 실제 소스 코드(`app/src`)를 전수 대조해 최신화했고, 2026-09-27 PM 인수인계 리뷰에서 다시 전수 대조했습니다(9~11절 추가).

---

## 🚦 현재 상태 요약 (2026-09-27 기준, 다음 담당자용 TL;DR)

**한 줄 요약**: **출시에 필요한 코드 작업은 끝났고 main에 push되어 있습니다.** 앱 이름은 **오늘노트 / TodayNote**, 패키지 ID는 **`com.zeonerd.todaynote`**(13절). 남은 건 Play Console 계정·등록 작업(11절 절차)과, 신규 개인 계정이면 **12명 × 14일 비공개 테스트**입니다.

> ⚠️ 2026-09-20 버전 문서는 "코드 관점에서 막힌 것은 없다"고 했으나, 당시 Billing 7.1.1을 쓰고 있어 **Play 정책(2026-08-31부터 Billing 8 이상 필수)상 업로드 자체가 거절되는 상태**였습니다. 9절 참고.

### ✅ 완료된 것
- 핵심 기능: 줄공책 UI, 이월, 중요도, 홈 위젯, 날짜 탐색
- **설정 화면**: 테마 선택(시스템/라이트/다크), 광고 제거 구매/복원, (EEA 등) 광고 개인정보 설정
- **AdMob 배너 광고**: 테스트 ID로 동작 확인, 실제 ID는 문서에 대기 중(8절 Phase 1) — 프로덕션 출시 직전에만 교체
- **Google Play Billing 8.3.0**: 상품 ID `remove_ads_sbj`, 결제 결과 안내·환불 반영 — Play Console 상품 등록 후 실결제 검증만 남음
- **targetSdk 36, R8 난독화, AAB 빌드**(`./gradlew bundleRelease`) 확인
- **개인정보처리방침 게시**: https://zeonerd.github.io/simple-bullet-journal/privacy-policy.html (HTTP 200 확인)
- **스토어 등록 에셋**: 아이콘, 그래픽 배너, Play 규격(2:1)으로 자른 스크린샷 4장(`store_assets/screenshots_play/`), 설명 문구 초안
- **출시 리스크 5건 해결 + 실기기 검증(9절)**, **1차 출시 사용성 개선 2건 + 실기기 검증(10절)** — 빠른 기록(연속 입력), 지난 미완료 전체 이월
- **데이터 백업/복원(12절)**: 설정 → 백업 파일 만들기 / 백업에서 복원(전체 교체)
- 버전: `versionName 1.0.0` / `versionCode 2`, 단위테스트 36/36

### ⏸ 사용자(개발자 본인)의 액션이 필요한 것 — 순서는 11절 참고
1. **Play Console 개발자 계정 생성 + 등록비 결제 + 본인 확인**
2. 앱 생성 → 앱 콘텐츠 선언 → 스토어 등록정보 (11절 B)
3. **Play 앱 서명 방식 결정**(첫 업로드 전, 되돌리기 어려움 — 11절 C1). 백업/복원 기능으로 기본값(Google 생성 키)도 무리 없음
4. 결제 프로필 생성 → `remove_ads_sbj` 인앱상품 등록 → License Tester 등록 → 실결제 검증
5. (신규 개인 계정) 테스터 12명 모집 → 비공개 테스트 14일 → 프로덕션 액세스 신청
6. 프로덕션 출시 직전: 실제 AdMob ID 교체 + `versionCode` 3 (11절 E)
7. (선택) Firebase Crashlytics 도입

### ⚠️ 확인 필요
- `store_assets/screenshots/05_widget.png`는 개인정보(카드명·일정·동네·개인 배경화면)가 찍혀 있어 2026-09-27 **삭제**. 단, **git 이력(커밋 79a1c2c 이후)에는 남아 있음** — 완전 제거하려면 이력 재작성 + 강제 push 필요(미실시). 위젯 스크린샷이 필요하면 단색 배경화면에서 우리 위젯만 보이게 재촬영.
- 개인정보처리방침 2026-09-27 개정·재게시 완료: 결제 저장 항목 정정(영수증 토큰 → 구매 여부 값만), AdMob 수집 항목에 진단 정보·목적 추가(데이터 보안 초안과 일치), UMP 동의 변경 메뉴 안내 추가.

### 다음 담당자가 지금 바로 할 수 있는 것 (계정 없이도 가능)
- 5절 출시 후 백로그 중 착수
- 스크린샷을 한글 샘플 데이터로 재촬영(현재 영문 placeholder)

---

## 1. 아키텍처 개요 및 설계 원칙

### 1.1 계층 분리 (Clean MVVM)
* **Data 계층**: `Room` + `TaskRepository` 추상화
  * DAO를 직접 ViewModel에서 참조하지 않고 반드시 `TaskRepository` 인터페이스를 거칩니다.
  * 모든 쿼리는 날짜(`date`, `yyyy-MM-dd`) 기준으로 필터링되며, `isPriority DESC, orderIndex ASC, createdAt ASC` 순으로 정렬됩니다.
  * **설정값 저장**: 구조화 데이터(Room)와 별도로 key-value 설정값은 `androidx.datastore:datastore-preferences` + `UserPreferencesRepository`(`UserPreferencesRepositoryImpl`)로 관리합니다. `UserPreferences(isAdRemoved, themeMode)`를 `Flow`로 노출하며, `SettingsViewModel`이 구독해 `SettingsScreen`(테마 선택)과 `BillingRepositoryImpl`(구매 완료 시 `isAdRemoved` 갱신)에서 실제로 소비합니다.
* **DI 계층**: `Google Hilt`
  * `DatabaseModule` 한 파일에서 `AppDatabase`, `TaskDao`, `TaskRepository`(`@Provides`로 `TaskRepositoryImpl` 반환)를 모두 제공합니다. (과거 문서에 있던 별도 `RepositoryModule`/`@Binds` 구조는 실제 코드에 없습니다.)
  * `DataStoreModule`에서 `DataStore<Preferences>`(`preferencesDataStore(name = "user_preferences")`)와 `UserPreferencesRepository`를 싱글톤으로 제공합니다.
* **Presentation 계층**: `Jetpack Compose` + `TaskViewModel`
  * `TaskViewModel`은 `@HiltViewModel` + `AndroidViewModel(application)`을 상속하며, 위젯 갱신을 위해 `GlanceAppWidgetManager`/`updateAll`을 호출합니다.
  * 단위 테스트 환경에서 Android Framework 의존성 충돌을 방지하기 위해 `widgetUpdater: suspend () -> Unit` 람다 프로퍼티를 주입 가능하게 설계하였습니다.
* **Widget 계층**: `Jetpack Glance`
  * 앱이 종료된 상태에서도 원격 뷰를 통해 데이터 조회/수정/이월이 가능하도록 `ToggleTaskAction`, `MigrateTasksAction`을 `ActionCallback`으로 구현했습니다.
  * ⚠️ 위젯 쪽은 Hilt 그래프를 타지 않습니다. `provideGlance`/`ToggleTaskAction`/`MigrateTasksAction` 각각에서 `TaskRepositoryImpl(AppDatabase.getInstance(context).taskDao())`를 직접 `new`합니다. 자세한 내용은 3절과 6절("확인된 이슈") 참고.

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
  * **2026-09-27 변경**: 예전에는 `fallbackToDestructiveMigration(dropAllTables = true)`가 켜져 있어 마이그레이션이 하나라도 빠지면 사용자 기록 전체가 **조용히 삭제**됐습니다. 지금은 `fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)`로 **출시 전 개발 빌드에만 있던 v1/v2에서만** 초기화를 허용합니다. 그 외 버전에서 마이그레이션이 빠지면 크래시로 드러납니다(데이터 유실보다 낫고, 업그레이드 테스트에서 바로 잡힘).
  * **스키마 이력**: `exportSchema = true` + KSP 인자 `room.schemaLocation`으로 `app/schemas/com.simple.bulletjournal.data.AppDatabase/4.json`이 생성됩니다. **반드시 커밋**하고, 스키마를 바꿀 때마다 새 `N.json`도 함께 커밋할 것. (v3 이하 JSON은 당시 export를 안 해서 없음)
  * 신규 스키마 변경 절차: `version` +1 → `Migration(N, N+1)` 추가 → `addMigrations` 등록 → 생성된 JSON 커밋 → 구버전 설치 후 업데이트 경로를 실기기에서 확인.

---

## 3. 홈 화면 위젯(Glance) 연동 주의사항

* 위젯은 Compose UI와 문법이 유사하나, **Jetpack Glance 전용 컴포넌트**(`androidx.glance.*`)만 사용해야 합니다.
* 앱 내부에서 태스크 변경 시 ViewModel에서 `updateWidget()`을 통해 모든 활성 위젯을 자동 갱신합니다.
* 위젯에서 할 일 체크 또는 이월 클릭 시에는 `ActionCallback`(`ToggleTaskAction`, `MigrateTasksAction`)에서 DB를 직접 조작한 뒤 `BulletJournalWidget().update(context, glanceId)`를 호출합니다.
* 이월 로직은 앱(`TaskViewModel`)과 위젯(`MigrateTasksAction`)이 모두 `TaskRepository.migrateUncompletedTasks(fromDate, untilDate, toDate)` 하나를 공유합니다(원본 날짜 범위 `fromDate` 이상 `untilDate` 미만). 실제 구현은 `TaskDao`의 `@Transaction` 메서드라 연속 클릭·동시 실행에도 중복 이월이 생기지 않습니다(2026-09-27). 위젯은 항상 "오늘 이전 전체 → 오늘"(`EARLIEST_TASK_DATE` ~ 오늘)입니다. 앱 쪽 규칙은 10.2절 참고.
* 위젯 색상은 `ui/theme/Color.kt`의 라이트/다크 팔레트를 `ColorProvider(day = ..., night = ...)`로 그대로 재사용해 앱 본체와 톤을 맞춥니다.
* `BulletJournalWidget.provideGlance`, `ToggleTaskAction`, `MigrateTasksAction` 세 곳 모두 `TaskRepositoryImpl(AppDatabase.getInstance(context).taskDao())`을 직접 생성해서 사용합니다(Hilt 미사용). Repository 생성자 시그니처가 바뀌면 이 세 곳을 모두 함께 고쳐야 합니다.

---

## 4. 단위 테스트 작성 및 유지보수 규칙

* 단위 테스트는 `app/src/test/java/com/simple/bulletjournal/`에 위치합니다.
* 테스트 시 Room DB 대신 **`FakeTaskRepository`**를 사용합니다.
* **주의**: `TaskDao`의 정렬 쿼리(`isPriority DESC, orderIndex ASC, createdAt ASC`)가 변경되면, `FakeTaskRepository`의 `sortedWith` 로직도 반드시 동일하게 맞춰주어야 테스트 일관성이 유지됩니다.
* **주의**: `FakeTaskRepository`의 `getMigratableTasks(Once)` / `migrateUncompletedTasks`는 `TaskDao`의 같은 이름 메서드 규칙(날짜 범위, 미완료·미이월 필터, `date ASC` → 화면 정렬 순, `createdAt`에 순번 가산, 원본에 `isMigrated` 표시)을 그대로 복제한 것입니다. DAO 쪽 쿼리·로직이 바뀌면 함께 맞출 것. **Room 쿼리 자체는 단위테스트로 검증되지 않으므로**(androidTest 미구축) 변경 시 실기기 확인 필수.
* **주의**: `FakeTaskRepository.insertTask`는 `id == 0L`일 때만 자동으로 id를 채번합니다(`nextId++`). 테스트에서 사전 데이터를 만들 때 `Task(id = 1, ...)`처럼 id를 직접 지정하면 이 카운터와 충돌해 엉뚱한 레코드가 덮어써질 수 있습니다(6절 이슈 4 참고) — **테스트 데이터의 id는 항상 기본값(0, 자동 할당)으로 둘 것.**
* 테스트 실행 명령어:
  ```bash
  ./gradlew testDebugUnitTest
  ```

---

## 5. v1.1+ 권장 개발 과제 (Next Roadmap)

다음 세션 개발자가 우선적으로 구현하기 좋은 추천 백로그입니다.

> ⚠️ **우선순위 조정 (2026-09-20)**: 스토어 출시(8절 참고)가 목표로 확정되면서 아래 4개 과제 중 **과제 2(테마 수동 선택)**는 8절 Phase 0의 설정 화면과 함께 구현되어 ✅ **완료**되었습니다. 과제 1/3/4는 스토어 1차 출시와 직접 관련이 없으므로 **출시 이후로 순연**을 권장합니다.

### 과제 1: 할 일 수동 순서 변경 (Manual Reordering)
* **현황**: `Task` 엔티티에 이미 `orderIndex: Int` 필드와 쿼리(`orderIndex ASC`)가 준비되어 있습니다.
* **구현 방향**:
  * **방안 A (추천)**: `TaskOnLine` 우측 옵션 메뉴 또는 위/아래 이동 버튼(▲/▼)을 통해 인접 항목과 `orderIndex`를 스왑(`swapOrder(task1, task2)`).
  * **방안 B**: `MainScreen`의 `Column + verticalScroll`을 `LazyColumn` + `reorderable` 제스처 라이브러리로 마이그레이션하여 길게 눌러 드래그 앤 드롭 구현.

### 과제 2: 앱 내 테마 수동 선택 옵션 — ✅ 2026-09-20 완료
* **구현 내용**: `DataStore Preferences`(`UserPreferencesRepository.themeMode`)에 [시스템 기본 / 라이트 / 다크]를 저장하고, `SettingsScreen`에 라디오 버튼 3개로 노출. 상태바 아이콘 대비까지 포함해 실기기 검증 완료(6절 이슈 6 참고).

### 과제 3: 정통 불렛 저널 기호 체계 확장
* **현황**: 현재는 완료 여부(`isCompleted`)와 중요도(`isPriority`)만 지원.
* **구현 방향**:
  * 태스크 타입을 구분하는 `TaskType` enum 도입:
    * `TASK` (`•`) : 할 일
    * `EVENT` (`○`) : 약속/이벤트
    * `NOTE` (`-`) : 단순 메모/기록
  * 불렛 아이콘 클릭 시 타입을 순환 변경하거나 선택할 수 있는 UX 제공.

### 과제 4: 데이터 백업 및 복원 — ✅ 2026-09-27 완료(12절, JSON 파일 내보내기/가져오기)
* **구현 방향(당시 계획)**:
  * 사용자가 작성한 전체 저널 데이터를 JSON 또는 CSV 파일로 로컬 저장소/드라이브에 내보내기 및 복원(Import/Export) 기능.

### 과제 5: 라이트 테마 상태바 아이콘 대비 개선 — ✅ 2026-09-20 해결 (6절 "이슈 6" 참고)

### 출시 후 백로그 (2026-09-27 PM 제안, 우선순위 순)
1. **항목별 이월 선택(가져오기/버리기)** + 이월된 원본에 `>` 표시·흐리게 — 불렛저널 핵심. 끝내 안 할 일이 오늘 배너에 계속 잡히는 한계(10.2절) 해결. `migrateUncompletedTasks`에 id 목록 오버로드 추가로 구현 가능
2. **과제 3(TaskType: 할 일/일정/메모)** — 스키마 변경 필요. 착수 전 Room `MigrationTestHelper`(androidTest) 도입 권장
3. 좌우 스와이프 날짜 이동, 삭제 확인창 → 되돌리기 스낵바, 긴 할 일 2줄 표시, 위젯 "+" 빠른 추가, 달력에 기록 있는 날 표시
4. 키보드가 올라와 있는 동안 배너 광고 숨김(광고 노출 수와 트레이드오프, 10.3절 관찰)
5. 위젯 자정 즉시 갱신(현재 최대 30분 지연, 9.5절)
6. 과제 1(수동 순서 변경)은 종이 공책 감성·별표 우선순위와 겹쳐 **후순위**로 조정

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

### 이슈 4: `migrateYesterdayTasks` 단위 테스트가 타임아웃 실패함 — ✅ 2026-09-20 해결
* **증상**: `TaskViewModelTest.migrateYesterdayTasks_copiesOnlyUncompletedTasksToCurrentDate`가 `app.cash.turbine.TurbineAssertionError: No value produced in 3s`로 매번(간헐적이 아니라 결정적으로) 실패했습니다.
* **근본 원인**: "간헐적"이라고 처음엔 오판했으나, 실제로는 **테스트 코드 자체의 ID 충돌 버그**였습니다. 테스트가 사전 데이터를 넣을 때 `Task(id = 1, ...)`, `Task(id = 2, ...)`로 id를 명시적으로 지정했는데, `FakeTaskRepository`의 자동 증가 카운터(`private var nextId = 1L`)는 이 명시적 id를 전혀 인지하지 못하고 여전히 1부터 시작합니다. `migrateYesterdayTasks()`가 오늘 날짜로 새 태스크를 `insertTask`(id 자동 할당 → 우연히 1)한 직후, 원본 어제 태스크를 `updateTask(task.copy(isMigrated = true))`로 갱신하는데, `FakeTaskRepository.updateTask`는 **id 일치 여부로 교체 대상을 찾기 때문에** 방금 넣은 "오늘" 태스크(id=1)까지 "어제" 태스크 내용으로 덮어써버렸습니다. 결과적으로 "오늘" 목록에는 아무것도 안 남아 `tasks` StateFlow가 갱신되지 않고 Turbine이 영원히 기다리게 됐습니다.
* **해결**: 테스트의 사전 데이터에서 명시적 `id` 지정을 제거하고 `FakeTaskRepository`의 자동 할당(`id = 0` 기본값)에 맡기도록 수정. 5회 연속 재실행으로 결정적 통과 확인(20/20 전부 통과).
* **교훈**: Fake/Mock Repository에 자동 증가 ID 카운터가 있다면, 테스트 데이터에 수동으로 id를 지정하지 말 것 — 카운터와 충돌하면 이번처럼 완전히 다른 레코드가 조용히 덮어써지는 사고로 이어질 수 있습니다.

### 이슈 5: `BulletJournalApp`이라는 이름의 최상위 컴포저블이 `Application` 클래스와 충돌해 화면이 완전히 빈 채로 렌더링됨 — ✅ 2026-09-20 해결
* **증상**: 앱을 실기기에 설치해서 실행하면 크래시 없이 정상적으로 "Displayed"까지 되지만, 화면에는 배경색만 채워지고 어떤 UI도 그려지지 않았습니다(설정 화면 진입용 톱니바퀴 아이콘도 당연히 안 보였습니다). `uiautomator dump`로 확인해도 `android:id/content` 아래에 자식 뷰가 단 하나도 없었고, `logcat`(main/system/crash 버퍼 전부) 어디에도 예외나 크래시 로그가 없었습니다.
* **근본 원인**: [`MainActivity.kt`](app/src/main/java/com/simple/bulletjournal/MainActivity.kt)에서 `setContent { BulletJournalApp() }`로 최상위 컴포저블을 호출하고 있었는데, 이 컴포저블 함수의 이름이 **같은 패키지의 `BulletJournalApp.kt`에 있는 `class BulletJournalApp : Application()`과 완전히 동일**했습니다. `Application` 클래스는 암묵적으로 인자 없는 public 생성자를 가지므로, `BulletJournalApp()`이라는 호출식이 우리가 만든 컴포저블 함수(파라미터 `settingsViewModel: SettingsViewModel = hiltViewModel()`에 기본값이 있어 인자 없이도 호출 가능)와 `Application`의 생성자 호출 사이에서 **오버로드 해석 시 컴파일 에러나 경고 없이 조용히 생성자 쪽으로 resolve**되었습니다. 즉 매번 새 `BulletJournalApp` 인스턴스를 만들어서 버리기만 했을 뿐, 실제 UI 컴포저블은 단 한 번도 실행되지 않았습니다.
* **디버깅 방법**: `Log.d`를 `onCreate()`와 컴포저블 최상단에 순서대로 심어서 어디까지 로그가 찍히는지 이분탐색했습니다. `setContent`의 람다 진입 로그는 찍혔지만, 컴포저블 함수 본문의 첫 줄 로그는 전혀 찍히지 않는 것으로 좁혀졌고, 이는 "함수가 아예 호출되지 않았다"는 뜻이었습니다.
* **해결**: 컴포저블 함수 이름을 `BulletJournalApp` → `BulletJournalRoot`로 변경. 실기기(Samsung SM-S711N, release 서명 빌드)에서 메인 화면·설정 화면·뒤로가기까지 전부 정상 동작 확인했습니다.
* **교훈**: 같은 패키지 안에서 클래스명과 최상위 함수명이 겹치면(특히 그 클래스가 인자 없는 생성자를 가진 경우) Kotlin이 경고 없이 엉뚱한 쪽을 호출할 수 있습니다. 향후 최상위 컴포저블 함수는 `XxxApp`처럼 `Application` 서브클래스와 헷갈릴 수 있는 이름을 피하고, 이번처럼 `XxxRoot` 또는 `XxxScreen` 계열로 명명할 것.

### 이슈 6: 라이트 테마에서 상태바 아이콘이 흰색에 가까워 식별 불가능함 — ✅ 2026-09-20 해결
* **증상**: 설정에서 테마를 "라이트"로 선택하면 앱 배경은 밝은 줄공책 색(`LightPaper`)으로 바뀌지만, 상단 상태바(시계/배터리/네트워크 아이콘)는 여전히 흰색에 가까운 밝은 색으로 남아 있어 사람 눈으로 거의 식별이 안 됐습니다.
* **근본 원인**: [`MainActivity.kt`](app/src/main/java/com/simple/bulletjournal/MainActivity.kt)의 `onCreate()`에서 `enableEdgeToEdge()`를 인자 없이 한 번만 호출했습니다. 이 함수는 호출 시점의 **시스템(OS) 다크모드 여부**를 기준으로 상태바 아이콘 밝기(라이트/다크 아이콘)를 자동 결정하는데, 이는 우리 앱이 `SettingsViewModel`을 통해 자체적으로 계산하는 `darkTheme`(사용자가 고른 시스템/라이트/다크 설정)과 **서로 다른 값**이었습니다. 폰의 OS 자체는 다크모드인데 앱 내에서 "라이트"를 선택하면, `enableEdgeToEdge()`는 OS 기준으로 "어두운 배경이니 밝은 아이콘"으로 한 번 결정하고 끝나버리고, 이후 `BulletJournalTheme`이 실제로는 밝은 배경을 그려도 상태바 아이콘 색은 갱신되지 않았습니다.
* **영향 범위**: 폰 OS가 다크모드 + 앱 내 테마를 "라이트"로 선택한 조합에서만 발생. "시스템 기본"을 쓰거나 OS와 앱 테마가 같은 방향이면 우연히 맞아떨어져 문제가 드러나지 않았습니다.
* **해결**: `BulletJournalRoot`(`MainActivity.kt`)에 `darkTheme` 값이 바뀔 때마다 실행되는 `SideEffect`를 추가해 `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme`를 호출하도록 수정. `enableEdgeToEdge()`의 최초 1회 자동 감지에만 의존하지 않고, 실제 앱 테마 상태에 반응형으로 맞춥니다.
* **검증**: 실기기(Samsung SM-S711N, OS 다크모드 고정, release 서명 빌드)에서 앱 내 테마를 라이트→다크→라이트로 전환하며 확인. 라이트 선택 시 상태바 아이콘이 검은색으로 또렷하게 바뀌고, 다크 선택 시 다시 흰색으로 정상 복귀(회귀 없음).

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
   * 새 버전 릴리즈 시 `app/build.gradle.kts`의 `versionCode`를 1씩 증가시키고 `versionName`을 갱신합니다. (현재 `versionCode = 2`, `versionName = "1.0.0"` — 2026-09-27 첫 공개 출시 버전으로 확정. 이전에 `1.1.0-dev`였으나 스토어 미출시 상태라 1.0.0으로 정리)

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
| **설정 화면(SettingsScreen) 신설** | ✅ 완료(2026-09-20): `SettingsScreen` + `SettingsViewModel` 추가, `MainScreen` 상단에 톱니바퀴 아이콘으로 진입. 화면 전환은 `NavHost` 없이 `MainActivity`의 로컬 상태(`mutableStateOf<Boolean>`)로 `MainScreen` ↔ `SettingsScreen` 토글(화면이 2개뿐이라 `NavHost`는 과함). 테마 라디오 3개(시스템/라이트/다크)는 `MainActivity`가 `SettingsViewModel.userPreferences`를 구독해 `BulletJournalTheme(darkTheme=...)`에 실시간 반영. 광고 제거 버튼은 이후 Phase 2에서 실제 `BillingRepository.purchaseAdRemoval()` 호출로 교체 완료(더 이상 임시 직접 호출 아님 — Phase 2 절 참고). 빌드/단위테스트/실기기 화면 검증(테마 전환, 설정 진입·뒤로가기) 모두 통과. 실기기 검증 과정에서 발견된 별개의 렌더링 버그는 6절 이슈 5 참고. |
| **DataStore Preferences 도입** | ✅ 완료(2026-09-20): `UserPreferencesRepository`/`DataStoreModule` 추가, 단위 테스트 3개 통과. `isAdRemoved`/`themeMode` 저장 가능. **아직 UI/ViewModel에서 실제로 쓰이진 않음** — 설정 화면에서 주입해 소비하는 게 다음 작업. |

### Phase 1 — 광고 (AdMob 배너) — ✅ 완료(2026-09-20, 테스트 광고 ID 기준)

1. ✅ `com.google.android.gms:play-services-ads`(23.6.0) + `com.google.android.ump:user-messaging-platform`(3.1.0) 의존성 추가. `AndroidManifest.xml`에 AdMob `APPLICATION_ID` meta-data 등록(현재 Google 공식 테스트 App ID `ca-app-pub-3940256099942544~3347511713` — `TODO(Phase 1 출시 전)` 주석으로 실제 ID 교체 지점 표시).
2. ✅ `INTERNET`, `ACCESS_NETWORK_STATE` 권한 추가 — **이 앱 최초의 네트워크 권한**이며, Data Safety 신고 대상입니다.
3. ✅ [`ui/ads/BannerAd.kt`](app/src/main/java/com/simple/bulletjournal/ui/ads/BannerAd.kt): `AndroidView`로 `AdView`를 래핑한 컴포저블. `remember`로 `AdView` 인스턴스를 보관하고 `DisposableEffect`로 `loadAd`/`destroy` 생명주기를 관리합니다. 테스트 배너 광고 단위 ID(`ca-app-pub-3940256099942544/6300978111`) 사용 중 — 실제 ID로 교체할 지점에 `TODO(Phase 1 출시 전)` 주석 표시.
4. ✅ **UMP(User Messaging Platform) 동의 플로우**: *(2026-09-27에 `data/AdsConsentRepositoryImpl.kt`로 이전 — 9.3절 참고. 아래는 당시 구현 기록)* [`BulletJournalApp.kt`](app/src/main/java/com/simple/bulletjournal/BulletJournalApp.kt)의 `requestConsentAndInitializeAds(activity, onReady)`가 `ConsentInformation.requestConsentInfoUpdate` → 필요 시 `loadAndShowConsentFormIfRequired` → 동의 완료(`canRequestAds() == true`) 후에만 `MobileAds.initialize()`를 호출합니다. `MainActivity.onCreate()`에서 이 함수를 호출하고, 완료 콜백에서 `adsReady = true`(Compose `mutableStateOf`)로 배너 노출을 트리거합니다.
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

1. ✅ `com.android.billingclient:billing-ktx`(7.1.1) 추가. → **2026-09-27 8.3.0으로 상향**(9.1절).
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
| **스토어 등록 에셋** | ✅ 완료(2026-09-20): 짧은/긴 설명 초안([`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) 하단), 512x512 Hi-res 아이콘, 1024x500 그래픽 배너, 실기기 스크린샷 5장 — 모두 [`store_assets/`](store_assets)에 준비됨. 아이콘/배너는 재생성 가능한 Python 스크립트([`store_assets/scripts/`](store_assets/scripts))도 함께 커밋됨. ⚠️ 스크린샷 원본 비율(1080:2340)이 Play 권장 최대 비율(2:1)을 살짝 초과 — 업로드 시 거부되면 크롭 필요(`store_assets/README.md` 참고). |
| **Play App Signing** | Play Console 앱 등록 시 함께 설정 — 사용자 액션 대기. |

### Phase 4 — 기존 5절 로드맵과의 우선순위 조정

* **과제 2(테마 수동 선택)**: ✅ Phase 0의 설정 화면과 함께 완료됨
* **과제 1(수동 순서 변경) / 3(TaskType 확장) / 4(백업·복원)**: 스토어 1차 출시와 직접적인 관련이 없으므로 출시 이후로 순연 권장
* **과제 5(라이트 테마 상태바 대비)**: ✅ 완료됨 (버그 수정 건, 6절 이슈 6 참고)

---

## 9. 출시 리스크 대응 (2026-09-27, PM 인수인계 리뷰)

인수인계 시점에 소스 전수 검토로 발견한 출시 리스크와 조치 내역입니다. 단위테스트 26/26 통과(기존 20 + 신규 6), `assembleDebug`/`assembleRelease`(R8) 빌드 성공까지 확인했습니다. **연결된 기기·에뮬레이터가 없어 실기기 검증은 하지 못했습니다** — 9.6절 체크리스트를 먼저 수행할 것.

### 9.1 🔴 Play Billing Library 7 → 8.3.0 (출시 불가 사유)
* **문제**: Google Play는 2026-08-31부터 Billing Library 8 미만을 쓰는 신규 앱·업데이트 업로드를 거절합니다(연장은 Play Console에서 2026-11-01까지 별도 신청). 7.1.1 그대로면 첫 업로드부터 막힙니다.
* **조치**: `billing = "8.3.0"`. v8에서 `queryProductDetailsAsync` 콜백이 `QueryProductDetailsResult`를 받도록 바뀐 부분 대응. `enableAutoServiceReconnection()`을 켜고, 예전의 `onBillingServiceDisconnected → connect()` 수동 재귀 재연결을 제거했습니다.
* 9.x도 나와 있으나, 정책 요건은 8 이상이므로 마이그레이션 자료가 충분한 8.3.0을 택했습니다. 다음 정책 기한(통상 2년 주기)에 맞춰 재검토할 것.

### 9.2 🟠 DB 파괴적 마이그레이션 제한 + 스키마 이력
* 2절 참고. `fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)`, `exportSchema = true`, `app/schemas/` 생성.
* **남은 과제**: Room `MigrationTestHelper` 기반 마이그레이션 테스트는 `androidTest` 환경이 아직 없어 추가하지 못했습니다. 다음 스키마 변경(예: `TaskType`) 전에 도입 권장.

### 9.3 🟠 UMP 개인정보 옵션(동의 변경) 진입점
* **문제**: EEA/영국 사용자는 광고 동의를 언제든 바꿀 수 있어야 하는데, 진입점이 없었습니다.
* **조치**: 동의 로직을 `Application`에서 [`data/AdsConsentRepository(Impl)`](app/src/main/java/com/simple/bulletjournal/data/AdsConsentRepositoryImpl.kt)로 옮기고 `di/AdsModule.kt`로 Hilt 싱글톤 제공. `privacyOptionsRequirementStatus == REQUIRED`일 때만 설정 화면에 "광고 개인정보 설정" 행이 나타나며, 누르면 `showPrivacyOptionsForm`을 띄웁니다. 동의 철회 시 `canShowAds`도 다시 계산합니다.
* 부수 수정: 예전 코드는 동의 콜백 두 경로에서 `MobileAds.initialize`가 중복 호출될 수 있었음 → `AtomicBoolean`으로 1회 보장. 동의 정보 갱신 실패(오프라인) 시에도 이전 세션 동의로 광고 초기화를 시도합니다(Google 권장 패턴).
* `MainActivity`는 `@Inject AdsConsentRepository`로 `gatherConsent(this)`를 호출하고 `canShowAds` StateFlow를 구독합니다. `BulletJournalApp`은 이제 빈 `@HiltAndroidApp` 클래스입니다.

### 9.4 🟡 결제 피드백 + 환불 반영
* **문제**: 상품 정보를 못 불러온 상태에서 "광고 제거"를 누르면 아무 반응이 없었고, "구매 복원"도 결과를 알려주지 않았습니다. 환불돼도 `isAdRemoved`가 영구히 `true`로 남았습니다.
* **조치**: `BillingRepository.notices: Flow<BillingNotice>`(상품 준비 안 됨/결제 보류/결제 실패/복원 완료/복원할 내역 없음/복원 실패)를 추가하고, `SettingsScreen`이 스낵바로 표시합니다. 사용자 취소는 알리지 않습니다. `ITEM_ALREADY_OWNED`는 구매 내역 재동기화로 처리합니다.
* **환불 반영**: 앱 시작 시/복원 시 구매 조회가 **성공했는데** 해당 상품의 `PURCHASED` 구매가 없으면 `isAdRemoved = false`로 되돌립니다. 조회 자체가 실패하면(오프라인 등) 상태를 건드리지 않습니다.

### 9.5 🟡 이월 트랜잭션화 + 자정 날짜 갱신
* **이월**: 앱·위젯에 중복돼 있던 이월 루프를 `TaskDao.migrateUncompletedTasks`(`@Transaction`)로 일원화(3절). 신규 테스트 `migrateTasks_calledTwice_doesNotDuplicate`(10절에서 이름 변경).
* **자정**: 앱을 켜둔 채 자정을 넘기면 "오늘" 페이지가 어제 날짜에 머물렀습니다. `TaskViewModel.onAppResumed()`를 `MainScreen`의 `LifecycleEventEffect(ON_RESUME)`에서 호출해, "오늘"을 보고 있던 경우에만 새 날짜로 옮깁니다(일부러 다른 날짜를 보던 경우는 유지). 테스트용으로 `today: () -> LocalDate`를 교체 가능하게 둠(`widgetUpdater`와 같은 방식). 의존성 `lifecycle-runtime-compose` 추가.
* 위젯은 `updatePeriodMillis = 30분` 주기라 자정 후 최대 30분간 전날 날짜가 보일 수 있습니다 — 출시 차단 사유는 아니라 보류.

### 9.6 실기기 검증 체크리스트 (다음 세션 최우선)
1. 기존 v4 DB에 할 일이 있는 상태의 이전 빌드 위에 새 빌드를 **덮어 설치** → 기록이 그대로 남는지 (마이그레이션/폴백 변경 확인)
2. 메인 화면 하단 테스트 배너 노출 (동의 로직 이전 후 회귀 없음 확인)
3. 설정 → "광고 제거" 클릭 → 상품 미등록 상태이므로 "지금은 결제를 진행할 수 없습니다" 스낵바 표시
4. 설정 → "구매 복원" → "복원할 구매 내역이 없습니다" 스낵바
5. 앱 이월 배너 "가져오기", 위젯 "가져오기 ➔" 빠르게 두 번 탭 → 중복 없이 한 번만 이월
6. (선택) UMP 디버그 지역을 EEA로 강제(`ConsentDebugSettings.setDebugGeography`)해 "광고 개인정보 설정" 행 노출·폼 동작 확인 — 테스트 후 디버그 설정은 반드시 제거
7. release 빌드(R8)로 위 1~5 반복

### 9.7 실기기 검증 결과 (2026-09-27, Samsung SM-S711N / Android 16, release 서명 빌드)
| # | 항목 | 결과 |
|---|---|---|
| 1 | 09-20 release 빌드(실사용 데이터 있음) 위에 새 빌드 덮어 설치 | ✅ 크래시 없음, 기존 기록(어제 미완료 4개 등) 그대로 유지 |
| 2 | 메인 화면 테스트 배너 노출 | ✅ 노출됨. **참고**: 업데이트 전에는 배너가 없었는데, 이는 Phase 0 시절 "광고 제거" 버튼이 결제 없이 `isAdRemoved = true`를 직접 세팅했던 흔적입니다. 새 구매 동기화 로직이 "Play에 구매 내역 없음"을 확인하고 정상적으로 해제한 것 — 의도된 동작 |
| 3 | 설정 → "광고 제거" (상품 미등록 상태) | ✅ "지금은 결제를 진행할 수 없습니다…" 스낵바 |
| 4 | 설정 → "구매 복원" | ✅ "복원할 구매 내역이 없습니다." 스낵바 |
| — | 한국 지역에서 "광고 개인정보 설정" 행 | ✅ 미노출(의도대로) |
| 5 | 이월 연속 실행 | ⏸ 실기기 미검증(실사용 데이터 보호) — 단위테스트로 확인. 이월 대상 조회 쿼리는 10.3절에서 실기기 검증됨 |
| 6 | EEA 강제 테스트 | ⏸ 미검증 — 테스트용 `ConsentDebugSettings` 코드가 필요해 보류 |
| 7 | release(R8) 빌드로 반복 | ✅ 9·10절 실기기 검증은 모두 release 서명 빌드로 수행 |

**검증 중 발견·수정한 버그 — 이슈 7: 설정 화면에서 시스템 뒤로가기를 누르면 앱이 종료됨** (기존 버그, 2026-09-27 해결)
* **원인**: 화면 전환을 `NavHost` 없이 `MainActivity`의 `showSettings` 로컬 상태로 하는데, `SettingsScreen`이 시스템 뒤로가기를 가로채지 않아 Activity가 그대로 종료됐습니다. 상단 ← 버튼만 동작했습니다.
* **해결**: `SettingsScreen`에 `BackHandler(onBack = onBack)` 추가. 화면이 늘어나 `NavHost`로 옮기기 전까지는, 새 화면을 추가할 때마다 같은 처리가 필요합니다.
* ✅ 실기기 재검증 완료(2026-09-27): 설정 진입 → 시스템 뒤로가기 → 메인 화면으로 복귀, 앱 유지 확인.

---

## 10. 1차 출시 사용성 개선 (2026-09-27)

PM 제안 중 사용자 확정분 2건. DB 스키마 변경 없음(버전 4 유지).

### 10.1 빠른 기록(Rapid Logging) — 연속 입력
* `MainScreen`의 할 일 추가 후 `focusManager.clearFocus()`를 없애 키보드·포커스를 유지 → 연달아 입력 가능.
* 빈 입력창에서 완료/추가를 누르면 입력 종료로 보고 키보드를 내림.
* 추가 직후 새 항목(가장 최근 `createdAt`)이 키보드에 가려지면 그 줄까지 자동 스크롤(`NotebookPage`의 `ScrollState`를 `MainScreen`으로 끌어올림).

### 10.2 이월 범위 확장 — 결정 사항
| 보고 있는 페이지 | 배너 | 이월 원본 범위 |
|---|---|---|
| 오늘 | "❭ 지난 미완료 할 일 N개" (+어제보다 오래된 게 있으면 "M월 d일부터") → 가져오기 | 오늘 **이전 전체**(기간 제한 없음) |
| 내일 | "❭ 오늘 남은 할 일 N개" → 내일로 | 오늘 |
| 그 외 날짜 | 없음 | — |

* **결정 배경(사용자 확정)**: 하루라도 앱을 안 열면 이전 미완료 할 일이 영영 사라지던 문제를 해결. 과거 페이지에서 "그 이전 전체"를 보여주면 헷갈리므로 오늘 페이지로 한정. 기존의 "내일 페이지에서 오늘 남은 일을 미리 옮기기" 사용성은 유지. 기간 제한은 두지 않음(불렛저널의 "의식적 이월" 취지).
* **구현**: `TaskViewModel.migrationOffer: StateFlow<MigrationOffer?>`(종류/개수/가장 오래된 날짜)와 `migrateTasks()`가 (선택 날짜, 오늘)로 종류를 판정. "오늘"은 `_today` StateFlow로 두어 자정이 지나면(`onAppResumed`) 배너가 다시 계산됨. 기존 `yesterdayUncompletedTasks` / `migrateYesterdayTasks()`는 제거.
* **알려진 한계**: 끝내 안 할 일(완료도 삭제도 안 한 항목)은 오늘 배너에 계속 잡힙니다. 현재는 완료 처리나 삭제만 가능 — 출시 후 과제 "항목별 가져오기/버리기 선택"으로 해결 예정. `migrateUncompletedTasks`가 날짜 범위를 받는 구조라, 선택한 항목 id 목록을 받는 오버로드를 추가하면 됩니다.
* **테스트**: 30/30 통과(신규 4: 지난 전체 집계·완료/이월/오늘/미래 제외, 여러 날 순서 유지, 내일 페이지 동작, 그 외 날짜 배너 없음).

### 10.3 실기기 검증 결과 (2026-09-27, SM-S711N, release 서명 빌드, 실사용 데이터)
| # | 항목 | 결과 |
|---|---|---|
| 1 | 미래 날짜(10/10)에 더미 8개 연속 입력 | ✅ 매 입력 후 키보드 유지(`mInputShown=true`), 마지막 항목(ZZ-8)이 입력창 바로 위에 보이도록 자동 스크롤, 빈 칸 완료 시 키보드 내림. 더미 8개 삭제 완료 |
| 2 | 오늘 페이지 배너 | ✅ "❭ 지난 미완료 할 일 10개 / 9월 18일부터". 9/17~9/26 페이지를 직접 대조: 화면상 미완료 11개 중 1개(9/23 항목)는 다음 날에 같은 항목이 있는 이미 이월된 원본이라 제외 → 10개 일치. 9/17은 과거에 통째로 이월된 날이라 제외 → 시작일 9/18 일치. **실제 Room 범위 쿼리 동작 확인** |
| 3 | 내일 페이지 배너 | ✅ 오늘에 더미 1개 추가 시 "❭ 오늘 남은 할 일 1개 / 내일로 ➔" 노출, 할 일 없을 땐 배너 없음. 더미는 이월하지 않고 삭제 |
| 4 | 위젯 문구 | ✅ "❭ 지난 미완료 10개" — 앱과 동일 |
| 5 | 실제 "가져오기" | ⏸ **사용자가 직접** 확인 예정(실사용 데이터 보호). 트랜잭션 이월 자체는 단위테스트로만 검증됨 |

* **관찰(후속 검토 거리)**: 입력 중에는 배너 광고가 입력창과 키보드 사이에 끼어 공책 영역이 더 좁아집니다. 키보드가 올라와 있는 동안 광고를 숨기는 방안을 출시 후 검토할 만합니다(광고 노출 수와의 트레이드오프).
* **검증 중 주의 사항**: 배너 광고가 있으면 입력창 위치가 위로 올라갑니다. 좌표 고정 탭은 광고를 누를 수 있으니, 기기 자동화 시 반드시 `uiautomator dump`의 요소 좌표를 사용할 것.

---

## 11. Play Console 등록 절차 (2026-09-27 작성)

> 결제·신분증·은행/세금 정보 입력은 **개발자 본인만** 할 수 있습니다. 각 단계의 "준비물"은 이 저장소에 이미 있습니다.

### A. 개발자 계정 (1~수일)
1. https://play.google.com/console 에서 개발자 계정 생성 — **개인** 또는 **조직**(D-U-N-S 번호 필요) 선택. 등록비 1회 결제, 신분증 본인 확인, 연락처(이메일·전화) 인증, Play Console 앱으로 기기 인증.
2. ⚠️ **2023-11-13 이후 생성한 개인 계정**은 프로덕션 출시 전 **비공개 테스트(테스터 12명 이상, 14일 연속 참여)**가 필수(D절). 조직 계정·그 이전 개인 계정은 면제.

### B. 앱 생성 + 앱 콘텐츠 + 스토어 등록정보
1. **앱 만들기**: 기본 언어 한국어, 앱, 무료, 정책 동의. 앱 이름: **`오늘노트 - 할 일, 투두리스트, 체크리스트`**(영문 등록정보 `TodayNote - To-do List & Daily Tasks`). 런처 이름은 `오늘노트`(영어 기기 `TodayNote`).
2. **앱 콘텐츠(정책 → 앱 콘텐츠)**
   | 항목 | 답변 |
   |---|---|
   | 개인정보처리방침 | `https://zeonerd.github.io/simple-bullet-journal/privacy-policy.html` |
   | 앱 액세스 권한 | 로그인 없음 — 모든 기능을 제한 없이 이용 가능 |
   | 광고 | 예, 광고 포함 |
   | 콘텐츠 등급(IARC) | 카테고리: 유틸리티·생산성 등. 폭력/성적/도박/약물 없음, 사용자 간 소통 없음, 위치 공유 없음, **디지털 상품 구매 있음(인앱결제)** → 전체이용가 예상 |
   | 타겟층 | **13세 이상 연령대만** 선택(방침 4절: 13세 미만 대상 아님). 13세 미만 포함 시 가족 정책·광고 추가 요건 발생 |
   | 데이터 보안 | `PRIVACY_POLICY.md`의 "Data Safety 설문 답변 초안"(2026-09-27 AdMob 공식 공개 기준으로 보강) |
   | 광고 ID | 사용함 — 광고, 분석, 사기 방지(AAB에 `AD_ID` 권한 포함 확인) |
   | 정부 앱 / 금융 기능 / 건강 | 해당 없음 |
3. **스토어 등록정보(성장 → 스토어 등록정보)**: 짧은/긴 설명은 `PRIVACY_POLICY.md` 하단 초안, 아이콘 `store_assets/hi_res_icon_512.png`, 그래픽 이미지 `store_assets/feature_graphic_1024x500.png`, 휴대전화 스크린샷 `store_assets/screenshots_play/` 4장(1080×2160, 2:1 규격). 카테고리: 생산성. 연락처 이메일: `zeonerd@proton.me`(방침과 동일).
4. **국가/지역**: UI가 한국어 전용이므로 대한민국 우선 권장.

### C. 첫 업로드 + 인앱결제 검증 (내부 테스트)
1. **Play 앱 서명 방식 — 첫 업로드 전에 결정(나중에 바꾸기 어려움)**
   * (권장, 2026-09-27 갱신) **Google이 생성한 키(기본값)**: 가장 간단하고 Google 권장.
   * 패키지 ID가 `com.zeonerd.todaynote`로 바뀌어(13절), 폰에 직접 설치했던 옛 앱(`com.simple.bulletjournal`, "불렛 저널")은 **서명과 무관하게 별개의 앱**입니다. 기록은 **① 옛 앱에서 "백업 파일 만들기" → ② 새 앱(오늘노트)에서 "백업에서 복원" → ③ 확인 후 옛 앱 삭제** 순서로 옮깁니다(옛 백업 파일도 복원됨, 12·13절).
   * (대안) "Java 키 저장소에서 키 내보내기 및 업로드"로 기존 `release.jks`를 앱 서명 키로 등록(PEPK 도구): 백업/복원 없이 덮어 설치 가능. 절차가 복잡해 백업/복원 기능이 생긴 뒤로는 필요성이 낮음.
   * 어느 쪽이든 `keystore/release.jks`(업로드 키) 백업은 필수.
2. `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab` (2026-09-27 빌드 확인, 약 8MB)
3. **테스트 및 출시 → 내부 테스트**: 새 버전 만들기 → AAB 업로드 → 출시 이름 `1.0.0 (2)` → 테스터(본인 이메일) 추가 → 출시.
4. **결제 프로필** 생성(수익 창출 설정) — 판매 대금 수령 계좌·세금 정보. 인앱상품 판매에 필요.
5. **수익 창출 → 제품 → 인앱 상품**: 상품 ID **정확히 `remove_ads_sbj`**, 이름 "광고 제거", 가격 설정 → 활성화.
6. **설정 → 라이선스 테스트**: 본인 Google 계정 추가(응답: RESPOND_NORMALLY).
7. 실결제 검증(Claude가 기기로 보조 가능): 광고 제거 결제창 표시 → 테스트 결제 → 배너 즉시 사라짐 → 앱 재시작 후 유지 → "구매 복원" → 주문 관리에서 테스트 주문 환불 → 앱 재시작 시 광고 복귀(9.4절 환불 반영).

### D. 비공개 테스트 12명 × 14일 (신규 개인 계정만)
1. **비공개 테스트 트랙** 생성 → 같은 AAB로 출시 → 테스터 이메일 목록 또는 Google 그룹 등록 → 참여(opt-in) 링크 공유.
2. 테스터 12명 이상이 **14일 연속** 참여 상태 유지. 2026년 기준 Google이 실제 사용 여부도 확인하므로 매일 조금씩 써 달라고 요청.
3. 이 기간 광고는 **테스트 ID 유지**(테스터의 실제 광고 클릭은 무효 트래픽 위험).
4. 피드백으로 버그 수정 시 `versionCode`를 올려 같은 트랙에 재배포.
5. 14일 후 대시보드에서 **프로덕션 액세스 신청**(테스트 방식·피드백·변경 사항 설문).

### E. 프로덕션 출시
1. 실제 AdMob ID 교체: `AndroidManifest.xml`의 `APPLICATION_ID` → `ca-app-pub-6630095840984426~4111120412`, `BannerAd.kt`의 광고 단위 → `ca-app-pub-6630095840984426/8566115600`(8절 Phase 1).
2. `versionCode` 3으로 올림 → `bundleRelease` → 프로덕션 트랙 출시(단계적 출시 권장) → 심사.
3. 출시 후 AdMob 콘솔에서 앱을 스토어 등록정보에 연결. (권장) `app-ads.txt`는 스토어 등록정보의 개발자 웹사이트 **도메인 루트**에 있어야 하므로, `zeonerd.github.io` 사용자 사이트 저장소가 필요.

---

## 12. 데이터 백업/복원 (2026-09-27)

재설치·기기 변경·Play 서명 전환 시 기록 유실에 대비한 기능. 사용자 확정: **파일 내보내기/가져오기, 복원은 전체 교체**. 서버·로그인·저장소 권한 없음.

### 12.1 구성
* **설정 → 데이터**: "백업 파일 만들기"(`ActivityResultContracts.CreateDocument`, 기본 파일명 `bullet-journal-backup-yyyy-MM-dd.json`), "백업에서 복원"(`OpenDocument`, 파일 관리자마다 .json MIME이 달라 `*/*`로 열고 내용으로 검증).
* **형식**(`data/JournalBackup.kt`): `{"app":"simple-bullet-journal","formatVersion":1,"exportedAt":...,"tasks":[...]}`. 할 일의 모든 필드(날짜·내용·완료·중요·이월·순서·작성 시각)를 담고 id는 제외. **Task 필드가 늘면 `FORMAT_VERSION`을 올리고 이전 버전 파일도 읽히게(새 필드 기본값) 유지할 것** — `decode`는 선택 필드에 기본값을 쓰도록 되어 있음.
* **복원 안전장치**: ① 파일 전체를 파싱·검증한 뒤에만 DB를 건드림(하나라도 잘못되면 전체 거부, 기존 기록 유지) ② 확인창에 "현재 N개 → 파일 M개(백업 시각)" 표시 ③ `TaskDao.replaceAllTasks`(`@Transaction`: 전체 삭제 + 일괄 삽입)로 중간 실패 시 원상복구 ④ 20MB 초과 파일 거부 ⑤ 복원 후 위젯 갱신.
* **백업하지 않는 것**: 테마(다시 고르면 됨), 광고 제거 구매 상태(앱 시작 시 Play에서 재확인·복원).
* Android 자동 백업(`allowBackup=true`, 규칙 없음 = 앱 데이터 전체)도 그대로 유지 — 보조 수단. 시점·동작이 기기 설정에 달려 있어 주 수단으로 삼지 않음.
* 개인정보처리방침 1·5절에 백업 파일(사용자가 고른 위치에만 저장, 비암호화 주의)과 Android 시스템 백업 안내 추가. 데이터 보안 설문 답변은 변경 없음(사용자 기기/계정 내 저장은 개발자 수집 아님).

### 12.2 테스트
* 단위테스트 `JournalBackupTest` 6개: 왕복 변환(id 제외 전 필드 보존, 특수문자), 빈 기록, 선택 필드 기본값, 다른 앱/미래 버전/깨진 파일/날짜 오류 거부, 한 항목만 잘못돼도 전체 거부, 전체 교체.
* `SettingsViewModel`의 백업 흐름은 `android.net.Uri`가 로컬 단위테스트에서 생성 불가라 단위테스트 대신 실기기로 검증(12.3 — 완료).

### 12.3 실기기 검증 (2026-09-27, SM-S711N, release 서명 빌드, **실사용 데이터 — 사용자 허락하에 복원 테스트**)
| # | 항목 | 결과 |
|---|---|---|
| 1 | 백업 파일 만들기 → 다운로드 폴더 저장 | ✅ "할 일 35개를 백업 파일로 저장했습니다." 파일(7.8KB) 검사: `app`/`formatVersion 1`/`exportedAt` 정상, 35개 전 필드 포함(9/7~9/27) |
| 2 | 더미 1개 추가(36개) → 백업에서 복원 | ✅ 확인창 "현재 기록 36개 → 백업 파일의 기록 35개 (2026년 9월 27일 21:27 백업)" → "35개를 복원했습니다", 더미 사라짐 |
| 3 | 복원 후 다시 백업해 원본과 비교 | ✅ 35개 **전 필드·순서 완전 일치**(`exportedAt`만 다름) |
| 4 | 다른 앱 형식 파일로 복원 시도 | ✅ 확인창 없이 "올바른 백업 파일이 아닙니다 (이 앱의 백업 파일이 아닙니다). 기존 기록은 그대로입니다." |
| — | 사용자 직접 확인(10.3절 #5) | ✅ 사용자가 오늘 페이지 "가져오기"로 지난 미완료 10개를 이월 — 오늘 목록에 원래 날짜 순서대로 표시 확인 |

* 테스트용 파일(잘못된 형식 파일, 비교용 두 번째 백업)은 기기에서 삭제했고, 사용자의 실제 백업 파일 `Download/bullet-journal-backup-2026-09-27.json`은 남겨 둠.
* 파일 선택 화면(DocumentsUI)은 같은 이름이 있으면 `… (1).json`으로 저장함 — 덮어쓰기 걱정 없음.

---

## 13. 앱 이름·패키지 ID 변경 (2026-09-27)

### 13.1 결정
| 항목 | 이전 | 변경 |
|---|---|---|
| 앱 이름(런처) | 불렛 저널 | **오늘노트** (영어 기기: **TodayNote**, `values-en/strings.xml`) |
| 스토어 이름 | (미정) | `오늘노트 - 할 일, 투두리스트, 체크리스트` / `TodayNote - To-do List & Daily Tasks` |
| 패키지 ID (`applicationId`) | `com.simple.bulletjournal` | **`com.zeonerd.todaynote`** — 첫 업로드 후 변경 불가. Play에 동일 ID 공개 앱 없음 확인(404) |
| 백업 파일 | `bullet-journal-backup-날짜.json`, `"app":"simple-bullet-journal"` | `todaynote-backup-날짜.json`, `"app":"todaynote"` — **옛 식별자도 계속 복원 허용**(테스트 `decode_acceptsLegacyBackupsMadeBeforeRename`) |
| 그래픽 이미지 | "Simple Bullet Journal" | "오늘노트 / TodayNote" (`store_assets/scripts/render_banner.py`로 재생성) |
| 개인정보처리방침·스토어 문구·README | Simple Bullet Journal / 불렛 저널 방식 | 오늘노트(TodayNote), 상표 단어 제거 |

### 13.2 배경
* **"Bullet Journal®"·"BuJo®"는 등록 상표**(Lightcage, LLC / Ryder Carroll). 상표권자는 제3자가 이 표장으로 제품·서비스를 판매하는 것을 금지하고 공식 앱을 따로 운영함. 광고·인앱결제로 수익을 내는 우리 앱에 쓰면 Play 상표 신고로 게시 중단될 위험 → 이름·설명·패키지 ID에서 모두 제거. **앞으로도 스토어 문구·마케팅에 "불렛 저널/Bullet Journal/BuJo"를 쓰지 말 것.**
* 이름은 벤치마크(개발자 "Notas Notepad"의 `[수식어]노트` 작명 + 이름 뒤 검색 키워드) 검토 후 사용자가 **오늘노트**로 결정. 영문은 "Daily Journal"(일기 앱이 포화, 할 일 앱과 결 다름, 식별력 약함) 대신 1:1 대응하는 **TodayNote**(Play 동일 이름 없음).
* "오늘노트" 검색 시 DailyNote류 메모 앱이 함께 노출되므로 스토어 이름 뒤 키워드("할 일, 투두리스트, 체크리스트")로 할 일 앱임을 분명히 함.
* 상표(KIPRIS) 조사: "오늘노트"·"TodayNote"·"오늘 노트" 국내 상표 0건까지 확인 후, 사용자 요청으로 추가 조사는 생략. 필요 시 출원 검토.

### 13.3 바꾸지 않은 것(내부 이름 — 사용자에게 안 보임)
* 소스 패키지·`namespace`(`com.simple.bulletjournal`), 클래스명(`BulletJournalApp`, `BulletJournalWidget`), 테마명(`Theme.SimpleBulletJournal`), DB 파일명(`bullet_journal.db`), `rootProject.name`, GitHub 저장소명(개인정보처리방침 URL `…/simple-bullet-journal/…` 포함). 저장소명을 바꾸면 방침 URL이 바뀌므로 Play 등록 전이라면 함께 결정할 것.

### 13.4 영향
* 폰의 옛 앱(`com.simple.bulletjournal`)과 새 앱(`com.zeonerd.todaynote`)은 **별개 앱으로 공존**. 기록 이전은 백업 → 복원(11절 C-1).
* AdMob: 실제 광고 ID로 교체·스토어 연결 시 새 패키지 기준으로 연결할 것.

### 13.5 실기기 검증 (2026-09-27, SM-S711N, release 서명 빌드)
| # | 항목 | 결과 |
|---|---|---|
| 1 | 새 패키지 설치 | ✅ `com.zeonerd.todaynote` 설치, 옛 `com.simple.bulletjournal`과 **별개 앱으로 공존**(옛 앱 데이터 그대로) |
| 2 | 앱 이름 | ✅ 시스템 앱 정보에 "오늘노트" 표시(APK 라벨: 기본 오늘노트, en/en-GB TodayNote) |
| 3 | 옛 형식 백업 복원 | ✅ 옛 앱에서 만든 `bullet-journal-backup-2026-09-27.json`(`"app":"simple-bullet-journal"`)을 새 앱에서 복원: "현재 0개 → 35개 (21:27 백업)" → "35개를 복원했습니다" |
| 4 | 새 형식 백업 | ✅ 기본 파일명 `todaynote-backup-2026-09-27.json`, `"app":"todaynote"`, 35개 **전 필드 옛 백업과 완전 일치** |

* 이로써 **옛 앱 → 새 앱 기록 이전 절차(11절 C-1)**가 실제로 동작함을 확인. 폰의 두 앱 중 옛 앱("불렛 저널")은 사용자가 확인 후 직접 삭제하면 됨. 다운로드 폴더의 백업 파일 2개는 사용자 데이터라 남겨 둠.

