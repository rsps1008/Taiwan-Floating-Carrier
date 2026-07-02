package com.rsps1008.floatingcarrier

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class CarrierWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_COPY_CARRIER = "com.rsps1008.floatingcarrier.action.COPY_CARRIER"
        private const val NOTIFICATION_CHANNEL_ID = "widget_feedback_channel"
        private const val NOTIFICATION_ID_COPY_SUCCESS = 1001
        private const val NOTIFICATION_ID_COPY_EMPTY = 1002

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
                CarrierPrefs.VALUE_WIDGET_CLICK_OPEN_FLOATING
            )
            val pendingIntent = when (clickAction) {
                CarrierPrefs.VALUE_WIDGET_CLICK_COPY_CARRIER -> {
                    val copyIntent = Intent(context, CarrierWidgetProvider::class.java).apply {
                        action = ACTION_COPY_CARRIER
                    }
                    PendingIntent.getBroadcast(
                        context,
                        1,
                        copyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                else -> {
                    val showFloatingIntent = Intent(context, FloatingViewService::class.java).apply {
                        action = FloatingViewService.ACTION_SHOW_FLOATING_VIEW
                    }
                    PendingIntent.getService(
                        context,
                        0,
                        showFloatingIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            }

            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_barcode_image, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun copyCarrierToClipboard(context: Context) {
            val sharedPref = context.getSharedPreferences(CarrierPrefs.PREF_FILE, Context.MODE_PRIVATE)
            val vehicleNumber = sharedPref.getString("vehicleNumber", null)
            val appContext = context.applicationContext
            if (!vehicleNumber.isNullOrBlank()) {
                val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("vehicle_number", vehicleNumber))
                showCopyNotification(
                    appContext,
                    NOTIFICATION_ID_COPY_SUCCESS,
                    appContext.getString(R.string.widget_copy_success)
                )
                updateAllWidgets(appContext)
            } else {
                showCopyNotification(
                    appContext,
                    NOTIFICATION_ID_COPY_EMPTY,
                    appContext.getString(R.string.widget_copy_empty)
                )
            }
        }

        private fun showCopyNotification(context: Context, notificationId: Int, message: String) {
            if (!canPostNotifications(context)) {
                return
            }
            ensureNotificationChannel(context)
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }

        private fun ensureNotificationChannel(context: Context) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
                return
            }
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.widget_feedback_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        private fun canPostNotifications(context: Context): Boolean {
            return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_COPY_CARRIER) {
            copyCarrierToClipboard(context)
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
