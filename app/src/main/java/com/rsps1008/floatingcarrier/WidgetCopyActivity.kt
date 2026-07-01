package com.rsps1008.floatingcarrier

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WidgetCopyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences(CarrierPrefs.PREF_FILE, Context.MODE_PRIVATE)
        val vehicleNumber = sharedPref.getString("vehicleNumber", null)
        if (!vehicleNumber.isNullOrBlank()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("vehicle_number", vehicleNumber))
            Toast.makeText(this, getString(R.string.widget_copy_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.widget_copy_empty), Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}
