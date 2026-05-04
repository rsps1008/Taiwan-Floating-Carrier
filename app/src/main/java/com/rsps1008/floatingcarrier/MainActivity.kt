package com.rsps1008.floatingcarrier

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.view.View
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY_PERMISSION = 1001
    private val settingsHandler = Handler(Looper.getMainLooper())
    private var vehicleApplyRunnable: Runnable? = null
    private var opacityApplyRunnable: Runnable? = null

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
        val editTextVehicleNumber = findViewById<TextInputEditText>(R.id.edit_text_vehicle_number)
        val vehicleHintText = findViewById<TextView>(R.id.vehicle_number_hint_text)
        val opacitySeekBar = findViewById<SeekBar>(R.id.opacity_seekbar)
        val opacityValueText = findViewById<TextView>(R.id.opacity_value_text)
        val startButton = findViewById<Button>(R.id.start_service_button)
        val savedVehicleNumber = sharedPref.getString("vehicleNumber", "")
        val savedOpacity = sharedPref.getInt("expandedOpacity", 100)
        editTextVehicleNumber.setText(savedVehicleNumber?.removePrefix("/") ?: "")
        opacitySeekBar.progress = ((savedOpacity.coerceIn(50, 100) - 50) / 10)
        opacityValueText.text = "${savedOpacity.coerceIn(50, 100)}%"

        fun currentVehicleInput(): String = editTextVehicleNumber.text?.toString()?.trim().orEmpty()
        fun updateVehicleHint() {
            val text = currentVehicleInput()
            val remaining = 7 - text.length
            if (remaining > 0) {
                vehicleHintText.setTextColor(getColor(R.color.error_text))
                vehicleHintText.text = getString(R.string.vehicle_short_hint, remaining)
                vehicleHintText.visibility = View.VISIBLE
            } else {
                vehicleHintText.setTextColor(getColor(R.color.text_secondary))
                vehicleHintText.text = getString(R.string.vehicle_applied_hint)
                vehicleHintText.visibility = View.VISIBLE
            }
        }
        fun persistVehicleIfReady() {
            val raw = currentVehicleInput().take(7)
            if (raw.length < 7) {
                updateVehicleHint()
                return
            }
            val vehicleNumber = "/$raw"
            sharedPref.edit()
                .putString("vehicleNumber", vehicleNumber)
                .apply()
            vehicleHintText.setTextColor(getColor(R.color.text_secondary))
            vehicleHintText.text = getString(R.string.vehicle_applied_hint)
            vehicleHintText.visibility = View.VISIBLE
            if (Settings.canDrawOverlays(this)) {
                startFloatingService(action = FloatingViewService.ACTION_UPDATE_SETTINGS)
            }
        }

        updateVehicleHint()

        vehicleApplyRunnable = Runnable { persistVehicleIfReady() }
        editTextVehicleNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                settingsHandler.removeCallbacks(vehicleApplyRunnable!!)
                val current = currentVehicleInput()
                val remaining = 7 - current.length
                if (remaining > 0) {
                    vehicleHintText.setTextColor(getColor(R.color.error_text))
                    vehicleHintText.text = getString(R.string.vehicle_short_hint, remaining)
                    vehicleHintText.visibility = View.VISIBLE
                    return
                }
                vehicleHintText.setTextColor(getColor(R.color.text_secondary))
                vehicleHintText.text = getString(R.string.vehicle_applied_hint)
                vehicleHintText.visibility = View.VISIBLE
                settingsHandler.postDelayed(vehicleApplyRunnable!!, 700)
            }
        })

        opacityApplyRunnable = Runnable {
            val selectedOpacity = 50 + (opacitySeekBar.progress.coerceIn(0, 5) * 10)
            sharedPref.edit()
                .putInt("expandedOpacity", selectedOpacity)
                .apply()
            opacityValueText.text = getString(R.string.opacity_value_100).replace("100", selectedOpacity.toString())
            if (Settings.canDrawOverlays(this)) {
                startFloatingService(action = FloatingViewService.ACTION_UPDATE_SETTINGS)
            }
        }

        opacitySeekBar.max = 5
        opacitySeekBar.progress = ((savedOpacity.coerceIn(50, 100) - 50) / 10)
        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val opacity = 50 + (progress.coerceIn(0, 5) * 10)
                opacityValueText.text = "$opacity%"
                settingsHandler.removeCallbacks(opacityApplyRunnable!!)
                settingsHandler.postDelayed(opacityApplyRunnable!!, 700)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

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

    private fun startFloatingService(action: String? = null) {
        val intent = Intent(this, FloatingViewService::class.java)
        if (action != null) {
            intent.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
