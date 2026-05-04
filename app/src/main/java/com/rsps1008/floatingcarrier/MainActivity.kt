package com.rsps1008.floatingcarrier

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY_PERMISSION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 檢查是否要顯示 UI，預設為 false（透明模式）
        val showUI = intent.getBooleanExtra("showUI", false)
        val hasOverlayPermission = Settings.canDrawOverlays(this)

        if (!showUI && hasOverlayPermission) {
            // 舊流程：已授權就直接進透明模式啟動懸浮窗
            startFloatingService()
            finish()
            return
        }

        // 首開沒權限，或是透過 wrench 進來時，都先顯示主畫面
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences("com.rsps1008.floatingcarrier.PREFERENCE_FILE", Context.MODE_PRIVATE)
        val editTextVehicleNumber = findViewById<EditText>(R.id.edit_text_vehicle_number)
        val saveButton = findViewById<Button>(R.id.save_button)
        val startButton = findViewById<Button>(R.id.start_service_button)
        val savedVehicleNumber = sharedPref.getString("vehicleNumber", "")
        editTextVehicleNumber.setText(savedVehicleNumber)

        saveButton.setOnClickListener {
            val vehicleNumber = editTextVehicleNumber.text.toString()
            val editor = sharedPref.edit()
            editor.putString("vehicleNumber", vehicleNumber)
            editor.apply()  // 儲存載具號碼
            updateFloatingService()
        }

        startButton.text = if (hasOverlayPermission) {
            getString(R.string.start_floating_button)
        } else {
            getString(R.string.request_overlay_button)
        }
        startButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService()
                if (!showUI) {
                    finish()
                }
            } else {
                val overlayIntent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(overlayIntent, REQUEST_CODE_OVERLAY_PERMISSION)
            }
        }

        if (showUI && hasOverlayPermission) {
            startFloatingService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService()
                if (!intent.getBooleanExtra("showUI", false)) {
                    finish()
                }
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
