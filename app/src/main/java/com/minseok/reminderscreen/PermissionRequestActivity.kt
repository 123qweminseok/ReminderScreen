package com.minseok.reminderscreen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button


class PermissionRequestActivity : AppCompatActivity() {

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
        private const val PREFS_NAME = "PermissionPrefs"
        private const val PREF_OVERLAY_PERMISSION = "overlay_permission"
    }

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 이미 권한이 있거나 권한을 승인한 적이 있는지 확인
        if (checkPermission()) {
            startMainActivity()
            return
        }

        setContentView(R.layout.activity_permission_request)

        findViewById<Button>(R.id.btnGrantPermission).setOnClickListener {
            requestOverlayPermission()
        }
    }

    private fun checkPermission(): Boolean {
        // 실제 권한이 있고 AND SharedPreferences에 저장된 값이 true인 경우
        return Settings.canDrawOverlays(this) &&
                sharedPreferences.getBoolean(PREF_OVERLAY_PERMISSION, false)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                // 권한이 부여되면 SharedPreferences에 저장
                sharedPreferences.edit()
                    .putBoolean(PREF_OVERLAY_PERMISSION, true)
                    .apply()

                startMainActivity()
            } else {
                // 권한이 거부된 경우
                sharedPreferences.edit()
                    .putBoolean(PREF_OVERLAY_PERMISSION, false)
                    .apply()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때마다 권한 체크
        if (checkPermission()) {
            startMainActivity()
        }
    }
}