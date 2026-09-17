package com.simple.bulletjournal.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.simple.bulletjournal.MainActivity
import com.simple.bulletjournal.data.AppDatabase
import com.simple.bulletjournal.data.Task
import com.simple.bulletjournal.data.TaskRepository
import com.simple.bulletjournal.data.TaskRepositoryImpl
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import com.simple.bulletjournal.ui.theme.*

// ── Notebook style colors (matches app) ──
private val PaperColor = ColorProvider(day = LightPaper, night = DarkPaper)
private val RuledLineColor = ColorProvider(day = LightRuledLine, night = DarkRuledLine)
private val MarginLineColor = ColorProvider(day = LightMarginLine, night = DarkMarginLine)
private val NoteTextColor = ColorProvider(day = LightNoteText, night = DarkNoteText)
private val CompletedRedColor = ColorProvider(day = LightCompleted, night = DarkCompleted)
private val SubtleTextColor = ColorProvider(day = LightNoteSubtleText, night = DarkNoteSubtleText)
private val BannerBgColor = ColorProvider(day = LightBannerBackground, night = DarkBannerBackground)
private val BannerTextColor = ColorProvider(day = LightBannerText, night = DarkBannerText)
private val ActionTextColor = ColorProvider(day = LightCompleted, night = DarkCompleted)
private val PriorityColor = ColorProvider(day = LightPriority, night = DarkPriority)


class BulletJournalWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val repository: TaskRepository = TaskRepositoryImpl(AppDatabase.getInstance(context).taskDao())
        val tasks = repository.getTasksByDateOnce(today)
        val uncompletedYesterdayCount = repository.getTasksByDateOnce(yesterday).count { !it.isCompleted && !it.isMigrated }
        val displayDate = LocalDate.now().format(
            DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
        )

        provideContent {
            WidgetContent(displayDate, tasks, uncompletedYesterdayCount)
        }
    }

    @Composable
    private fun WidgetContent(displayDate: String, tasks: List<Task>, uncompletedYesterdayCount: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(PaperColor)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // ── Header (date) ──
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "\uD83D\uDCCB $displayDate",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NoteTextColor
                    )
                )
            }

            // ── Header bottom line ──
            Spacer(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RuledLineColor)
            )

            // ── Yesterday Migration Banner (if any) ──
            if (uncompletedYesterdayCount > 0) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .background(BannerBgColor)
                        .cornerRadius(6.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "❭ 어제 미완료 ${uncompletedYesterdayCount}개",
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = BannerTextColor
                        )
                    )
                    Text(
                        text = "가져오기 ➔",
                        modifier = GlanceModifier.clickable(actionRunCallback<MigrateTasksAction>()),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ActionTextColor
                        )
                    )
                }

                Spacer(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(RuledLineColor)
                )
            }

            // ── Notebook body ──
            if (tasks.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "할 일이 없습니다 ✨",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = SubtleTextColor
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                ) {
                    items(tasks, itemId = { it.id }) { task ->
                        WidgetNotebookLine(task)
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetNotebookLine(task: Task) {
        val taskText = if (task.isPriority) "★ ${task.content}" else task.content

        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                // ── Red margin line ──
                Box(
                    modifier = GlanceModifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(MarginLineColor)
                ) {}

                Spacer(modifier = GlanceModifier.width(6.dp))

                // ── Checkbox + task text ──
                CheckBox(
                    checked = task.isCompleted,
                    onCheckedChange = actionRunCallback<ToggleTaskAction>(
                        actionParametersOf(ToggleTaskAction.TASK_ID_KEY to task.id)
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                    text = taskText,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = if (task.isPriority) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (task.isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                        color = if (task.isCompleted) {
                            CompletedRedColor
                        } else {
                            NoteTextColor
                        }
                    )
                )
            }

            // ── Blue ruled line (bottom border) ──
            Spacer(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RuledLineColor)
            )
        }
    }
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TASK_ID_KEY] ?: return
        val repository: TaskRepository = TaskRepositoryImpl(AppDatabase.getInstance(context).taskDao())
        val task = repository.getTaskById(taskId) ?: return
        repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        BulletJournalWidget().update(context, glanceId)
    }

    companion object {
        val TASK_ID_KEY = ActionParameters.Key<Long>("task_id")
    }
}

class MigrateTasksAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val repository: TaskRepository = TaskRepositoryImpl(AppDatabase.getInstance(context).taskDao())
        val uncompleted = repository.getTasksByDateOnce(yesterday).filter { !it.isCompleted && !it.isMigrated }
        uncompleted.forEach { task ->
            repository.insertTask(
                Task(
                    date = today,
                    content = task.content,
                    isPriority = task.isPriority
                )
            )
            repository.updateTask(task.copy(isMigrated = true))
        }
        BulletJournalWidget().update(context, glanceId)
    }
}
