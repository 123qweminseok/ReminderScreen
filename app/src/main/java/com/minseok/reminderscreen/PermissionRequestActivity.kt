package com.minseok.reminderscreen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class PermissionRequestActivity : AppCompatActivity() {

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_request)

        findViewById<Button>(R.id.btnGrantPermission).setOnClickListener {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                // 권한이 부여되었으므로 메인 액티비티로 이동
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // 사용자가 권한을 거부한 경우 처리
                // 여기서는 단순히 버튼을 다시 클릭할 수 있게 합니다.
                // 실제 앱에서는 사용자에게 왜 이 권한이 필요한지 추가 설명을 제공할 수 있습니다.
            }
        }
    }
}