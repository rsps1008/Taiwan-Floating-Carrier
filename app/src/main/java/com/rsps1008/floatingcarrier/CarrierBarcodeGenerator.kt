package com.rsps1008.floatingcarrier

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object CarrierBarcodeGenerator {

    fun generate(content: String, width: Int, height: Int): Bitmap? {
        if (content.isBlank() || width <= 0 || height <= 0) return null

        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.CODE_39, width, height)
            val bitMatrixWidth = bitMatrix.width

            var startX = 0
            var endX = bitMatrixWidth - 1

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
            val whiteBorderWidth = (croppedWidth * 0.03).toInt().coerceAtLeast(4)
            val finalWidth = croppedWidth + whiteBorderWidth * 2

            val bitmap = Bitmap.createBitmap(finalWidth, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)

            for (x in 0 until croppedWidth) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x + whiteBorderWidth,
                        y,
                        if (bitMatrix.get(x + startX, y)) Color.BLACK else Color.WHITE
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
