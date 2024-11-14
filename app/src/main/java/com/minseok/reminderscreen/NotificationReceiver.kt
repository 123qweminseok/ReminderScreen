package com.minseok.reminderscreen

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ReminderScreen:NotificationWakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L) // 10분

        try {
            val content = intent.getStringExtra("TODO_CONTENT") ?: return
            val time = intent.getLongExtra("TODO_TIME", 0)

            // 전체화면 알림 인텐트
            val fullScreenIntent = Intent(context, TodoAlertDialog::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("TODO_CONTENT", content)
                putExtra("TODO_TIME", time)
            }

            // 알림 다이얼로그 실행
            context.startActivity(fullScreenIntent)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            wakeLock.release()
        }
    }
}