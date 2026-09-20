# Simple Bullet Journal (간편 불렛 저널) 📓

> **아날로그 줄공책의 감성과 불렛 저널(Bullet Journal)의 핵심 철학을 담은 안드로이드 다이어리 앱**

---

## 📖 프로젝트 개요
**Simple Bullet Journal**은 복잡한 다이어리 앱 대신, 실제 **줄공책(Lined Notebook)**에 만년필로 기록하듯 간결하고 직관적으로 일정을 관리할 수 있도록 설계된 개인 생산성 앱입니다.

- **버전**: v1.1.0-dev (versionCode 2)
- **최소 SDK**: Android 8.0 (API 26)
- **타겟 SDK**: Android 14 (API 34)
- **개발 언어**: Kotlin 2.2.10
- **UI 툴킷**: Jetpack Compose + Glance (홈 화면 위젯)

---

## ✨ 핵심 기능 (v1.0.0)

1. **빈티지 줄공책 & 다크 칠판 감성 UI**
   - 만년필 질감의 텍스트, 빨간색 여백 마진 라인, 하늘색 가로 공책 줄 배경
   - OS 시스템 다크 모드 연동: 어두운 환경에서는 눈이 편안한 **빈티지 다크 칠판(Chalkboard) 테마**로 자동 전환
   - 칠판 감성을 살린 세련된 Adaptive Icon(분필로 그린 불렛과 언더바) 적용

2. **불렛 저널 핵심 이월(`>`) 시스템 (가져오기)**
   - 어제 완료하지 못한 할 일을 자동 감지하여 상단 배너에 안내
   - 원클릭으로 오늘 날짜 할 일 목록에 그대로 가져오기(Migration) 처리
   - 한 번 가져오면 중복 이월을 방지하고 배너가 즉시 사라지는 스마트 UI 적용

3. **할 일 우선순위(중요도) 지정**
   - 태스크 우측의 별표(★ / ☆) 버튼으로 중요도 지정
   - 중요 항목은 앰버 골드 컬러와 함께 목록 최상단으로 자동 정렬

4. **할 일 빠른 수정 & 삭제**
   - 텍스트 탭 시 즉시 인라인 다이얼로그로 내용 수정 가능
   - 우측 X 버튼으로 간편한 삭제 및 확인 다이얼로그 제공
   - 키보드가 올라올 때 화면이 밀려나지 않고 부드럽게 크기가 조정되는 안정적인 입력 환경 제공

5. **날짜 탐색**
   - 상단 헤더의 ◀ / ▶ 버튼으로 전날·다음 날 이동
   - 날짜 텍스트 탭 시 `DatePickerDialog`로 특정 날짜 바로 이동
   - 오늘이 아닌 날짜를 보고 있을 때 "오늘로 돌아가기" 버튼 노출

6. **설정 화면**
   - 상단 톱니바퀴 아이콘으로 진입
   - 테마 선택(시스템 기본 / 라이트 / 다크) — 즉시 앱 전체에 반영
   - 광고 제거 구매 버튼 + 구매 복원 버튼 (Google Play Billing 연동, Play Console 상품 등록 후 실결제 가능)

7. **배너 광고 (AdMob)**
   - 메인 화면 하단에 배너 광고 노출 (GDPR/EEA 동의 플로우 포함)
   - 설정에서 "광고 제거"를 누르면 즉시 배너가 사라지고, 앱 재시작 후에도 유지됨
   - 현재 테스트 광고 ID로 동작 중 — 스토어 출시 전 실제 AdMob ID로 교체 예정

8. **홈 화면 위젯 (Glance AppWidget)**
   - 앱을 켜지 않고도 홈 화면에서 오늘 할 일 확인 및 체크 토글
   - 위젯 상에서 어제 미완료 할 일 즉시 가져오기(`가져오기 ➔`) 기능 지원
   - 중요(★) 할 일 시각적 볼드 강조
   - 시스템 다크/라이트 모드 설정에 즉각적으로 연동되는 다이나믹 컬러 테마 적용

---

## 🛠 기술 스택 & 아키텍처

- **UI**: Jetpack Compose, Material 3, Material Icons Extended
- **위젯**: Jetpack Glance (`androidx.glance.appwidget`)
- **데이터베이스**: Room 2.7.0 with **KSP** (Kotlin Symbol Processing)
  - 날짜 컬럼 인덱싱(`@Index(["date"])`)으로 빠른 조회 속도 보장
- **아키텍처**: MVVM + Clean Repository Pattern
  - `TaskDao` ➔ `TaskRepository` ➔ `TaskViewModel` ➔ Compose UI
  - `MainScreen`/`SettingsScreen` 모두 `hiltViewModel()`로 각각 `TaskViewModel`/`SettingsViewModel`을 생성합니다(`androidx.hilt:hilt-navigation-compose`). 화면 전환은 `NavHost` 없이 `MainActivity`의 로컬 상태로 처리합니다. Glance 위젯(`BulletJournalWidget`, `ToggleTaskAction`, `MigrateTasksAction`)은 여전히 Hilt를 거치지 않고 `TaskRepositoryImpl(AppDatabase.getInstance(context).taskDao())`을 직접 생성합니다 — 상세 내용은 [HANDOVER.md](HANDOVER.md) 참고.
