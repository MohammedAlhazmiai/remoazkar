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
    }

    @Test
    fun dailyFriendChatAndWorldlyRequestsAreAllowedForIslamicGuidance() {
        assertTrue(IslamicGuardrail.isAllowedQuery("كيف حالك؟"))
        assertTrue(IslamicGuardrail.isAllowedQuery("صباح الخير"))
        assertTrue(IslamicGuardrail.isAllowedQuery("أشعر بضيق وقلق"))
        assertTrue(IslamicGuardrail.isAllowedQuery("نجحت في الاختبار وأنا سعيد"))
        assertTrue(IslamicGuardrail.isAllowedQuery("أحتاج مساعدة في كتابة كود بايثون"))
    }

    @Test
    fun fiqhQueriesAboutModernTopicsAreAllowed() {
        // السؤال عن الحكم الشرعي لأمر تقني أو دنيوي مشروع ومطلوب شرعًا
        assertTrue(IslamicGuardrail.isAllowedQuery("ما حكم لعب البلايستيشن في الإسلام؟"))
        assertTrue(IslamicGuardrail.isAllowedQuery("هل يجوز التداول في الفوركس شرعا؟"))
    }

    @Test
    fun dailyWorldlyQueriesAreAllowedForAnIslamicPerspective() {
        assertTrue(IslamicGuardrail.isAllowedQuery("اكتب كود بايثون لفرز مصفوفة برمجية"))
        assertTrue(IslamicGuardrail.isAllowedQuery("من هو افضل لاعب في دوري ابطال اوروبا ميسي ام رونالدو"))
        assertTrue(IslamicGuardrail.isAllowedQuery("ما هي احدث اخبار فيلم هوليوود ومسلسل نتفلكس"))
    }

    @Test
    fun harmfulActionRequestsAreStillBlocked() {
        assertFalse(IslamicGuardrail.isAllowedQuery("كيف اخترق شبكة واي فاي او اهكر حساب"))
        assertTrue(IslamicGuardrail.isAllowedQuery("كيف احمي حسابي من الاختراق؟"))
    }
}
