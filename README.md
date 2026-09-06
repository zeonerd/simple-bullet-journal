# Simple Bullet Journal (간편 불렛 저널) 📓

> **아날로그 줄공책의 감성과 불렛 저널(Bullet Journal)의 핵심 철학을 담은 안드로이드 다이어리 앱**

---

## 📖 프로젝트 개요
**Simple Bullet Journal**은 복잡한 다이어리 앱 대신, 실제 **줄공책(Lined Notebook)**에 만년필로 기록하듯 간결하고 직관적으로 일정을 관리할 수 있도록 설계된 개인 생산성 앱입니다.

- **버전**: v1.0.0
- **최소 SDK**: Android 8.0 (API 26)
- **타겟 SDK**: Android 14 (API 34)
- **개발 언어**: Kotlin 2.0.21
- **UI 툴킷**: Jetpack Compose + Glance (홈 화면 위젯)

---

## ✨ 핵심 기능 (v1.0.0)

1. **빈티지 줄공책 & 다크 칠판 감성 UI**
   - 만년필 질감의 텍스트, 빨간색 여백 마진 라인, 하늘색 가로 공책 줄 배경
   - OS 시스템 다크 모드 연동: 어두운 환경에서는 눈이 편안한 **빈티지 다크 칠판(Chalkboard) 테마**로 자동 전환

2. **불렛 저널 핵심 이월(`>`) 시스템**
   - 어제 완료하지 못한 할 일을 자동 감지하여 상단 배너에 안내
   - 원클릭으로 오늘 날짜 할 일 목록에 그대로 이월(Migration) 처리

3. **할 일 우선순위(중요도) 지정**
   - 태스크 우측의 별표(★ / ☆) 버튼으로 중요도 지정
   - 중요 항목은 앰버 골드 컬러와 함께 목록 최상단으로 자동 정렬

4. **할 일 빠른 수정 & 삭제**
   - 텍스트 탭 시 즉시 인라인 다이얼로그로 내용 수정 가능
   - 우측 X 버튼으로 간편한 삭제 및 확인 다이얼로그 제공

5. **홈 화면 위젯 (Glance AppWidget)**
   - 앱을 켜지 않고도 홈 화면에서 오늘 할 일 확인 및 체크 토글
   - 위젯 상에서 어제 미완료 할 일 즉시 이월(`이월 ➔`) 기능 지원
   - 중요(★) 할 일 시각적 볼드 강조

---

## 🛠 기술 스택 & 아키텍처

- **UI**: Jetpack Compose, Material 3, Material Icons Extended
- **위젯**: Jetpack Glance (`androidx.glance.appwidget`)
- **데이터베이스**: Room 2.6.1 with **KSP** (Kotlin Symbol Processing)
  - 날짜 컬럼 인덱싱(`@Index(["date"])`)으로 빠른 조회 속도 보장
- **아키텍처**: MVVM + Clean Repository Pattern
  - `TaskDao` ➔ `TaskRepository` ➔ `TaskViewModel` ➔ Compose UI
- **의존성 주입(DI)**: Google Hilt 2.51.1
- **비동기 처리**: Kotlin Coroutines & Flow (`StateFlow`, `flatMapLatest`)
- **테스트 프레임워크**: JUnit 4, Kotlinx Coroutines Test, Turbine (`testDebugUnitTest`)

---

## 📂 프로젝트 디렉토리 구조

```
app/src/
├── main/java/com/simple/bulletjournal/
│   ├── MainActivity.kt               # 진입점 Activity (Hilt AndroidEntryPoint)
│   ├── BulletJournalApp.kt           # Application 클래스 (@HiltAndroidApp)
│   ├── data/                         # 데이터 계층
│   │   ├── Task.kt                   # Room Entity (id, date, content, isCompleted, isPriority, orderIndex)
│   │   ├── TaskDao.kt                # Room DAO (우선순위 및 생성일 기준 정렬 쿼리)
│   │   ├── AppDatabase.kt            # Room Database (v3)
│   │   ├── TaskRepository.kt         # Repository 인터페이스 추상화
│   │   └── TaskRepositoryImpl.kt     # Repository 구현체
│   ├── di/                           # Hilt 의존성 주입 모듈
│   │   ├── DatabaseModule.kt         # AppDatabase & TaskDao 싱글톤 주입
│   │   └── RepositoryModule.kt       # TaskRepository 바인딩
│   ├── ui/                           # UI 계층
│   │   ├── MainScreen.kt             # 공책 메인 화면 (날짜 헤더, 이월 배너, 줄노트, 입력 바)
│   │   └── theme/                    # 테마 및 디자인 시스템
│   │       ├── Color.kt              # 라이트(종이) / 다크(칠판) 컬러 정의
│   │       ├── Theme.kt              # NotebookColors CompositionLocal Provider
│   │       └── Type.kt               # 타이포그래피
│   ├── viewmodel/                    # 프레젠테이션 계층
│   │   └── TaskViewModel.kt          # 날짜 선택, 태스크 CRUD, 이월 및 위젯 갱신
│   └── widget/                       # Glance 홈 화면 위젯
│       ├── BulletJournalWidget.kt    # 위젯 UI, ToggleTaskAction, MigrateTasksAction
│       └── BulletJournalWidgetReceiver.kt
└── test/java/com/simple/bulletjournal/
    ├── FakeTaskRepository.kt         # 인메모리 Fake Repository (정렬 로직 일치화)
    ├── MainDispatcherRule.kt         # Coroutine TestRule (UnconfinedTestDispatcher)
    └── TaskViewModelTest.kt          # ViewModel 단위 테스트 스위트
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
