# 📋 개발 인수인계 문서 (Handover Document - v1.0.0+)

본 문서는 **Simple Bullet Journal** 프로젝트의 아키텍처 설계 배경, 주요 구현 상세, 그리고 **v1.0.0 이후(v1.1+) 개발자가 즉시 작업을 이어갈 수 있도록 필요한 기술적 맥락과 로드맵**을 상세히 기술합니다.

---

## 1. 아키텍처 개요 및 설계 원칙

### 1.1 계층 분리 (Clean MVVM)
* **Data 계층**: `Room` + `TaskRepository` 추상화
  * DAO를 직접 ViewModel에서 참조하지 않고 반드시 `TaskRepository` 인터페이스를 거칩니다.
  * 모든 쿼리는 날짜(`date`, `yyyy-MM-dd`) 기준으로 필터링되며, `isPriority DESC, orderIndex ASC, createdAt ASC` 순으로 정렬됩니다.
* **DI 계층**: `Google Hilt`
  * `DatabaseModule`: `AppDatabase`, `TaskDao` 제공
  * `RepositoryModule`: `TaskRepositoryImpl`을 `TaskRepository`로 바인딩 (`@Binds`)
* **Presentation 계층**: `Jetpack Compose` + `TaskViewModel`
  * ViewModel은 `AndroidViewModel(application)`을 상속하며, 위젯 갱신을 위해 `GlanceAppWidgetManager`를 호출합니다.
  * 단위 테스트 환경에서 Android Framework 의존성 충돌을 방지하기 위해 `widgetUpdater: suspend () -> Unit` 람다 프로퍼티를 주입 가능하게 설계하였습니다.
* **Widget 계층**: `Jetpack Glance`
  * 앱이 종료된 상태에서도 원격 뷰를 통해 데이터 조회/수정/이월이 가능하도록 `ToggleTaskAction`, `MigrateTasksAction`을 `ActionCallback`으로 구현했습니다.

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
      val orderIndex: Int = 0,          // 수동 순서 조정용 인덱스
      val createdAt: Long = System.currentTimeMillis()
  )
  ```
* **현재 DB 버전**: `version = 3`
  * v1: 기본 Task 엔티티 (인덱스 없음)
  * v2: `date` 컬럼 인덱싱 추가 (`@Index(["date"])`)
  * v3: `isPriority`, `orderIndex` 필드 추가
  * `fallbackToDestructiveMigration()` 설정이 적용되어 있으나, 정식 서비스 시에는 마이그레이션 전략(`Migration(3, 4)`)을 작성하는 것을 권장합니다.

---

## 3. 홈 화면 위젯(Glance) 연동 주의사항

* 위젯은 Compose UI와 문법이 유사하나, **Jetpack Glance 전용 컴포넌트**(`androidx.glance.*`)만 사용해야 합니다.
* 앱 내부에서 태스크 변경 시 ViewModel에서 `updateWidget()`을 통해 모든 활성 위젯을 자동 갱신합니다.
* 위젯에서 할 일 체크 또는 이월 클릭 시에는 `ActionCallback`에서 DB를 직접 조작한 뒤 `BulletJournalWidget().update(context, glanceId)`를 호출합니다.

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

## 6. 빌드 & 배포 주의사항

1. **JDK 환경**:
   * macOS 기준 Android Studio 내장 JBR 경로: `/Applications/Android Studio.app/Contents/jbr/Contents/Home`
   * 터미널 실행 시:
     ```bash
     JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew <tasks>
     ```
2. **릴리즈 서명 키 관리**:
   * 서명 키 파일: `keystore/release.jks` (alias: `bulletjournal`)
   * 설정 파일: `keystore.properties` (템플릿: `keystore.properties.example`)
   * **경고**: 이 키스토어 파일이 유실되면 기존 사용자가 앱 데이터를 유지한 채 업데이트할 수 없습니다. 안전한 곳에 백업을 유지하세요.
3. **버전 번호 관리**:
   * 새 버전 릴리즈 시 `app/build.gradle.kts`의 `versionCode`를 1씩 증가시키고 `versionName`을 갱신합니다.
