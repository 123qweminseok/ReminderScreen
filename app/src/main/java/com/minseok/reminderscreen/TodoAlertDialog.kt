package com.minseok.reminderscreen

import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import java.text.SimpleDateFormat
import java.util.*

class TodoAlertDialog : AppCompatActivity() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isAlarmActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_todo_alert)

        // 화면을 켜고 잠금 화면 위에 표시
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // WakeLock 획득
        acquireWakeLock()

        val content = intent.getStringExtra("TODO_CONTENT") ?: ""
        val time = intent.getLongExtra("TODO_TIME", 0)

        setupUI(content, time)
        startAlarm()
    }

    private fun setupUI(content: String, time: Long) {
        // 제목 설정
        findViewById<TextView>(R.id.tvAlertTitle).apply {
            text = "할 일 알림"
            startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
        }

        // 내용 설정
        findViewById<TextView>(R.id.tvTodoContent).apply {
            text = content
            startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
        }

        // 시간 설정
        findViewById<TextView>(R.id.tvTodoTime).apply {
            text = formatTime(time)
            startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
        }

        // 버튼 설정
        findViewById<Button>(R.id.btnConfirm).apply {
            setOnClickListener {
                stopAlarmAndFinish()
            }
            startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left))
        }

        // 5분 후 자동 종료
        handler.postDelayed({
            if (isAlarmActive) {
                stopAlarmAndFinish()
            }
        }, 5 * 60 * 1000)  // 5분
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE, "ReminderScreen:AlertWakeLock"
        )
        wakeLock?.acquire(5 * 60 * 1000L) // 5분
    }

    private fun startAlarm() {
        try {
            mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI).apply {
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 진동 시작
        val notificationHelper = NotificationHelper(this)
        notificationHelper.vibrate()
    }

    private fun stopAlarmAndFinish() {
        isAlarmActive = false
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null

        wakeLock?.release()
        wakeLock = null

        handler.removeCallbacksAndMessages(null)
        finish()
    }

    private fun formatTime(time: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmAndFinish()
    }

    override fun onBackPressed() {
        stopAlarmAndFinish()
    }
}