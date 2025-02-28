package com.example.taiwanbarcodefloating

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlin.math.abs

class FloatingViewService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isBarcodeVisible = true  // 記錄條碼是否可見
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var isDragging = false       // 判斷是否正在拖曳

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
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

            startForeground(1, notification)
        }

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)

        // 取得螢幕寬度
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // 設定懸浮窗寬度為螢幕寬度的80%
        layoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                (screenWidth * 0.8).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                (screenWidth * 0.8).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }

        // 從 SharedPreferences 取得上次位置
        val sharedPref = getSharedPreferences("com.example.taiwanbarcodefloating.PREFERENCE_FILE", Context.MODE_PRIVATE)
        layoutParams.x = sharedPref.getInt("lastXPosition", 0)
        layoutParams.y = sharedPref.getInt("lastYPosition", 100)

        // 加入懸浮視窗
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.addView(floatingView, layoutParams)

        // 設定拖曳功能
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var touchX: Float = 0f
            private var touchY: Float = 0f
            private val dragThreshold = 10  // 拖曳移動門檻

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - touchX
                        val deltaY = event.rawY - touchY
                        if (abs(deltaX) > dragThreshold || abs(deltaY) > dragThreshold) {
                            layoutParams.x = initialX + deltaX.toInt()
                            layoutParams.y = initialY + deltaY.toInt()
                            windowManager?.updateViewLayout(floatingView, layoutParams)
                            isDragging = true
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // 非拖曳則切換條碼顯示/隱藏
                            toggleBarcodeVisibility(
                                floatingView?.findViewById(R.id.barcode_image),
                                floatingView?.findViewById(R.id.vehicle_number_text)
                            )
                        }
                        // 儲存目前位置
                        val editor = sharedPref.edit()
                        editor.putInt("lastXPosition", layoutParams.x)
                        editor.putInt("lastYPosition", layoutParams.y)
                        editor.apply()
                        return true
                    }
                }
                return false
            }
        })

        // 關閉按鈕事件
        val closeBtn = floatingView?.findViewById<ImageView>(R.id.close_btn)
        closeBtn?.setOnClickListener {
            stopSelf()
            val editor = sharedPref.edit()
            editor.putBoolean("isFirstTime", true)
            editor.commit()
            System.exit(0)
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
        val barcodeImageView = floatingView?.findViewById<ImageView>(R.id.barcode_image)
        val vehicleNumberTextView = floatingView?.findViewById<TextView>(R.id.vehicle_number_text)
        val barcodeWidth = (screenWidth * 0.8).toInt()
        val vehicleNumber: String? = sharedPref.getString("vehicleNumber", null)
        val finalVehicleNumber = vehicleNumber ?: "預設載具號碼"
        barcodeImageView?.setImageBitmap(generateBarcode(finalVehicleNumber, barcodeWidth, (screenWidth / 11 * 2)))
        vehicleNumberTextView?.text = finalVehicleNumber

        // "載具" 文字點擊事件（不影響拖曳）
        val textView = floatingView?.findViewById<TextView>(R.id.textView)
        textView?.setOnTouchListener { _, event ->
            floatingView?.onTouchEvent(event) ?: false
        }
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
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager?.removeView(it) }
    }

    // 根據自訂寬高生成條碼
    private fun generateBarcode(content: String, width: Int, height: Int): android.graphics.Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.CODE_39, width, height)
            val bitMatrixWidth = bitMatrix.width

            var startX = 0
            var endX = bitMatrixWidth - 1

            // 找左邊界
            for (x in 0 until bitMatrixWidth) {
                var isBlack = false
                for (y in 0 until height) {
                    if (bitMatrix.get(x, y)) {
                        startX = x
                        isBlack = true
                        break
                    }
                }
                if (isBlack) break
            }

            // 找右邊界
            for (x in bitMatrixWidth - 1 downTo 0) {
                var isBlack = false
                for (y in 0 until height) {
                    if (bitMatrix.get(x, y)) {
                        endX = x
                        isBlack = true
                        break
                    }
                }
                if (isBlack) break
            }

            val croppedWidth = endX - startX + 1
            val whiteBorderWidth = (croppedWidth * 0.03).toInt()
            val finalWidth = croppedWidth + whiteBorderWidth * 2

            val bitmap = android.graphics.Bitmap.createBitmap(finalWidth, height, android.graphics.Bitmap.Config.RGB_565)

            // 填充白邊
            for (x in 0 until finalWidth) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, android.graphics.Color.WHITE)
                }
            }

            // 畫出條碼內容
            for (x in 0 until croppedWidth) {
                for (y in 0 until height) {
                    bitmap.setPixel(x + whiteBorderWidth, y, if (bitMatrix.get(x + startX, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
