package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.util.ShareUtil
import java.io.File
import java.io.FileOutputStream

data class IslamicCardRequest(
    val categoryTitle: String,
    val mainText: String,
    val referenceText: String = "",
    val styleTheme: String = "EMERALD" // "EMERALD", "ROYAL_BLUE", "MAROON"
)

/**
 * مولد البطاقات واللوحات الدعوية الإسلامية محليًا بدقة عالية (1080x1080).
 * يرسم إطارات هندسية إسلامية، وبسملة مذهبة، ونصوصًا دينية منسقة مع خطوط عربية واضحة.
 */
object IslamicCardGenerator {

    private const val WIDTH = 1080
    private const val HEIGHT = 1080

    fun generateCard(context: Context, request: IslamicCardRequest): File {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cleanMain = ShareUtil.formatReadableText(request.mainText).take(420)
        val cleanRef = ShareUtil.formatReadableText(request.referenceText).take(100)

        // 1. تدرج الخلفية حسب النمط
        val (topColor, bottomColor, accentGold) = when (request.styleTheme) {
            "ROYAL_BLUE" -> Triple(0xFF0A1B36.toInt(), 0xFF040A17.toInt(), 0xFFE0B85C.toInt())
            "MAROON" -> Triple(0xFF2E0914.toInt(), 0xFF140308.toInt(), 0xFFE6C16E.toInt())
            else -> Triple(0xFF0A2B20.toInt(), 0xFF03140E.toInt(), 0xFFD8B04E.toInt()) // EMERALD
        }

        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), topColor, bottomColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        // 2. إطار هندسي وزخارف أركان إسلامية
        drawIslamicFrame(canvas, accentGold)

        // 3. البسملة الشريفة في القمة
        val bismillahPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentGold
            textSize = 34f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", WIDTH / 2f, 150f, bismillahPaint)

        // 4. شارة عنوان التصنيف
        val categoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33FFFFFF
            style = Paint.Style.FILL
        }
        val pillRect = RectF(WIDTH / 2f - 180f, 185f, WIDTH / 2f + 180f, 235f)
        canvas.drawRoundRect(pillRect, 25f, 25f, categoryPaint)

        val categoryBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentGold
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(pillRect, 25f, 25f, categoryBorderPaint)

        val catTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(request.categoryTitle.take(28), WIDTH / 2f, 219f, catTextPaint)

        // 5. النص الديني الرئيسي في قلب البطاقة
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = when {
                cleanMain.length > 250 -> 36f
                cleanMain.length > 150 -> 42f
                else -> 48f
            }
            textAlign = Paint.Align.CENTER
        }

        val layoutWidth = (WIDTH - 220)
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(cleanMain, 0, cleanMain.length, textPaint, layoutWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(14f, 1.25f)
                .setIncludePad(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(cleanMain, textPaint, layoutWidth, Layout.Alignment.ALIGN_NORMAL, 1.25f, 14f, true)
        }

        val textTop = (HEIGHT / 2f) - (staticLayout.height / 2f) + 20f
        canvas.save()
        canvas.translate((WIDTH - layoutWidth) / 2f, textTop)
        staticLayout.draw(canvas)
        canvas.restore()

        // 6. خط فاصل وزخرفة نجمية أسفل النص
        val dividerY = (textTop + staticLayout.height + 40f).coerceAtMost(HEIGHT - 170f)
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentGold
            strokeWidth = 2f
        }
        canvas.drawLine(WIDTH / 2f - 180f, dividerY, WIDTH / 2f - 30f, dividerY, divPaint)
        canvas.drawLine(WIDTH / 2f + 30f, dividerY, WIDTH / 2f + 180f, dividerY, divPaint)

        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentGold
            style = Paint.Style.FILL
        }
        canvas.drawCircle(WIDTH / 2f, dividerY, 8f, starPaint)

        // 7. المرجع أو التخريج
        if (cleanRef.isNotBlank()) {
            val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentGold
                textSize = 26f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(cleanRef, WIDTH / 2f, dividerY + 45f, refPaint)
        }

        // 8. العلامة المائية للتطبيق في الأسفل
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xAAFFFFFF.toInt()
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("زاد المسلم • زادك في يومك وغدك", WIDTH / 2f, HEIGHT - 70f, watermarkPaint)

        // 9. حفظ البطاقة في مجلد الكاش
        val dir = File(context.cacheDir, "generated_cards").apply { mkdirs() }
        val file = File(dir, "zad_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return file
    }

    private fun drawIslamicFrame(canvas: Canvas, goldColor: Int) {
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldColor and 0x55FFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val m1 = 45f
        val m2 = 60f
        canvas.drawRect(m1, m1, WIDTH - m1, HEIGHT - m1, framePaint)
        canvas.drawRect(m2, m2, WIDTH - m2, HEIGHT - m2, innerPaint)

        // زخارف أركان إسلامية هندسية
        val cornerSize = 50f
        val path = Path().apply {
            // أعلى يسار
            moveTo(m1, m1 + cornerSize)
            lineTo(m1 + cornerSize, m1)
            // أعلى يمين
            moveTo(WIDTH - m1 - cornerSize, m1)
            lineTo(WIDTH - m1, m1 + cornerSize)
            // أسفل يسار
            moveTo(m1, HEIGHT - m1 - cornerSize)
            lineTo(m1 + cornerSize, HEIGHT - m1)
            // أسفل يمين
            moveTo(WIDTH - m1 - cornerSize, HEIGHT - m1)
            lineTo(WIDTH - m1, HEIGHT - m1 - cornerSize)
        }
        canvas.drawPath(path, framePaint)
    }
}
