package com.example.taiwanbarcodefloating

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY_PERMISSION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 檢查是否要顯示 UI，預設為 false（透明模式）
        val showUI = intent.getBooleanExtra("showUI", false)
        if (!showUI) {
            // 透明模式：僅啟動懸浮窗服務並立即 finish
            if (Settings.canDrawOverlays(this)) {
                startFloatingService()
            } else {
                // 請求 overlay 權限
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
            }
            finish()
            return
        } else {
            // 若透過 ic_wrench 點擊進入，則切換為正常主題顯示設定 UI
            setTheme(R.style.AppTheme)
            setContentView(R.layout.activity_main)

            val sharedPref = getSharedPreferences("com.example.taiwanbarcodefloating.PREFERENCE_FILE", Context.MODE_PRIVATE)
            val editTextVehicleNumber = findViewById<EditText>(R.id.edit_text_vehicle_number)
            val saveButton = findViewById<Button>(R.id.save_button)
            val savedVehicleNumber = sharedPref.getString("vehicleNumber", "")
            editTextVehicleNumber.setText(savedVehicleNumber)
            saveButton.setOnClickListener {
                val vehicleNumber = editTextVehicleNumber.text.toString()
                val editor = sharedPref.edit()
                editor.putString("vehicleNumber", vehicleNumber)
                editor.apply()  // 儲存載具號碼
                updateFloatingService()
            }
            startFloatingService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService()
            }
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingViewService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun updateFloatingService() {
        stopService(Intent(this, FloatingViewService::class.java))
        startFloatingService()
    }
}
