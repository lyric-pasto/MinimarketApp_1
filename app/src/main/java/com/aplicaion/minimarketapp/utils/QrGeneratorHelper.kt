package com.aplicaion.minimarketapp.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.util.Locale

/**
 * Generador de Códigos QR para pagos con Yape y billeteras digitales.
 */
object QrGeneratorHelper {

    /**
     * Genera un código QR dinámico con el número oficial y el monto exacto a pagar.
     * Compatible con lectores QR de Yape y billeteras interoperables.
     */
    fun generarQrYape(numero: String = "928193824", monto: Double, size: Int = 400): Bitmap? {
        val montoStr = String.format(Locale.US, "%.2f", monto)
        // Formato estándar de cobro Yape / billeteras digitales
        val payload = "https://yape.pe/cobro?phone=$numero&amount=$montoStr&currency=PEN"
        return generarQrBitmap(payload, size)
    }

    /**
     * Genera un Bitmap QR a partir de cualquier contenido de texto.
     */
    fun generarQrBitmap(contenido: String, size: Int = 400): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = MultiFormatWriter().encode(
                contenido,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) Color.parseColor("#3F0071") else Color.WHITE
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
