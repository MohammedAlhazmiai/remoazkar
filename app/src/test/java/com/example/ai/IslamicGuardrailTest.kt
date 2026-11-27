package com.example.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslamicGuardrailTest {

    @Test
    fun religiousQueriesAreAllowed() {
        assertTrue(IslamicGuardrail.isAllowedQuery("ما حكم بخاخ الربو في الصيام؟"))
        assertTrue(IslamicGuardrail.isAllowedQuery("متى يكون سجود السهو قبل السلام وبعده؟"))
        assertTrue(IslamicGuardrail.isAllowedQuery("اكتب خطبة جمعة كاملة عن تقوى الله"))
        assertTrue(IslamicGuardrail.isAllowedQuery("فسر لي حلم رؤية المطر والبحر والكعبة"))
        assertTrue(IslamicGuardrail.isAllowedQuery("صمم لي بطاقة دعوية إسلامية عن يوم الجمعة"))
        assertTrue(IslamicGuardrail.isAllowedQuery("اكتب خاطرة إيمانية عن التوكل واليقين بالله"))
        assertTrue(IslamicGuardrail.isAllowedQuery("السلام عليكم ورحمة الله وبركاته"))
        assertTrue(IslamicGuardrail.isAllowedQuery("جزاك الله خيرًا وبارك فيك"))
        assertTrue(IslamicGuardrail.isAllowedQuery("اليوم تعبان في العمل وودي نسولف شوي"))
        assertTrue(IslamicGuardrail.isAllowedQuery("ماذا يمكنك ان تقدمه لي من مساعدة"))
        assertTrue(IslamicGuardrail.isAllowedQuery("زهقان وطفشان كيف حالك يا صديقي"))
        assertTrue(IslamicGuardrail.isAllowedQuery("رأيت ثعبان يطاردني"))
        assertTrue(IslamicGuardrail.isAllowedQuery("سقوط أسناني في الحلم"))
        assertTrue(IslamicGuardrail.isAllowedQuery("وش حكم المسح على الجوربين"))
        assertTrue(IslamicGuardrail.isAllowedQuery("علامات الساعة الصغرى والكبرى"))
    }

    @Test
    fun fiqhQueriesAboutModernTopicsAreAllowed() {
        // السؤال عن الحكم الشرعي لأمر تقني أو دنيوي مشروع ومطلوب شرعًا
        assertTrue(IslamicGuardrail.isAllowedQuery("ما حكم لعب البلايستيشن في الإسلام؟"))
        assertTrue(IslamicGuardrail.isAllowedQuery("هل يجوز التداول في الفوركس شرعا؟"))
    }

    @Test
    fun secularNonReligiousQueriesArePolitelyRejected() {
        assertFalse(IslamicGuardrail.isAllowedQuery("اكتب كود بايثون لفرز مصفوفة برمجية"))
        assertFalse(IslamicGuardrail.isAllowedQuery("من هو افضل لاعب في دوري ابطال اوروبا ميسي ام رونالدو"))
        assertFalse(IslamicGuardrail.isAllowedQuery("ما هي احدث اخبار فيلم هوليوود ومسلسل نتفلكس"))
        assertFalse(IslamicGuardrail.isAllowedQuery("كيف اخترق شبكة واي فاي او اهكر حساب"))
    }
}
