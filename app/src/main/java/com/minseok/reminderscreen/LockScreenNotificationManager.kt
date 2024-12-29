package com.minseok.reminderscreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class LockScreenNotificationManager(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "TodoLockScreenChannel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Todo Lock Screen Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTodoNotification(todoItems: List<TodoItem>) {
        // 현재 날짜의 아이템만 필터링
        val today = Calendar.getInstance().time
        val todayItems = todoItems.filter { item ->
            val itemCal = Calendar.getInstance().apply { time = item.date }
            val todayCal = Calendar.getInstance().apply { time = today }

            itemCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    itemCal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH) &&
                    itemCal.get(Calendar.DAY_OF_MONTH) == todayCal.get(Calendar.DAY_OF_MONTH) &&
                    !item.isCompleted  // 완료되지 않은 항목만 포함
        }

        // 오늘 날짜의 아이템이 없으면 알림을 보여주지 않음
        if (todayItems.isEmpty()) return

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("오늘의 할 일")
            .setContentText("${todayItems.size}개의 할 작업이 있습니다.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(todayItems.joinToString("\n") { it.content }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(1, notification)
    }
}