package com.aplicaion.minimarketapp.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Locale

/**
 * Generador de Códigos QR para pagos con Yape y billeteras digitales del Perú
 * compatible con el estándar oficial interoperable EMVCo (BCRP / Yape / BCP / Plin).
 */
object QrGeneratorHelper {

    private const val YAPE_PRIMARY_COLOR = "#742284" // Color púrpura característico de Yape
    private const val DEFAULT_MERCHANT_NAME = "MINIMARKET OFICIAL"
    private const val DEFAULT_CITY = "LIMA"

    /**
     * Construye un campo EMVCo en formato TLV (Tag-Length-Value)
     */
    private fun emvField(tag: String, value: String): String {
        val len = String.format(Locale.US, "%02d", value.length)
        return "$tag$len$value"
    }

    /**
     * Calcula el checksum CRC16-CCITT (0xFFFF, polinomio 0x1021) requerido por la norma EMVCo QR
     */
    private fun calcularCRC16(data: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021
        val bytes = data.toByteArray(Charsets.ISO_8859_1)

        for (b in bytes) {
            for (i in 0 until 8) {
                val bit = ((b.toInt() shr (7 - i)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) {
                    crc = crc xor polynomial
                }
            }
        }
        crc = crc and 0xFFFF
        return String.format(Locale.US, "%04X", crc)
    }

    /**
     * Genera la cadena de datos (payload) oficial estándar EMVCo para cobro con QR en Perú (Yape / Plin / BCP)
     * compatible con los lectores de las apps bancarias peruanas.
     */
    fun construirPayloadEmvcoYape(
        numeroCelular: String = "928193824",
        monto: Double,
        nombreComercio: String = DEFAULT_MERCHANT_NAME
    ): String {
        val sb = StringBuilder()

        // 00: Payload Format Indicator ("01")
        sb.append(emvField("00", "01"))

        // 01: Point of Initiation Method: "12" = Dynamic QR (incluye monto específico)
        sb.append(emvField("01", "12"))

        // 26: Merchant Account Information - Yape / BCP (Subtags: 00=GUI "pe.yape", 01=Teléfono)
        val cleanPhone = numeroCelular.replace(" ", "").replace("+51", "").trim()
        val subTagGui = emvField("00", "pe.yape")
        val subTagPhone = emvField("01", cleanPhone)
        val tag26Value = "$subTagGui$subTagPhone"
        sb.append(emvField("26", tag26Value))

        // 52: Merchant Category Code (MCC) - 5411 (Grocery Stores, Supermarkets, Minimarkets)
        sb.append(emvField("52", "5411"))

        // 53: Transaction Currency - 604 (PEN - Sol Peruano ISO 4217)
        sb.append(emvField("53", "604"))

        // 54: Transaction Amount
        val montoStr = String.format(Locale.US, "%.2f", monto)
        sb.append(emvField("54", montoStr))

        // 58: Country Code - PE (Perú ISO 3166-1 alpha 2)
        sb.append(emvField("58", "PE"))

        // 59: Merchant Name
        val merchantSanitized = if (nombreComercio.length > 25) nombreComercio.substring(0, 25) else nombreComercio
        sb.append(emvField("59", merchantSanitized.uppercase(Locale.US)))

        // 60: Merchant City
        sb.append(emvField("60", DEFAULT_CITY))

        // 62: Additional Data Field Template (Subtag 01: Bill / Reference number)
        val refNumber = "MINI${System.currentTimeMillis() % 1000000}"
        val subTagRef = emvField("01", refNumber)
        sb.append(emvField("62", subTagRef))

        // 63: CRC16 Calculation
        val preCrc = sb.toString() + "6304"
        val crcCalculado = calcularCRC16(preCrc)

        return "$preCrc$crcCalculado"
    }

    /**
     * Genera un código QR de alta fidelidad con formato EMVCo oficial,
     * diseño nítido y el distintivo logo/isotipo central de Yape.
     */
    fun generarQrYape(
        numero: String = "928193824",
        monto: Double,
        nombreComercio: String = "MINIMARKET OFICIAL",
        size: Int = 450,
        logoCenter: Bitmap? = null
    ): Bitmap? {
        val payload = construirPayloadEmvcoYape(numero, monto, nombreComercio)
        return generarQrBitmapEstilizado(payload, size, logoCenter)
    }

    /**
     * Genera un Bitmap QR nítido con corrección de errores de alto nivel (ErrorCorrectionLevel.H)
     * e integra un isotipo central estilizado de Yape para un acabado 100% auténtico.
     */
    fun generarQrBitmapEstilizado(
        contenido: String,
        size: Int = 450,
        logoCenter: Bitmap? = null
    ): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
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

            val qrColor = Color.parseColor(YAPE_PRIMARY_COLOR)
            val bgColor = Color.WHITE

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) qrColor else bgColor)
                }
            }

            // Dibujar el distintivo centro Yape (Badge o Logo)
            val canvas = Canvas(bitmap)
            val centerSize = (size * 0.22f).toInt()
            val left = (width - centerSize) / 2f
            val top = (height - centerSize) / 2f
            val right = left + centerSize
            val bottom = top + centerSize

            // Fondo blanco protector con borde redondeado para el logo central
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00D1B2") // Color Turquesa/Cyan característico de Yape
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }

            val badgeRect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(badgeRect, 12f, 12f, bgPaint)
            canvas.drawRoundRect(badgeRect, 12f, 12f, borderPaint)

            if (logoCenter != null) {
                val logoPadding = centerSize * 0.12f
                val destLogoRect = RectF(
                    left + logoPadding,
                    top + logoPadding,
                    right - logoPadding,
                    bottom - logoPadding
                )
                canvas.drawBitmap(logoCenter, null, destLogoRect, Paint(Paint.FILTER_BITMAP_FLAG))
            } else {
                // Dibujar monograma Yape con estilo
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor(YAPE_PRIMARY_COLOR)
                    textSize = centerSize * 0.42f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                val textY = top + (centerSize / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
                canvas.drawText("yape", width / 2f, textY, textPaint)
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
