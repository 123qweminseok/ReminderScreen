package com.minseok.reminderscreen

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import java.util.*

class NotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "TodoAlarmChannel"
    private val notificationSettings = NotificationSettings(context)

    init {
        createNotificationChannel()
    }

    fun scheduleNotification(todoItem: TodoItem) {
        todoItem.time?.let { time ->
            // 알람 시간 설정 (정확한 시간으로 변경)
            val alarmTime = Calendar.getInstance().apply {
                timeInMillis = time.time
            }

            val currentTime = System.currentTimeMillis()
            if (alarmTime.timeInMillis <= currentTime) {
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TODO_ID", todoItem.id)
                putExtra("TODO_CONTENT", todoItem.content)
                putExtra("TODO_TIME", time.time)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                todoItem.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(alarmTime.timeInMillis, pendingIntent),
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "할 일 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "할 일 알림을 위한 채널입니다"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            try {
                notificationManager.createNotificationChannel(channel)
                Log.d("NotificationHelper", "Notification channel created successfully")
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Error creating notification channel", e)
            }
        }
    }

    fun showNotification(content: String, time: Long) {
        Log.d("NotificationHelper", "Showing notification for: $content")

        try {
            val fullScreenIntent = Intent(context, TodoAlertDialog::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("TODO_CONTENT", content)
                putExtra("TODO_TIME", time)
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val soundUri = when (notificationSettings.soundType) {
                0 -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                1 -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                2 -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("할 일 알림")
                .setContentText("5분 후 예정: $content")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .apply {
                    if (notificationSettings.useSound) {
                        setSound(soundUri)
                    }
                }
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)

            Log.d("NotificationHelper", "Notification shown successfully")

            if (notificationSettings.useVibration) {
                vibrate()
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error showing notification", e)
        }
    }

    fun vibrate() {
        val pattern = when (notificationSettings.vibrationPattern) {
            0 -> longArrayOf(0, 200, 200, 200)
            1 -> longArrayOf(0, 500, 200, 500)
            2 -> longArrayOf(0, 200, 200, 200, 200, 200)
            else -> longArrayOf(0, 200, 200, 200)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            val vibrationEffect = VibrationEffect.createWaveform(pattern, -1)
            vibrator.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(pattern, -1)
                vibrator.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }
}