- **의존성 주입(DI)**: Google Hilt 2.60.1
- **광고**: Google Mobile Ads SDK(`play-services-ads` 23.6.0) + User Messaging Platform(`user-messaging-platform` 3.1.0, GDPR/EEA 동의)
- **설정 저장**: `androidx.datastore:datastore-preferences`
- **인앱결제**: Google Play Billing Library(`billing-ktx` 7.1.1) — 비소모성 상품 `remove_ads_sbj`
- **비동기 처리**: Kotlin Coroutines & Flow (`StateFlow`, `flatMapLatest`)
- **테스트 프레임워크**: JUnit 4, Kotlinx Coroutines Test, Turbine (`testDebugUnitTest`)

---

## 📂 프로젝트 디렉토리 구조

```
app/src/
├── main/java/com/simple/bulletjournal/
│   ├── MainActivity.kt               # 진입점 Activity (Hilt AndroidEntryPoint), 화면 전환·테마·광고 초기화 오케스트레이션
│   ├── BulletJournalApp.kt           # Application 클래스 (@HiltAndroidApp), UMP 동의 + MobileAds 초기화
│   ├── data/                         # 데이터 계층
│   │   ├── Task.kt                   # Room Entity (id, date, content, isCompleted, isPriority, isMigrated, orderIndex)
│   │   ├── TaskDao.kt                # Room DAO (우선순위 및 생성일 기준 정렬 쿼리)
│   │   ├── AppDatabase.kt            # Room Database (v4, Migration(3,4)로 isMigrated 컬럼 추가)
│   │   ├── TaskRepository.kt         # Repository 인터페이스 추상화
│   │   ├── TaskRepositoryImpl.kt     # Repository 구현체
│   │   ├── UserPreferences.kt        # 설정값 모델 (isAdRemoved, themeMode)
│   │   ├── UserPreferencesRepository.kt      # 설정값 Repository 인터페이스
│   │   ├── UserPreferencesRepositoryImpl.kt  # DataStore Preferences 기반 구현체
│   │   ├── BillingRepository.kt      # 인앱결제 Repository 인터페이스
│   │   └── BillingRepositoryImpl.kt  # Play BillingClient 래핑, 상품 ID: remove_ads_sbj
│   ├── di/                           # Hilt 의존성 주입 모듈
│   │   ├── DatabaseModule.kt         # AppDatabase, TaskDao, TaskRepository 싱글톤 주입(@Provides)을 한 파일에서 처리
│   │   ├── DataStoreModule.kt        # DataStore<Preferences>, UserPreferencesRepository 싱글톤 주입
│   │   └── BillingModule.kt          # BillingRepository 싱글톤 주입
│   ├── ui/                           # UI 계층
│   │   ├── MainScreen.kt             # 공책 메인 화면 (설정 진입 버튼, 날짜 헤더, 이월 배너, 줄노트, 입력 바, 배너 광고)
│   │   ├── SettingsScreen.kt         # 설정 화면 (테마 선택, 광고 제거 구매/복원)
│   │   ├── ads/
│   │   │   └── BannerAd.kt           # AdMob 배너를 AndroidView로 래핑한 컴포저블
│   │   └── theme/                    # 테마 및 디자인 시스템
│   │       ├── Color.kt              # 라이트(종이) / 다크(칠판) 컬러 정의
│   │       ├── Theme.kt              # NotebookColors CompositionLocal Provider
│   │       └── Type.kt               # 타이포그래피
│   ├── viewmodel/                    # 프레젠테이션 계층
│   │   ├── TaskViewModel.kt          # 날짜 선택, 태스크 CRUD, 이월 및 위젯 갱신
│   │   └── SettingsViewModel.kt      # 테마 변경, 광고 제거 구매/복원 위임
│   └── widget/                       # Glance 홈 화면 위젯
│       ├── BulletJournalWidget.kt    # 위젯 UI, ToggleTaskAction, MigrateTasksAction
│       └── WidgetReceiver.kt         # 위젯 리시버
└── test/java/com/simple/bulletjournal/
    ├── FakeTaskRepository.kt              # 인메모리 Fake Repository (정렬 로직 일치화)
    ├── FakeUserPreferencesRepository.kt   # 인메모리 Fake 설정값 Repository
    ├── FakeBillingRepository.kt           # 인앱결제 호출 위임 검증용 Fake
    ├── MainDispatcherRule.kt              # Coroutine TestRule (UnconfinedTestDispatcher)
    ├── TaskViewModelTest.kt               # TaskViewModel 단위 테스트 스위트
    ├── SettingsViewModelTest.kt           # SettingsViewModel 단위 테스트 스위트
    └── UserPreferencesRepositoryTest.kt   # DataStore 기반 설정값 저장/조회 테스트
```

---

## 🚀 빌드 및 실행 가이드

### 1. 개발 환경 요구사항
- **Android Studio**: Koala / Ladybug (2024.1+) 권장
- **JDK**: Java 17 이상 (Android Studio 내장 JBR 권장)
- **Gradle**: 9.5.0 (Gradle Wrapper 포함)

### 2. 빌드 명령어
```bash
# 단위 테스트 실행
./gradlew testDebugUnitTest

# 디버그 APK 빌드
./gradlew assembleDebug

# 릴리즈 서명 APK 빌드 (keystore.properties 필요)
./gradlew assembleRelease
```

### 3. 릴리즈 서명 설정
릴리즈 빌드를 위해서는 프로젝트 루트에 `keystore.properties` 파일이 필요합니다.
```properties
storeFile=../keystore/release.jks
storePassword=<비밀번호>
keyAlias=bulletjournal
keyPassword=<비밀번호>
```
*(보안을 위해 `*.jks` 및 `keystore.properties`는 `.gitignore`로 보호됩니다.)*
