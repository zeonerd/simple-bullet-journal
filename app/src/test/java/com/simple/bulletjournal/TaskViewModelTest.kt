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
            repository = fakeRepository,
            widgetUpdater = {}
        )
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
}
