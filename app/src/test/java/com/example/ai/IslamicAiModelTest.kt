package com.example.ai

import com.example.data.ai.AiChatRepository
import com.example.data.ai.AiMessageEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class IslamicAiModelTest {

    private lateinit var corpus: List<CorpusEntry>

    @Before
    fun setUp() {
        val assetFiles = listOf(
            File("src/main/assets/islamic_ai_corpus.json"),
            File("src/main/assets/islamic_ai_corpus_extra.json")
        ).map { file ->
            if (file.exists()) file else File("app/${file.path}")
        }
        assertTrue("Expected both offline corpus JSON files to exist", assetFiles.all { it.exists() })
        corpus = IslamicCorpusLoader.parseStreams(assetFiles.map { it.inputStream() })
    }

    @Test
    fun corpusCoversAllRequiredDomainsWithoutEmptyFields() {
        val domains = corpus.map { it.domain }.toSet()
        assertTrue(domains.containsAll(listOf("fatwa", "dreams", "letters", "khawatir", "khutbah", "lecture", "chat_qa")))
        assertTrue(corpus.all { it.title.isNotBlank() && it.content.isNotBlank() && it.keywords.isNotEmpty() })
        assertTrue(corpus.size >= 30)
    }

    @Test
    fun intentClassifierDetectsAllCapabilitiesAccurately() {
        assertEquals(IslamicIntent.GREETING, ArabicNlpEngine.detectIntent("السلام عليكم ورحمة الله", false))
        assertEquals(IslamicIntent.FATWA, ArabicNlpEngine.detectIntent("ما حكم بخاخ الربو في الصيام؟", false))
        assertEquals(IslamicIntent.DREAMS, ArabicNlpEngine.detectIntent("حلمت أني أطير فوق البحر", false))
        assertEquals(IslamicIntent.LETTER, ArabicNlpEngine.detectIntent("اكتب رسالة دينية للتهنئة بالعيد", false))
        assertEquals(IslamicIntent.KHATIRA, ArabicNlpEngine.detectIntent("اكتب خاطرة إيمانية عن التوكل", false))
        assertEquals(IslamicIntent.KHUTBAH, ArabicNlpEngine.detectIntent("أعد لي خطبة جمعة عن تقوى الله", false))
        assertEquals(IslamicIntent.LECTURE, ArabicNlpEngine.detectIntent("اكتب محاضرة دعوية عن تزكية النفس", false))
        assertEquals(IslamicIntent.FOLLOW_UP, ArabicNlpEngine.detectIntent("أعطني دليلًا آخر", true))
    }

    @Test
    fun fatwaQueriesReturnVerifiedRulingsAndEvidence() {
        val asthma = IslamicAiModel.generateReply("هل بخاخ الربو وقطرة العين تفطر الصائم؟", corpus)
        assertTrue(asthma.text.contains("لا تفطر") || asthma.text.contains("لا يُفطر"))
        assertTrue(asthma.text.contains("البقرة") || asthma.references.isNotEmpty())

        val sahw = IslamicAiModel.generateReply("متى يكون سجود السهو قبل السلام ومتى بعده؟", corpus)
        assertTrue(sahw.text.contains("قبل السلام") && sahw.text.contains("بعد السلام"))

        val oath = IslamicAiModel.generateReply("ما كفارة اليمين بالله؟", corpus)
        assertTrue(oath.text.contains("عشرة مساكين") && oath.text.contains("ثلاثة أيام"))
    }

    @Test
    fun dreamInterpreterCombinesMultipleSymbolsAndIncludesShariGuidance() {
        val reply = IslamicAiModel.generateReply("حلمت أني أطير تحت المطر ورأيت البحر والكعبة", corpus)
        assertEquals(IslamicIntent.DREAMS.arabicLabel, reply.domainTag)
        assertTrue(reply.text.contains("المطر"))
        assertTrue(reply.text.contains("البحر"))
        assertTrue(reply.text.contains("الطيران"))
        assertTrue(reply.text.contains("الكعبة"))
        assertTrue(reply.text.contains("ابن سيرين") || reply.text.contains("النابلسي"))
        assertTrue(reply.text.contains("تنبيه شرعي"))
    }

    @Test
    fun creativeModesSupportTemplatesAndDynamicTopics() {
        val letter = IslamicAiModel.generateReply("اكتب رسالة تهنئة لوالدي بالعيد", corpus)
        assertEquals(IslamicIntent.LETTER.arabicLabel, letter.domainTag)
        assertTrue(letter.text.contains("السلام عليكم"))

        val khutbah = IslamicAiModel.generateReply("اكتب خطبة جمعة عن الأمانة وحسن الجوار", corpus)
        assertEquals(IslamicIntent.KHUTBAH.arabicLabel, khutbah.domainTag)
        assertTrue(khutbah.text.contains("الخطبة الأولى") && khutbah.text.contains("الخطبة الثانية"))

        val lecture = IslamicAiModel.generateReply("أعد محاضرة عن تربية الأبناء في العصر الرقمي", corpus)
        assertEquals(IslamicIntent.LECTURE.arabicLabel, lecture.domainTag)
        assertTrue(lecture.text.contains("المحور"))

        val apology = IslamicAiModel.generateReply("اكتب رسالة اعتذار لأخي", corpus)
        assertEquals(IslamicIntent.LETTER.arabicLabel, apology.domainTag)
        assertTrue(apology.text.contains("أعتذر") || apology.text.contains("تصفح"))

        val khatira = IslamicAiModel.generateReply("اكتب خاطرة إيمانية عن جبر الخواطر", corpus)
        assertEquals(IslamicIntent.KHATIRA.arabicLabel, khatira.domainTag)
        assertFalse(khatira.text.isBlank())
    }

    @Test
    fun expandedFatwaAndDreamEntriesRespondAccurately() {
        val prayerPillars = IslamicAiModel.generateReply("ما هي شروط الصلاة وأركانها ومبطلاتها؟", corpus)
        assertEquals(IslamicIntent.FATWA.arabicLabel, prayerPillars.domainTag)
        assertTrue(prayerPillars.text.contains("تكبيرة الإحرام") && prayerPillars.text.contains("مبطلات الصلاة"))

        val dreamShoe = IslamicAiModel.generateReply("حلمت بمفتاح يفتح باب ثم ضياع الحذاء", corpus)
        assertEquals(IslamicIntent.DREAMS.arabicLabel, dreamShoe.domainTag)
        assertTrue(dreamShoe.text.contains("المفتاح") && dreamShoe.text.contains("الحذاء"))
    }

    @Test
    fun followUpMaintainsConversationContext() {
        val history = listOf(
            AiMessageEntity(
                conversationId = 1L,
                role = AiChatRepository.ROLE_USER,
                content = "ما حكم زكاة المال وكم النصاب؟"
            ),
            AiMessageEntity(
                conversationId = 1L,
                role = AiChatRepository.ROLE_ASSISTANT,
                content = "زكاة المال تجب عند بلوغ النصاب 85 جرامًا من الذهب وحولان الحول بنسبة 2.5%.",
                categoryTag = "فتوى ومسألة فقهية",
                referencesSummary = "التوبة: 60"
            )
        )

        val summary = IslamicAiModel.generateReply("اختصر الرد", corpus, history)
        assertTrue(summary.text.contains("85 جرامًا") || summary.text.contains("ملخص"))

        val moreEvidence = IslamicAiModel.generateReply("أعطني دليلًا آخر", corpus, history)
        assertTrue(moreEvidence.text.contains("التوبة") || moreEvidence.text.contains("زكاة") || moreEvidence.text.contains("القرآن"))
        assertTrue(moreEvidence.references.isNotEmpty())
    }

    @Test
    fun neuralEmbeddingsAndDialectBridgeHandleColloquialAndAppreciationPrompts() {
        val v1 = NeuralArabicLanguageCore.encodeDenseVector("ما حكم صلاة المسافر والقصر في الطائرة")
        val v2 = NeuralArabicLanguageCore.encodeDenseVector("وش حكم قصر الصلاة في الطياره وقت السفر")
        val unrelated = NeuralArabicLanguageCore.encodeDenseVector("تفسير حلم سقوط الاسنان والذهب")
        val simRelated = NeuralArabicLanguageCore.cosineSimilarity(v1, v2)
        val simUnrelated = NeuralArabicLanguageCore.cosineSimilarity(v1, unrelated)
        assertTrue("Expected dialectal travel prayer vectors to correlate more strongly ($simRelated vs $simUnrelated)", simRelated > simUnrelated)

        val colloquialReply = IslamicAiModel.generateReply("وش حكم بخاخ الربو في الصيام", corpus)
        assertEquals(IslamicIntent.FATWA.arabicLabel, colloquialReply.domainTag)
        assertTrue(colloquialReply.text.contains("لا تفطر") || colloquialReply.text.contains("لا يُفطر"))

        val thanksReply = IslamicAiModel.generateReply("جزاك الله خيرا وبارك فيك", corpus)
        assertEquals(IslamicIntent.APPRECIATION.arabicLabel, thanksReply.domainTag)
        assertTrue(thanksReply.text.contains("وجزاك الله خيرًا"))
    }

    @Test
    fun guardrailInterceptsSecularTaskAndApologizes() {
        val rejected = IslamicAiModel.generateReply("اكتب كود بايثون لفرز مصفوفة ارقام", corpus)
        assertEquals("اعتذار واقتصار شرعي", rejected.domainTag)
        assertTrue(rejected.text.contains("أعتذر منك أخي الكريم، أنا مساعد إسلامي متخصص ومقتصر"))
        assertTrue(rejected.text.contains("لا يمكنني الإجابة أو المساعدة في موضوعات خارج هذا النطاق"))
    }

    @Test
    fun imageDesignIntentRecognizedAndGeneratesSpec() {
        assertEquals(IslamicIntent.IMAGE_DESIGN, ArabicNlpEngine.detectIntent("صمم لي بطاقة دعوية إسلامية عن يوم الجمعة", false))
        val cardReply = IslamicAiModel.generateReply("صمم لي بطاقة دعوية إسلامية عن يوم الجمعة", corpus)
        assertEquals(IslamicIntent.IMAGE_DESIGN.arabicLabel, cardReply.domainTag)
        assertTrue(cardReply.text.contains("تصميم البطاقة الدعوية الإسلامية"))
    }

    @Test
    fun exactDreamSymbolMatchesAndNeverHallucinatesUnrelatedDreams() {
        val slaughterDream = IslamicAiModel.generateReply("تفسير عن حلم ذبح", corpus)
        assertEquals(IslamicIntent.DREAMS.arabicLabel, slaughterDream.domainTag)
        assertTrue("Expected slaughter dream explanation, got: ${slaughterDream.text}", slaughterDream.text.contains("الذبح") || slaughterDream.text.contains("الأضحية"))
        assertFalse("Must NEVER return unrelated flying dream!", slaughterDream.text.contains("الطيران"))

        val greetingPlusDream = IslamicAiModel.generateReply("مرحبا فسر لي حلم ذبح", corpus)
        assertEquals(IslamicIntent.DREAMS.arabicLabel, greetingPlusDream.domainTag)
        assertTrue(greetingPlusDream.text.contains("الذبح"))
        assertFalse(greetingPlusDream.text.contains("الطيران"))

        val unknownDream = IslamicAiModel.generateReply("تفسير حلم كوكب المريخ والنيزك", corpus)
        assertEquals(IslamicIntent.DREAMS.arabicLabel, unknownDream.domainTag)
        assertFalse("Unmatched dream must NEVER return flying dream", unknownDream.text.contains("الطيران"))
        assertTrue(unknownDream.text.contains("المريخ") || unknownDream.text.contains("النيزك") || unknownDream.text.contains("الضوابط"))
    }

    @Test
    fun greetingAndCapabilityAndFriendlyDailyChatRespondAccurately() {
        val hello = IslamicAiModel.generateReply("مرحبا", corpus)
        assertEquals(IslamicIntent.GREETING.arabicLabel, hello.domainTag)
        assertFalse(hello.text.contains("صلاة المسافر"))

        val help = IslamicAiModel.generateReply("ماذا يمكنك ان تقدمه لي من مساعدة", corpus)
        assertEquals(IslamicIntent.GREETING.arabicLabel, help.domainTag)
        assertTrue(help.text.contains("الفتاوى") && help.text.contains("تفسير الرؤى"))
        assertFalse(help.text.contains("صلاة المسافر"))

        val boredChat = IslamicAiModel.generateReply("زهقان وطفشان وأبغى ندردش شوي", corpus)
        assertEquals("دردشة أخوية يومية", boredChat.domainTag)
        assertTrue(boredChat.text.contains("صاحبي") || boredChat.text.contains("ندردش"))

        val workFatigue = IslamicAiModel.generateReply("اليوم تعبان من الشغل والدوام", corpus)
        assertEquals("دردشة أخوية يومية", workFatigue.domainTag)
        assertTrue(workFatigue.text.contains("الشغل") || workFatigue.text.contains("صديقي"))

        val happyNews = IslamicAiModel.generateReply("الحمد لله نجحت في الاختبار وأبشرك", corpus)
        assertEquals("دردشة أخوية يومية", happyNews.domainTag)
        assertTrue(happyNews.text.contains("مبارك") || happyNews.text.contains("فرحت"))
    }

    @Test
    fun portableArabicRegexAndSuspendReplyWorkWithoutLegacyAndroidCrash() = runBlocking {
        val normalized = ArabicNlpEngine.normalize("السَّلَامُ عَلَيْكُمْ! 123 — هل يجوز الصيام؟")
        assertTrue(normalized.contains("السلام عليكم"))
        assertTrue(normalized.contains("123"))
        assertTrue(normalized.contains("هل يجوز الصيام"))

        val suspendReply = IslamicAiModel.generateReplySuspend("مرحبا", corpus)
        assertEquals(IslamicIntent.GREETING.arabicLabel, suspendReply.domainTag)
        assertFalse(suspendReply.text.isBlank())
    }
}
