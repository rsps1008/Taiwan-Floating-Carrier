package com.rsps1008.floatingcarrier

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import kotlin.math.roundToInt

class CarrierWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_COPY_CARRIER = "com.rsps1008.floatingcarrier.action.COPY_CARRIER"

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
            val density = context.resources.displayMetrics.density
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).takeIf { it > 0 }
                ?: options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).coerceAtLeast(180)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 0 }
                ?: options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT).coerceAtLeast(72)
            val bitmapWidth = (widthDp * density).roundToInt().coerceAtLeast(240)
            val bitmapHeight = (heightDp * density * 0.72f).roundToInt().coerceAtLeast(96)

            val barcode = vehicleNumber?.let { CarrierBarcodeGenerator.generate(it, bitmapWidth, bitmapHeight) }
            if (barcode != null) {
                views.setImageViewBitmap(R.id.widget_barcode_image, barcode)
            }

            val clickAction = sharedPref.getString(
                CarrierPrefs.KEY_WIDGET_CLICK_ACTION,
                CarrierPrefs.VALUE_WIDGET_CLICK_OPEN_APP
            )
            val pendingIntent = when (clickAction) {
                CarrierPrefs.VALUE_WIDGET_CLICK_COPY_CARRIER -> {
                    val copyIntent = Intent(context, WidgetCopyActivity::class.java).apply {
                        action = ACTION_COPY_CARRIER
                    }
                    PendingIntent.getActivity(
                        context,
                        1,
                        copyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                else -> {
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        putExtra("showUI", true)
                    }
                    PendingIntent.getActivity(
                        context,
                        0,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            }

            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_barcode_image, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
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
