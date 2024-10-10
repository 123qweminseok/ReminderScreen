package com.minseok.reminderscreen

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class NotificationHelper(private val context: Context) {
    fun scheduleNotification(todoItem: TodoItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("TODO_CONTENT", todoItem.content)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, todoItem.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)

        todoItem.time?.let { time ->
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time.time, pendingIntent)
        }
    }
}
