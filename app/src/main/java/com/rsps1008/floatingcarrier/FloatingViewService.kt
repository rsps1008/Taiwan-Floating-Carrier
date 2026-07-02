package com.rsps1008.floatingcarrier

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.abs

class FloatingViewService : Service() {

    companion object {
        const val ACTION_SHOW_FLOATING_VIEW = "com.rsps1008.floatingcarrier.action.SHOW_FLOATING_VIEW"
        const val ACTION_UPDATE_SETTINGS = "com.rsps1008.floatingcarrier.action.UPDATE_SETTINGS"
        const val PREF_KEY_CLOSE_ACTION = "closeAction"
        const val PREF_VALUE_CLOSE_FLOATING = "close_floating"
        const val PREF_VALUE_CLOSE_APP = "close_app"
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var bubbleContainer: View? = null
    private var contentContainer: View? = null
    private var isBarcodeVisible = true  // 記錄條碼是否可見
    private var isBubbleCollapsed = false
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var expandedWidthPx: Int = 0
    private var bubbleSizePx: Int = 0
    private var isDragging = false       // 判斷是否正在拖曳

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_FLOATING_VIEW -> {
                if (isBubbleCollapsed) {
                    expandFromBubble(bubbleContainer, contentContainer)
                }
                refreshFromPrefs()
            }
            ACTION_UPDATE_SETTINGS -> {
                refreshFromPrefs()
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        AppRuntimeState.isFloatingServiceRunning = true
        // 建立前景服務通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "floating_view_service_channel"
            val channelName = "Floating View Service"
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)

            val notification = Notification.Builder(this, channelId)
                .setContentTitle("Floating Service Running")
                .setContentText("Displaying the floating view")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notification)
            }
        }

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)

        // 取得螢幕寬度
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        expandedWidthPx = (screenWidth * 0.8).toInt()
        bubbleSizePx = (displayMetrics.density * 64).toInt()

        // 設定懸浮窗寬度為螢幕寬度的80%
        layoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                expandedWidthPx,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                expandedWidthPx,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }

        // 從 SharedPreferences 取得上次位置
        val sharedPref = getSharedPreferences("com.rsps1008.floatingcarrier.PREFERENCE_FILE", Context.MODE_PRIVATE)
        layoutParams.x = sharedPref.getInt("lastXPosition", 0)
        layoutParams.y = sharedPref.getInt("lastYPosition", 100)
        val expandedOpacity = sharedPref.getInt("expandedOpacity", 100).coerceIn(50, 100)

        // 加入懸浮視窗
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.addView(floatingView, layoutParams)
        floatingView?.alpha = expandedOpacity / 100f

        bubbleContainer = floatingView?.findViewById<View>(R.id.bubble_container)
        val bubbleIcon = floatingView?.findViewById<View>(R.id.bubble_icon)
        contentContainer = floatingView?.findViewById<View>(R.id.content_container)
        val headerContainer = floatingView?.findViewById<View>(R.id.header_container)
        val barcodeImageView = floatingView?.findViewById<ImageView>(R.id.barcode_image)
        val vehicleNumberTextView = floatingView?.findViewById<TextView>(R.id.vehicle_number_text)

        bubbleContainer?.setOnClickListener {
            expandFromBubble(bubbleContainer, contentContainer)
        }
        val bubbleTouchListener = object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var touchX: Float = 0f
            private var touchY: Float = 0f
            private var isBubbleDragging = false
            private val dragThreshold = 10

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (!isBubbleCollapsed) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        isBubbleDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - touchX
                        val deltaY = event.rawY - touchY
                        if (abs(deltaX) > dragThreshold || abs(deltaY) > dragThreshold) {
                            layoutParams.x = initialX + deltaX.toInt()
                            layoutParams.y = initialY + deltaY.toInt()
                            windowManager?.updateViewLayout(floatingView, layoutParams)
                            isBubbleDragging = true
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isBubbleDragging) {
                            expandFromBubble(bubbleContainer, contentContainer)
                        }
                        val editor = sharedPref.edit()
                        editor.putInt("lastXPosition", layoutParams.x)
                        editor.putInt("lastYPosition", layoutParams.y)
                        editor.apply()
                        return true
                    }
                }
                return false
            }
        }
        bubbleContainer?.setOnTouchListener(bubbleTouchListener)
        bubbleIcon?.setOnTouchListener(bubbleTouchListener)

        fun createExpandedDragListener(onTap: (() -> Unit)?): View.OnTouchListener {
            return object : View.OnTouchListener {
                private var initialX: Int = 0
                private var initialY: Int = 0
                private var touchX: Float = 0f
                private var touchY: Float = 0f
                private var isViewDragging = false
                private val dragThreshold = 10

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    if (isBubbleCollapsed) return false
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = layoutParams.x
                            initialY = layoutParams.y
                            touchX = event.rawX
                            touchY = event.rawY
                            isViewDragging = false
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - touchX
                            val deltaY = event.rawY - touchY
                            if (abs(deltaX) > dragThreshold || abs(deltaY) > dragThreshold) {
                                layoutParams.x = initialX + deltaX.toInt()
                                layoutParams.y = initialY + deltaY.toInt()
                                windowManager?.updateViewLayout(floatingView, layoutParams)
                                isViewDragging = true
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!isViewDragging) {
                                onTap?.invoke()
                            }
                            val editor = sharedPref.edit()
                            editor.putInt("lastXPosition", layoutParams.x)
                            editor.putInt("lastYPosition", layoutParams.y)
                            editor.apply()
                            return true
                        }
                    }
                    return false
                }
            }
        }

        headerContainer?.setOnTouchListener(createExpandedDragListener {
            collapseToBubble(bubbleContainer, contentContainer)
        })

        contentContainer?.setOnTouchListener(createExpandedDragListener(null))

        barcodeImageView?.setOnTouchListener(createExpandedDragListener {
            collapseToBubble(bubbleContainer, contentContainer)
        })

        vehicleNumberTextView?.setOnTouchListener(createExpandedDragListener {
            collapseToBubble(bubbleContainer, contentContainer)
        })

        // 關閉按鈕事件
        val closeBtn = floatingView?.findViewById<ImageView>(R.id.close_btn)
        closeBtn?.setOnClickListener {
            val closeAction = sharedPref.getString(PREF_KEY_CLOSE_ACTION, PREF_VALUE_CLOSE_FLOATING)
            stopSelf()
            if (closeAction == PREF_VALUE_CLOSE_APP) {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }

        // 設定按鈕（板手 icon）的點擊事件，並傳入 showUI=true 以顯示設定介面
        val settingBtn = floatingView?.findViewById<ImageButton>(R.id.setting_btn)
        settingBtn?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("showUI", true)
            startActivity(intent)
        }

        // 生成條碼與顯示載具號碼
        val barcodeWidth = (screenWidth * 0.8).toInt()
        val finalVehicleNumber = sharedPref.getString("vehicleNumber", null)
        barcodeImageView?.setImageBitmap(
            finalVehicleNumber?.let { CarrierBarcodeGenerator.generate(it, barcodeWidth, (screenWidth / 11 * 2)) }
        )
        vehicleNumberTextView?.text = finalVehicleNumber.orEmpty()
        CarrierWidgetProvider.updateAllWidgets(this)
    }

    private fun refreshFromPrefs() {
        val sharedPref = getSharedPreferences("com.rsps1008.floatingcarrier.PREFERENCE_FILE", Context.MODE_PRIVATE)
        val expandedOpacity = sharedPref.getInt("expandedOpacity", 100).coerceIn(50, 100)
        if (isBubbleCollapsed) {
            floatingView?.alpha = 1f
        } else {
            floatingView?.alpha = expandedOpacity / 100f
        }

        val finalVehicleNumber = sharedPref.getString("vehicleNumber", null)
        val barcodeImageView = floatingView?.findViewById<ImageView>(R.id.barcode_image)
        val vehicleNumberTextView = floatingView?.findViewById<TextView>(R.id.vehicle_number_text)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val barcodeWidth = (screenWidth * 0.8).toInt()
        barcodeImageView?.setImageBitmap(
            finalVehicleNumber?.let { CarrierBarcodeGenerator.generate(it, barcodeWidth, (screenWidth / 11 * 2)) }
        )
        vehicleNumberTextView?.text = finalVehicleNumber.orEmpty()
        CarrierWidgetProvider.updateAllWidgets(this)
    }

    private fun collapseToBubble(bubbleContainer: View?, contentContainer: View?) {
        if (isBubbleCollapsed) return
        contentContainer?.visibility = View.GONE
        bubbleContainer?.visibility = View.VISIBLE
        layoutParams.width = bubbleSizePx
        layoutParams.height = bubbleSizePx
        floatingView?.alpha = 1f
        windowManager?.updateViewLayout(floatingView, layoutParams)
        isBubbleCollapsed = true
    }

    private fun expandFromBubble(bubbleContainer: View?, contentContainer: View?) {
        if (!isBubbleCollapsed) return
        bubbleContainer?.visibility = View.GONE
        contentContainer?.visibility = View.VISIBLE
        layoutParams.width = expandedWidthPx
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        floatingView?.alpha = getSharedPreferences("com.rsps1008.floatingcarrier.PREFERENCE_FILE", Context.MODE_PRIVATE)
            .getInt("expandedOpacity", 100).coerceIn(50, 100) / 100f
        windowManager?.updateViewLayout(floatingView, layoutParams)
        isBubbleCollapsed = false
        isDragging = false
    }

    // 切換條碼顯示/隱藏
    private fun toggleBarcodeVisibility(barcodeImageView: ImageView?, vehicleNumberTextView: TextView?) {
        if (isBarcodeVisible) {
            barcodeImageView?.visibility = View.GONE
            vehicleNumberTextView?.visibility = View.GONE
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            barcodeImageView?.visibility = View.VISIBLE
            vehicleNumberTextView?.visibility = View.VISIBLE
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        windowManager?.updateViewLayout(floatingView, layoutParams)
        isBarcodeVisible = !isBarcodeVisible
        isDragging = false
    }

    override fun onDestroy() {
        super.onDestroy()
        AppRuntimeState.isFloatingServiceRunning = false
        floatingView?.let { windowManager?.removeView(it) }
    }

}
