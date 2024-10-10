package com.minseok.reminderscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val content = intent.getStringExtra("TODO_CONTENT") ?: return
        showNotification(context, content)
    }

    private fun showNotification(context: Context, content: String) {
        // 알림 생성 및 표시 로직
    }
}
