package com.minseok.reminderscreen

import android.media.RingtoneManager
import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import android.media.Ringtone
import android.widget.Button
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.os.Build
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.view.View
import com.google.android.material.button.MaterialButton

class NotificationSettingsActivity : AppCompatActivity() {
    private lateinit var notificationSettings: NotificationSettings
    private var currentTestRingtone: Ringtone? = null
    private lateinit var rgVibrationPatterns: RadioGroup
    private lateinit var rgSoundTypes: RadioGroup
    private var customSoundUri: Uri? = null

    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            customSoundUri = it
            rgSoundTypes.check(R.id.rbSoundType4)
            notificationSettings.soundType = 3
            notificationSettings.customSoundUri = it.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        notificationSettings = NotificationSettings(this)

        rgVibrationPatterns = findViewById(R.id.rgVibrationPatterns)
        rgSoundTypes = findViewById(R.id.rgSoundTypes)

        rgVibrationPatterns.isEnabled = notificationSettings.useVibration
        rgSoundTypes.isEnabled = notificationSettings.useSound

        setupSwitches()
        setupVibrationPatterns()
        setupSoundTypes()
        setupTestButtons()
    }

    private fun setupSwitches() {
        findViewById<SwitchMaterial>(R.id.switchVibration).apply {
            isChecked = notificationSettings.useVibration
            setOnCheckedChangeListener { _, isChecked ->
                notificationSettings.useVibration = isChecked
                rgVibrationPatterns.isEnabled = isChecked
            }
        }

        findViewById<SwitchMaterial>(R.id.switchSound).apply {
            isChecked = notificationSettings.useSound
            setOnCheckedChangeListener { _, isChecked ->
                notificationSettings.useSound = isChecked
                rgSoundTypes.isEnabled = isChecked
            }
        }

        findViewById<SwitchMaterial>(R.id.switchTodoCount).apply {
            isChecked = notificationSettings.showTodoCountNotification
            setOnCheckedChangeListener { _, isChecked ->
                notificationSettings.showTodoCountNotification = isChecked
            }
        }
    }
    private fun setupVibrationPatterns() {
        rgVibrationPatterns.check(
            when (notificationSettings.vibrationPattern) {
                0 -> R.id.rbVibrationPattern1
                1 -> R.id.rbVibrationPattern2
                2 -> R.id.rbVibrationPattern3
                else -> R.id.rbVibrationPattern1
            }
        )
        rgVibrationPatterns.setOnCheckedChangeListener { _, checkedId ->
            notificationSettings.vibrationPattern = when (checkedId) {
                R.id.rbVibrationPattern1 -> 0
                R.id.rbVibrationPattern2 -> 1
                R.id.rbVibrationPattern3 -> 2
                else -> 0
            }
        }
    }

    private fun setupSoundTypes() {
        rgSoundTypes.check(
            when (notificationSettings.soundType) {
                0 -> R.id.rbSoundType1
                1 -> R.id.rbSoundType2
                2 -> R.id.rbSoundType3
                3 -> R.id.rbSoundType4
                else -> R.id.rbSoundType1
            }
        )

        val btnSelectSound = findViewById<MaterialButton>(R.id.btnSelectSound)

        rgSoundTypes.setOnCheckedChangeListener { _, checkedId ->
            notificationSettings.soundType = when (checkedId) {
                R.id.rbSoundType1 -> 0
                R.id.rbSoundType2 -> 1
                R.id.rbSoundType3 -> 2
                R.id.rbSoundType4 -> 3
                else -> 0
            }
            btnSelectSound.visibility = if (checkedId == R.id.rbSoundType4) View.VISIBLE else View.GONE
            stopTestSound()
        }

        btnSelectSound.setOnClickListener {
            pickAudio.launch("audio/*")
        }

        // 기존 사용자 지정 사운드 URI 복원
        notificationSettings.customSoundUri?.let {
            customSoundUri = Uri.parse(it)
        }

        // 사용자 지정 사운드가 선택되어 있으면 버튼 표시
        if (notificationSettings.soundType == 3) {
            btnSelectSound.visibility = View.VISIBLE
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
            0 -> longArrayOf(0, 200, 200, 200)
            1 -> longArrayOf(0, 500, 200, 500)
            2 -> longArrayOf(0, 200, 200, 200, 200, 200)
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
            3 -> customSoundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
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