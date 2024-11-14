package com.minseok.reminderscreen

import android.media.RingtoneManager
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import android.widget.Switch
import android.media.Ringtone
import android.widget.Button
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.os.Build
class NotificationSettingsActivity : AppCompatActivity() {
    private lateinit var notificationSettings: NotificationSettings
    private var currentTestRingtone: Ringtone? = null
    private lateinit var rgVibrationPatterns: RadioGroup  // 추가
    private lateinit var rgSoundTypes: RadioGroup         // 추가

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        notificationSettings = NotificationSettings(this)

        // RadioGroup 초기화를 먼저 수행
        rgVibrationPatterns = findViewById(R.id.rgVibrationPatterns)
        rgSoundTypes = findViewById(R.id.rgSoundTypes)

        // 초기 상태 설정
        rgVibrationPatterns.isEnabled = notificationSettings.useVibration
        rgSoundTypes.isEnabled = notificationSettings.useSound

        setupSwitches()
        setupVibrationPatterns()
        setupSoundTypes()
        setupTestButtons()
    }

    private fun setupSwitches() {
        // 진동 스위치 설정
        findViewById<Switch>(R.id.switchVibration).apply {
            isChecked = notificationSettings.useVibration
            setOnCheckedChangeListener { _, isChecked ->
                notificationSettings.useVibration = isChecked
                rgVibrationPatterns.isEnabled = isChecked  // 변경
            }
        }

        // 소리 스위치 설정
        findViewById<Switch>(R.id.switchSound).apply {
            isChecked = notificationSettings.useSound
            setOnCheckedChangeListener { _, isChecked ->
                notificationSettings.useSound = isChecked
                rgSoundTypes.isEnabled = isChecked  // 변경
            }
        }
    }

    private fun setupVibrationPatterns() {
        rgVibrationPatterns.check(  // 변경
            when (notificationSettings.vibrationPattern) {
                0 -> R.id.rbVibrationPattern1
                1 -> R.id.rbVibrationPattern2
                2 -> R.id.rbVibrationPattern3
                else -> R.id.rbVibrationPattern1
            }
        )
        rgVibrationPatterns.setOnCheckedChangeListener { _, checkedId ->  // 변경
            notificationSettings.vibrationPattern = when (checkedId) {
                R.id.rbVibrationPattern1 -> 0
                R.id.rbVibrationPattern2 -> 1
                R.id.rbVibrationPattern3 -> 2
                else -> 0
            }
        }
    }

    private fun setupSoundTypes() {
        rgSoundTypes.check(  // 변경
            when (notificationSettings.soundType) {
                0 -> R.id.rbSoundType1
                1 -> R.id.rbSoundType2
                2 -> R.id.rbSoundType3
                else -> R.id.rbSoundType1
            }
        )
        rgSoundTypes.setOnCheckedChangeListener { _, checkedId ->  // 변경
            notificationSettings.soundType = when (checkedId) {
                R.id.rbSoundType1 -> 0
                R.id.rbSoundType2 -> 1
                R.id.rbSoundType3 -> 2
                else -> 0
            }
            stopTestSound()
        }
    }








    private fun setupTestButtons() {
        findViewById<Button>(R.id.btnTestVibration).setOnClickListener {
            testVibration()
        }

        findViewById<Button>(R.id.btnTestSound).setOnClickListener {
            testSound()
        }
    }

    private fun testVibration() {
        if (!notificationSettings.useVibration) return

        val pattern = when (notificationSettings.vibrationPattern) {
            0 -> longArrayOf(0, 200, 200, 200) // 짧은 진동
            1 -> longArrayOf(0, 500, 200, 500) // 긴 진동
            2 -> longArrayOf(0, 200, 200, 200, 200, 200) // 연속 진동
            else -> longArrayOf(0, 200, 200, 200)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            val vibrationEffect = VibrationEffect.createWaveform(pattern, -1)
            vibrator.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(pattern, -1)
                vibrator.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    private fun testSound() {
        stopTestSound()

        if (!notificationSettings.useSound) return

        val soundUri = when (notificationSettings.soundType) {
            0 -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            1 -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            2 -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        currentTestRingtone = RingtoneManager.getRingtone(this, soundUri).apply {
            play()
        }
    }

    private fun stopTestSound() {
        currentTestRingtone?.stop()
        currentTestRingtone = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTestSound()
    }
}