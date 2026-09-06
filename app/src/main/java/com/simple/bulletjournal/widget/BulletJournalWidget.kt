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
import androidx.glance.unit.ColorProvider
import com.simple.bulletjournal.MainActivity
import com.simple.bulletjournal.data.AppDatabase
import com.simple.bulletjournal.data.Task
import com.simple.bulletjournal.data.TaskRepository
import com.simple.bulletjournal.data.TaskRepositoryImpl
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Notebook style colors (matches app) ──
private val PaperColor = ColorProvider(Color(0xFFFFFEF0))
private val RuledLineColor = ColorProvider(Color(0xFFB8D4E3))
private val MarginLineColor = ColorProvider(Color(0xFFE0AAAA))
private val NoteTextColor = ColorProvider(Color(0xFF333333))
private val CompletedRedColor = ColorProvider(Color(0xFFE53935))
private val SubtleTextColor = ColorProvider(Color(0xFF888888))

class BulletJournalWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val repository: TaskRepository = TaskRepositoryImpl(AppDatabase.getInstance(context).taskDao())
        val tasks = repository.getTasksByDateOnce(today)
        val displayDate = LocalDate.now().format(
            DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
        )

        provideContent {
            WidgetContent(displayDate, tasks)
        }
    }

    @Composable
    private fun WidgetContent(displayDate: String, tasks: List<Task>) {
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
                    text = task.content,
                    style = TextStyle(
                        fontSize = 13.sp,
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
