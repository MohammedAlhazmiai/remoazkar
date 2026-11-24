package com.example.ui.theme

import android.os.Build
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R

/**
 * مكتبة الخطوط العربية المدعومة في التطبيق.
 *
 * كل الخطوط مرخّصة بترخيص SIL Open Font License (OFL) وتعمل دون اتصال بالإنترنت،
 * وتدعم التشكيل الكامل (الحركات) والعلامات القرآنية.
 */
object AppFonts {

    /** الخط الافتراضي للنظام (سريع وخفيف). */
    val Default: FontFamily = FontFamily.Default

    /** أميري — خط نسخ كلاسيكي أنيق، مناسب للنصوص الطويلة والقرآن. */
    val Amiri: FontFamily = FontFamily(
        Font(R.font.amiri_regular, FontWeight.Normal),
        Font(R.font.amiri_bold, FontWeight.Bold)
    )

    /** شهرزاد الجديد — خط نسخ واضح جدًا مع دعم كامل للتشكيل. */
    val Scheherazade: FontFamily = FontFamily(
        Font(R.font.scheherazade_regular, FontWeight.Normal),
        Font(R.font.scheherazade_bold, FontWeight.Bold)
    )

    /** لطيف — خط نسخ رفيع واقتصادي في المساحة. */
    val Lateef: FontFamily = FontFamily(
        Font(R.font.lateef_regular, FontWeight.Normal)
    )

    /** عارف رقعة — خط رقعة فني للعناوين والشعارات. */
    val ArefRuqaa: FontFamily = FontFamily(
        Font(R.font.arefruqaa_regular, FontWeight.Normal)
    )

    /** نسخ نوتو — على أندرويد 7–9 نستخدم Amiri الثابت تفاديًا لأخطاء الخطوط المتغيرة (fvar). */
    val NotoNaskh: FontFamily = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        FontFamily(Font(R.font.notonaskh, FontWeight.Normal))
    } else {
        Amiri
    }

    /** ريم كوفي — على أندرويد 7–9 نستخدم ArefRuqaa الثابت تفاديًا لأخطاء الخطوط المتغيرة (fvar). */
    val ReemKufi: FontFamily = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        FontFamily(Font(R.font.reemkufi, FontWeight.Normal))
    } else {
        ArefRuqaa
    }

    /** القاهرة — على أندرويد 7–9 نستخدم Scheherazade الثابت تفاديًا لأخطاء الخطوط المتغيرة (fvar). */
    val Cairo: FontFamily = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        FontFamily(Font(R.font.cairo, FontWeight.Normal))
    } else {
        Scheherazade
    }

    /** قائمة الخطوط المتاحة بالترتيب المطابق لمؤشر التخزين. */
    val all: List<FontOption> = listOf(
        FontOption("الافتراضي", "Default", Default),
        FontOption("أميري", "Amiri", Amiri),
        FontOption("شهرزاد", "Scheherazade", Scheherazade),
        FontOption("نسخ نوتو", "Noto Naskh", NotoNaskh),
        FontOption("ريم كوفي", "Reem Kufi", ReemKufi),
        FontOption("القاهرة", "Cairo", Cairo),
        FontOption("لطيف", "Lateef", Lateef),
        FontOption("عارف رقعة", "Aref Ruqaa", ArefRuqaa)
    )

    /** يعيد عائلة الخط حسب المؤشر مع حماية من القيم الخارجة. */
    fun familyFor(index: Int): FontFamily =
        all.getOrNull(index)?.family ?: Default
}

/** خيار خط واحد: اسم عربي، اسم إنجليزي، وعائلة الخط. */
data class FontOption(
    val arabicName: String,
    val englishName: String,
    val family: FontFamily
)
