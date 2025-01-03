package com.minseok.reminderscreen

import android.content.Context

class NotificationSettings(context: Context) {
    private val prefs = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)

    var useVibration: Boolean
        get() = prefs.getBoolean("use_vibration", true)
        set(value) = prefs.edit().putBoolean("use_vibration", value).apply()

    var useSound: Boolean
        get() = prefs.getBoolean("use_sound", true)
        set(value) = prefs.edit().putBoolean("use_sound", value).apply()



    var showTodoCountNotification: Boolean
        get() = prefs.getBoolean("show_todo_count_notification", false)  // 기본값을 false로 변경
        set(value) = prefs.edit().putBoolean("show_todo_count_notification", value).apply()

    var vibrationPattern: Int
        get() = prefs.getInt("vibration_pattern", 0)
        set(value) = prefs.edit().putInt("vibration_pattern", value).apply()

    var soundType: Int
        get() = prefs.getInt("sound_type", 0)
        set(value) = prefs.edit().putInt("sound_type", value).apply()

    var customSoundUri: String?
        get() = prefs.getString("custom_sound_uri", null)
        set(value) = prefs.edit().putString("custom_sound_uri", value).apply()
}