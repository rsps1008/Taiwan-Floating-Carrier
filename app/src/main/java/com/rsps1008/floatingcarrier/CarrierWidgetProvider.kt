package com.rsps1008.floatingcarrier

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class CarrierWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_SHOW_FLOATING_FROM_WIDGET =
            "com.rsps1008.floatingcarrier.action.SHOW_FLOATING_FROM_WIDGET"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CarrierWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            widgetIds.forEach { widgetId ->
                updateWidget(context, appWidgetManager, widgetId, appWidgetManager.getAppWidgetOptions(widgetId))
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            options: Bundle
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_carrier_barcode)
            val sharedPref = context.getSharedPreferences(CarrierPrefs.PREF_FILE, Context.MODE_PRIVATE)
            val vehicleNumber = sharedPref.getString("vehicleNumber", null)
            val hasVehicleNumber = !vehicleNumber.isNullOrBlank()
            val showCarrierNumber = sharedPref.getBoolean(
                CarrierPrefs.KEY_WIDGET_SHOW_CARRIER_NUMBER,
                true
            )
            val density = context.resources.displayMetrics.density
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).takeIf { it > 0 }
                ?: options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).coerceAtLeast(180)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 }
                ?: options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).coerceAtLeast(72)
            val bitmapWidth = (widthDp * density).roundToInt().coerceAtLeast(240)
            val bitmapHeight = (heightDp * density).roundToInt().coerceAtLeast(96)

            val barcode = vehicleNumber?.takeIf { hasVehicleNumber }
                ?.let { CarrierBarcodeGenerator.generate(it, bitmapWidth, bitmapHeight) }
            if (barcode != null) {
                views.setImageViewBitmap(R.id.widget_barcode_image, barcode)
            }
            views.setViewVisibility(
                R.id.widget_barcode_image,
                if (barcode != null) android.view.View.VISIBLE else android.view.View.GONE
            )
            views.setViewVisibility(
                R.id.widget_barcode_text,
                if (hasVehicleNumber && showCarrierNumber) android.view.View.VISIBLE else android.view.View.GONE
            )
            views.setTextViewText(R.id.widget_barcode_text, vehicleNumber.orEmpty())
            views.setViewVisibility(
                R.id.widget_empty_hint,
                if (hasVehicleNumber) android.view.View.GONE else android.view.View.VISIBLE
            )

            val clickAction = sharedPref.getString(
                CarrierPrefs.KEY_WIDGET_CLICK_ACTION,
                CarrierPrefs.VALUE_WIDGET_CLICK_OPEN_FLOATING
            )
            val pendingIntent = when (clickAction) {
                CarrierPrefs.VALUE_WIDGET_CLICK_COPY_CARRIER -> {
                    val copyIntent = Intent(context, WidgetCopyActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    }
                    PendingIntent.getActivity(
                        context,
                        1,
                        copyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                CarrierPrefs.VALUE_WIDGET_CLICK_OPEN_SELECTED_APP -> {
                    val targetPackage = sharedPref.getString(CarrierPrefs.KEY_WIDGET_TARGET_PACKAGE, null)
                    val targetIntent = targetPackage?.let { context.packageManager.getLaunchIntentForPackage(it) }
                    if (targetIntent != null) {
                        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        PendingIntent.getActivity(
                            context,
                            2,
                            targetIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    } else {
                        createShowFloatingPendingIntent(context)
                    }
                }
                else -> {
                    createShowFloatingPendingIntent(context)
                }
            }

            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_barcode_image, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_barcode_text, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_hint, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createShowFloatingPendingIntent(context: Context): PendingIntent {
            val showFloatingIntent = Intent(context, CarrierWidgetProvider::class.java).apply {
                action = ACTION_SHOW_FLOATING_FROM_WIDGET
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                showFloatingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_SHOW_FLOATING_FROM_WIDGET -> {
                val serviceIntent = Intent(context, FloatingViewService::class.java).apply {
                    action = FloatingViewService.ACTION_SHOW_FLOATING_VIEW
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId, appWidgetManager.getAppWidgetOptions(appWidgetId))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidget(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onEnabled(context: Context) {
        updateAllWidgets(context)
    }
}
