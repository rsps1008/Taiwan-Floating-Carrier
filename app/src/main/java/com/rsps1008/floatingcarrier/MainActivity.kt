package com.rsps1008.floatingcarrier

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY_PERMISSION = 1001
    private val REQUEST_CODE_NOTIFICATION_PERMISSION = 1002
    private val settingsHandler = Handler(Looper.getMainLooper())
    private var vehicleApplyRunnable: Runnable? = null
    private var opacityApplyRunnable: Runnable? = null
    private var pendingOverlayPrompt = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        val sharedPref = getSharedPreferences(CarrierPrefs.PREF_FILE, Context.MODE_PRIVATE)
        requestNotificationPermissionIfNeeded()
        showSettingsUi(sharedPref, hasOverlayPermission)
        pendingOverlayPrompt = !hasOverlayPermission && !sharedPref.getBoolean(CarrierPrefs.KEY_OVERLAY_PROMPT_SUPPRESSED, false)
        if (pendingOverlayPrompt) {
            showOverlayPermissionPrompt(sharedPref)
        }
    }

    private fun showSettingsUi(sharedPref: android.content.SharedPreferences, hasOverlayPermission: Boolean) {
        settingsHandler.removeCallbacksAndMessages(null)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_main)

        val editTextVehicleNumber = findViewById<TextInputEditText>(R.id.edit_text_vehicle_number)
        val vehicleHintText = findViewById<TextView>(R.id.vehicle_number_hint_text)
        val opacitySeekBar = findViewById<SeekBar>(R.id.opacity_seekbar)
        val opacityValueText = findViewById<TextView>(R.id.opacity_value_text)
        val closeActionGroup = findViewById<RadioGroup>(R.id.close_action_group)
        val widgetClickGroup = findViewById<RadioGroup>(R.id.widget_click_group)
        val resetOverlayPromptButton = findViewById<Button>(R.id.reset_overlay_prompt_button)
        val startButton = findViewById<Button>(R.id.start_service_button)
        val savedVehicleNumber = sharedPref.getString("vehicleNumber", "")
        val savedOpacity = sharedPref.getInt("expandedOpacity", 100)
        val savedCloseAction = sharedPref.getString(
            FloatingViewService.PREF_KEY_CLOSE_ACTION,
            FloatingViewService.PREF_VALUE_CLOSE_FLOATING
        )
        val savedWidgetClickAction = sharedPref.getString(
            CarrierPrefs.KEY_WIDGET_CLICK_ACTION,
            CarrierPrefs.VALUE_WIDGET_CLICK_OPEN_FLOATING
        )
        editTextVehicleNumber.setText(savedVehicleNumber?.removePrefix("/") ?: "")
        opacitySeekBar.progress = ((savedOpacity.coerceIn(50, 100) - 50) / 10)
        opacityValueText.text = "${savedOpacity.coerceIn(50, 100)}%"
        when (savedCloseAction) {
            FloatingViewService.PREF_VALUE_CLOSE_APP -> closeActionGroup.check(R.id.close_action_close_app)
            else -> closeActionGroup.check(R.id.close_action_close_floating)
        }
        when (savedWidgetClickAction) {
            CarrierPrefs.VALUE_WIDGET_CLICK_COPY_CARRIER -> widgetClickGroup.check(R.id.widget_click_copy_carrier)
            else -> widgetClickGroup.check(R.id.widget_click_open_app)
        }

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
                .commit()
            vehicleHintText.setTextColor(getColor(R.color.text_secondary))
            vehicleHintText.text = getString(R.string.vehicle_applied_hint)
            vehicleHintText.visibility = View.VISIBLE
            CarrierWidgetProvider.updateAllWidgets(this)
            if (Settings.canDrawOverlays(this) && isFloatingServiceRunning()) {
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
                .commit()
            opacityValueText.text = getString(R.string.opacity_value_100).replace("100", selectedOpacity.toString())
            CarrierWidgetProvider.updateAllWidgets(this)
            if (Settings.canDrawOverlays(this) && isFloatingServiceRunning()) {
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

        closeActionGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedAction = when (checkedId) {
                R.id.close_action_close_app -> FloatingViewService.PREF_VALUE_CLOSE_APP
                else -> FloatingViewService.PREF_VALUE_CLOSE_FLOATING
            }
            sharedPref.edit()
                .putString(FloatingViewService.PREF_KEY_CLOSE_ACTION, selectedAction)
                .commit()
        }

        widgetClickGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedAction = when (checkedId) {
                R.id.widget_click_copy_carrier -> CarrierPrefs.VALUE_WIDGET_CLICK_COPY_CARRIER
                else -> CarrierPrefs.VALUE_WIDGET_CLICK_OPEN_FLOATING
            }
            sharedPref.edit()
                .putString(CarrierPrefs.KEY_WIDGET_CLICK_ACTION, selectedAction)
                .apply()
            CarrierWidgetProvider.updateAllWidgets(this)
        }

        resetOverlayPromptButton.setOnClickListener {
            sharedPref.edit()
                .putBoolean(CarrierPrefs.KEY_OVERLAY_PROMPT_SUPPRESSED, false)
                .apply()
            pendingOverlayPrompt = !Settings.canDrawOverlays(this)
            if (pendingOverlayPrompt) {
                showOverlayPermissionPrompt(sharedPref)
            }
        }

        startButton.text = if (hasOverlayPermission) {
            getString(R.string.start_floating_button)
        } else {
            getString(R.string.request_overlay_settings_button)
        }
        startButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService()
                finish()
            } else {
                if (sharedPref.getBoolean(CarrierPrefs.KEY_OVERLAY_PROMPT_SUPPRESSED, false)) {
                    launchOverlaySettings()
                } else {
                    showOverlayPermissionPrompt(sharedPref, force = true)
                }
            }
        }
    }

    private fun showOverlayPermissionPrompt(
        sharedPref: android.content.SharedPreferences,
        force: Boolean = false
    ) {
        if (pendingOverlayPrompt || force) {
            pendingOverlayPrompt = false
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.overlay_prompt_title))
                .setMessage(getString(R.string.overlay_prompt_message))
                .setCancelable(true)
                .setNegativeButton(getString(R.string.overlay_prompt_cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setNeutralButton(getString(R.string.overlay_prompt_dont_ask)) { dialog, _ ->
                    sharedPref.edit()
                        .putBoolean(CarrierPrefs.KEY_OVERLAY_PROMPT_SUPPRESSED, true)
                        .apply()
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.overlay_prompt_go_settings)) { dialog, _ ->
                    dialog.dismiss()
                    launchOverlaySettings()
                }
                .show()
        }
    }

    private fun launchOverlaySettings() {
        val overlayIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(overlayIntent, REQUEST_CODE_OVERLAY_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                val sharedPref = getSharedPreferences(CarrierPrefs.PREF_FILE, Context.MODE_PRIVATE)
                showSettingsUi(sharedPref, true)
                findViewById<TextView>(R.id.subtitle_text).text = getString(R.string.overlay_permission_granted_hint)
                if (isFloatingServiceRunning()) {
                    startFloatingService(action = FloatingViewService.ACTION_UPDATE_SETTINGS)
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

    private fun isFloatingServiceRunning(): Boolean = AppRuntimeState.isFloatingServiceRunning

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE_NOTIFICATION_PERMISSION
        )
    }
}
