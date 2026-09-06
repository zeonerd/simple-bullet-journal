package com.simple.bulletjournal

import android.app.Application
import app.cash.turbine.test
import com.simple.bulletjournal.data.Task
import com.simple.bulletjournal.viewmodel.TaskViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var viewModel: TaskViewModel
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository()
        viewModel = TaskViewModel(
            application = Application(),
            repository = fakeRepository
        ).apply {
            widgetUpdater = {}
        }
    }

    @Test
    fun initialState_selectedDateIsToday() {
        assertEquals(LocalDate.now(), viewModel.selectedDate.value)
    }

    @Test
    fun dateNavigation_previousNextToday() {
        val today = LocalDate.now()

        viewModel.goToPreviousDay()
        assertEquals(today.minusDays(1), viewModel.selectedDate.value)

        viewModel.goToNextDay()
        assertEquals(today, viewModel.selectedDate.value)

        viewModel.goToNextDay()
        assertEquals(today.plusDays(1), viewModel.selectedDate.value)

        viewModel.goToToday()
        assertEquals(today, viewModel.selectedDate.value)
    }

    @Test
    fun selectDate_updatesSelectedDate() {
        val targetDate = LocalDate.of(2026, 12, 25)
        viewModel.selectDate(targetDate)
        assertEquals(targetDate, viewModel.selectedDate.value)
    }

    @Test
    fun addTask_insertsTaskAndUpdatesFlow() = runTest {
        viewModel.tasks.test {
            // Initial empty list
            assertEquals(emptyList<Task>(), awaitItem())

            viewModel.addTask("테스트 할 일 1")

            val updatedList = awaitItem()
            assertEquals(1, updatedList.size)
            assertEquals("테스트 할 일 1", updatedList[0].content)
            assertFalse(updatedList[0].isCompleted)
            assertEquals(LocalDate.now().format(formatter), updatedList[0].date)
        }
    }

    @Test
    fun addTask_blankContent_doesNotInsert() = runTest {
        viewModel.tasks.test {
            assertEquals(emptyList<Task>(), awaitItem())

            viewModel.addTask("")
            viewModel.addTask("   ")

            expectNoEvents()
        }
    }

    @Test
    fun toggleTask_togglesCompletionStatus() = runTest {
        viewModel.tasks.test {
            assertEquals(emptyList<Task>(), awaitItem())

            viewModel.addTask("할 일 완료 테스트")
            val listAfterAdd = awaitItem()
            val task = listAfterAdd[0]
            assertFalse(task.isCompleted)

            viewModel.toggleTask(task)
            val listAfterToggle = awaitItem()
            assertTrue(listAfterToggle[0].isCompleted)

            viewModel.toggleTask(listAfterToggle[0])
            val listAfterSecondToggle = awaitItem()
            assertFalse(listAfterSecondToggle[0].isCompleted)
        }
    }

    @Test
    fun deleteTask_removesTaskFromFlow() = runTest {
        viewModel.tasks.test {
            assertEquals(emptyList<Task>(), awaitItem())

            viewModel.addTask("삭제할 할 일")
            val listWithTask = awaitItem()
            assertEquals(1, listWithTask.size)

            viewModel.deleteTask(listWithTask[0])
            val listAfterDelete = awaitItem()
            assertEquals(0, listAfterDelete.size)
        }
    }

    @Test
    fun dateChange_loadsTasksForSelectedDateOnly() = runTest {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        // Add task for today
        fakeRepository.insertTask(
            Task(id = 1, date = today.format(formatter), content = "오늘 할 일")
        )
        // Add task for tomorrow
        fakeRepository.insertTask(
            Task(id = 2, date = tomorrow.format(formatter), content = "내일 할 일")
        )

        viewModel.tasks.test {
            val todayTasks = awaitItem()
            assertEquals(1, todayTasks.size)
            assertEquals("오늘 할 일", todayTasks[0].content)

            // Switch date to tomorrow
            viewModel.goToNextDay()

            val tomorrowTasks = awaitItem()
            assertEquals(1, tomorrowTasks.size)
            assertEquals("내일 할 일", tomorrowTasks[0].content)
        }
    }

    @Test
    fun editTask_updatesTaskContent() = runTest {
        viewModel.tasks.test {
            assertEquals(emptyList<Task>(), awaitItem())

            viewModel.addTask("원래 내용")
            val list = awaitItem()
            val task = list[0]

            viewModel.editTask(task, "수정된 내용")
            val updatedList = awaitItem()
            assertEquals("수정된 내용", updatedList[0].content)
        }
    }

    @Test
    fun editTask_blankContent_doesNotUpdate() = runTest {
        viewModel.tasks.test {
            assertEquals(emptyList<Task>(), awaitItem())

            viewModel.addTask("수정 불가 테스트")
            val list = awaitItem()
            val task = list[0]

            viewModel.editTask(task, "")
            viewModel.editTask(task, "   ")
            expectNoEvents()
        }
    }

    @Test
    fun migrateYesterdayTasks_copiesOnlyUncompletedTasksToCurrentDate() = runTest {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // 어제 할 일 2개 추가: 1개는 미완료(중요), 1개는 완료
        fakeRepository.insertTask(
            Task(id = 1, date = yesterday.format(formatter), content = "어제 미완료 할 일", isCompleted = false, isPriority = true)
        )
        fakeRepository.insertTask(
            Task(id = 2, date = yesterday.format(formatter), content = "어제 완료된 일", isCompleted = true)
        )

        viewModel.tasks.test {
            // 오늘 날짜의 초기 할 일 목록은 비어있음
            assertEquals(emptyList<Task>(), awaitItem())

            // 어제 미완료 할 일 이월 실행
            viewModel.migrateYesterdayTasks()

            // 오늘 날짜로 미완료 할 일만 이월되어 추가되었는지 검증
            val todayTasks = awaitItem()
            assertEquals(1, todayTasks.size)
            assertEquals("어제 미완료 할 일", todayTasks[0].content)
            assertEquals(today.format(formatter), todayTasks[0].date)
            assertFalse(todayTasks[0].isCompleted)
            assertTrue(todayTasks[0].isPriority)
        }
    }

    @Test
    fun togglePriority_togglesPriorityStatus() = runTest {
        viewModel.tasks.test {
            assertEquals(emptyList<Task>(), awaitItem())

            viewModel.addTask("우선순위 테스트")
            val listAfterAdd = awaitItem()
            val task = listAfterAdd[0]
            assertFalse(task.isPriority)

            viewModel.togglePriority(task)
            val listAfterToggle = awaitItem()
            assertTrue(listAfterToggle[0].isPriority)

            viewModel.togglePriority(listAfterToggle[0])
            val listAfterSecondToggle = awaitItem()
            assertFalse(listAfterSecondToggle[0].isPriority)
        }
    }

    @Test
    fun priorityOrdering_priorityTasksAppearFirst() = runTest {
        viewModel.tasks.test {
            assertEquals(emptyList<Task>(), awaitItem())

            viewModel.addTask("일반 작업 1")
            awaitItem()

            viewModel.addTask("일반 작업 2")
            val listTwo = awaitItem()

            // 두 번째 작업을 중요(priority)로 변경
            val secondTask = listTwo[1]
            viewModel.togglePriority(secondTask)

            val orderedList = awaitItem()
            assertEquals(2, orderedList.size)
            // 중요 작업이 목록의 맨 앞으로 정렬되어야 함
            assertEquals("일반 작업 2", orderedList[0].content)
            assertTrue(orderedList[0].isPriority)
            assertEquals("일반 작업 1", orderedList[1].content)
            assertFalse(orderedList[1].isPriority)
        }
    }
}
