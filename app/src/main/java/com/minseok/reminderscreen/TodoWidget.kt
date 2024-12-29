package com.minseok.reminderscreen

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import android.view.View
import android.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TodoWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, TodoWidget::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    this.onUpdate(context, appWidgetManager, appWidgetIds)
                }
            }
            "com.minseok.reminderscreen.TOGGLE_WIDGET" -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val isExpanded = !prefs.getBoolean(KEY_EXPANDED + appWidgetId, false)
                    prefs.edit().putBoolean(KEY_EXPANDED + appWidgetId, isExpanded).apply()

                    // 위젯 업데이트
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            "com.minseok.reminderscreen.COMPLETE_TODO" -> {
                val todoId = intent.getLongExtra("todo_id", -1L)
                if (todoId != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val todoRepository = TodoRepository(context)
                        val todoItem = todoRepository.getTodoItemById(todoId)
                        todoItem?.let { original ->
                            val updated = original.copy(isCompleted = true)
                            todoRepository.updateTodoItem(updated)

                            val appWidgetManager = AppWidgetManager.getInstance(context)
                            val componentName = ComponentName(context, TodoWidget::class.java)
                            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                            onUpdate(context, appWidgetManager, appWidgetIds)
                        }
                    }
                }
            }
        }
    }
    companion object {
        private const val PREFS_NAME = "TodoWidgetPrefs"
        private const val KEY_EXPANDED = "widget_expanded_"

        private fun toggleWidgetExpansion(context: Context, widgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isExpanded = !prefs.getBoolean(KEY_EXPANDED + widgetId, false)
            prefs.edit().putBoolean(KEY_EXPANDED + widgetId, isExpanded).apply()
        }

        private fun isWidgetExpanded(context: Context, widgetId: Int): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_EXPANDED + widgetId, false)
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val isExpanded = isWidgetExpanded(context, appWidgetId)
            val views = RemoteViews(context.packageName,
                if (isExpanded) R.layout.todo_widget_expanded
                else R.layout.todo_widget)

            // 오늘 날짜 설정
            val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
            views.setTextViewText(R.id.widget_date, dateFormat.format(Date()))

            // 토글 동작 설정
// updateAppWidget 함수 내의 토글 설정 부분을 수정
// 토글 동작 설정
            val toggleIntent = Intent(context, TodoWidget::class.java).apply {
                action = "com.minseok.reminderscreen.TOGGLE_WIDGET"  // 이 부분을 수정
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_layout, togglePendingIntent)
            // 앱 실행 버튼 설정 (확장 모드에서만)
            if (isExpanded) {
                val appIntent = Intent(context, MainActivity::class.java)
                val appPendingIntent = PendingIntent.getActivity(
                    context, 0, appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_open_app, appPendingIntent)
            }

            // 오늘의 할 일 가져오기
            CoroutineScope(Dispatchers.IO).launch {
                val todoRepository = TodoRepository(context)
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val allTodos = todoRepository.getTodoItemsByDate(today)
                val (incompleteTodos, completedTodos) = allTodos.partition { !it.isCompleted }
                    .let { Pair(it.first.sortedBy { todo -> todo.time }, it.second.sortedBy { todo -> todo.time }) }

                val todosToShow = if (isExpanded) {
                    incompleteTodos + completedTodos
                } else {
                    incompleteTodos.take(3)
                }

                // UI 업데이트
                views.removeAllViews(R.id.widget_todo_container)

                if (todosToShow.isEmpty()) {
                    views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_todo_container, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_empty_view, View.GONE)
                    views.setViewVisibility(R.id.widget_todo_container, View.VISIBLE)

                    if (isExpanded) {
                        // 미완료 할 일 섹션
                        if (incompleteTodos.isNotEmpty()) {
                            addSectionHeader(views, "처리할 일", context)
                            incompleteTodos.forEach { todo ->
                                addTodoItem(views, todo, context, true)
                            }
                        }

                        // 완료된 할 일 섹션
                        if (completedTodos.isNotEmpty()) {
                            addSectionHeader(views, "완료된 일", context)
                            completedTodos.forEach { todo ->
                                addTodoItem(views, todo, context, false)
                            }
                        }
                    } else {
                        todosToShow.forEach { todo ->
                            addTodoItem(views, todo, context, false)
                        }
                    }
                }

                // 위젯 업데이트
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun addSectionHeader(views: RemoteViews, title: String, context: Context) {
            val headerView = RemoteViews(context.packageName, R.layout.widget_section_header)
            headerView.setTextViewText(R.id.section_title, title)
            views.addView(R.id.widget_todo_container, headerView)
        }

        private fun addTodoItem(views: RemoteViews, todo: TodoItem, context: Context, showCompleteButton: Boolean) {
            val todoView = RemoteViews(context.packageName, R.layout.widget_todo_item)

            val todoText = todo.time?.let { time ->
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                "${timeFormat.format(time)} ${todo.content}"
            } ?: todo.content

            todoView.setTextViewText(R.id.widget_todo_text, todoText)

            if (todo.isCompleted) {
                todoView.setTextColor(R.id.widget_todo_text, Color.GRAY)
                todoView.setInt(R.id.widget_todo_text, "setPaintFlags",
                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG)
                todoView.setViewVisibility(R.id.btn_complete, View.GONE)
            } else {
                todoView.setTextColor(R.id.widget_todo_text, Color.BLACK)
                todoView.setViewVisibility(R.id.btn_complete, if (showCompleteButton) View.VISIBLE else View.GONE)

                if (showCompleteButton) {
                    val completeIntent = Intent(context, TodoWidget::class.java).apply {
                        action = "com.minseok.reminderscreen.COMPLETE_TODO"
                        putExtra("todo_id", todo.id)
                    }
                    val completePendingIntent = PendingIntent.getBroadcast(
                        context,
                        todo.id.hashCode(),
                        completeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    todoView.setOnClickPendingIntent(R.id.btn_complete, completePendingIntent)
                }
            }

            views.addView(R.id.widget_todo_container, todoView)
        }
    }
}