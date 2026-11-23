package com.example.ai

/**
 * محرك محلي لمطابقة عبارات المحادثة اليومية وردود إسلامية معدّة مسبقًا.
 * ليس نموذجًا لغويًا مدرّبًا أو توليدًا مفتوحًا؛ في الأسئلة اليومية الأخرى يستخدم التطبيق Gemini عند الاتصال
 * ثم يرجع إلى رد محلي عام عند تعذر الشبكة.
 */
object IslamicDailyChatEngine {

    data class DailyChatReply(
        val text: String,
        val domainTag: String,
        val detectedTopic: String,
        val followUps: List<String>
    )

    private val EVERYDAY_CHAT_SIGNALS = listOf(
        "يومي", "اشعر", "انا", "عندي", "صديقي", "صديقتي", "الدوام", "الدراسه", "المدرسه",
        "مشكلتي", "تعبان", "متعب", "مرهق", "مبسوط", "سعيد", "حزين", "قلقان", "متوتر",
        "ملل", "طفشان", "زهقان", "محتار", "فضفض", "تحدث معي", "اريد ان اتحدث", "حياتي", "عائلتي"
    )

    private val WORLDLY_TOPIC_SIGNALS = listOf(
        "مباراه", "رياضه", "ميسي", "رونالدو", "فيلم", "مسلسل", "اغنيه", "لعبه", "بلايستيشن",
        "امتحان", "وظيفه", "راتب", "سفر", "طقس", "سياره", "هاتف", "كمبيوتر", "تقنيه", "تسوق",
        "طبخ", "هوايه", "تنظيم الوقت", "اختيار تخصص", "علاقه", "زواج", "تربيه", "صديق", "صديقه",
        "برمجه", "كود", "بايثون", "رياضيات", "علوم", "اخبار", "عمل", "مال", "تجاره", "هاتف"
    )

    private val RELIGIOUS_QUESTION_SIGNALS = listOf(
        "ما حكم", "هل يجوز", "فتوى", "حكم شرعي", "حكم", "صلاه", "صلاة", "صيام", "صوم", "زكاه",
        "قران", "قرآن", "حديث", "اذكار", "دعاء", "تفسير حلم", "حلم", "سجود", "وضوء",
        "طهاره", "سيره", "سنه", "خطبه", "محاضره", "خاطره ايمانيه", "بطاقه دعويه", "فضل",
        "الله", "رسول", "اسلام", "شرعا", "حديث نبوي", "صدقه", "صوم", "رمضان"
    )

    fun isEverydayChat(query: String): Boolean {
        val norm = ArabicNlpEngine.normalize(query)
        if (norm.isBlank()) return false
        if (RELIGIOUS_QUESTION_SIGNALS.any { norm.contains(it) }) return false
        return matchDailyChat(norm) != null || EVERYDAY_CHAT_SIGNALS.any { norm.contains(it) } ||
            WORLDLY_TOPIC_SIGNALS.any { norm.contains(it) }
    }

    fun offlineFallback(query: String): DailyChatReply? {
        if (!isEverydayChat(query)) return null
        return DailyChatReply(
            text = "فهمت سؤالك: «${query.trim().take(160)}». أستطيع مناقشة هذا الأمر معك من منظور إسلامي يراعي الأمانة والعدل والرحمة والاعتدال، لكن معرفتي المحلية محدودة ولا أملك الأخبار أو الأسعار الآنية دون اتصال. ما الجانب الذي تريد أن نركز عليه؟",
            domainTag = "دردشة يومية ودية",
            detectedTopic = "محادثة شخصية يومية",
            followUps = listOf("أريد أن أحكي لك ما حدث", "ساعدني في ترتيب أفكاري", "أعطني نصيحة إسلامية")
        )
    }

    fun matchDailyChat(query: String): DailyChatReply? {
        val norm = ArabicNlpEngine.normalize(query)

        return when {
            norm.contains("كيف حالك") || norm.contains("كيفك") || norm.contains("كيف اخبارك") ||
                norm.contains("وش اخبارك") || norm.contains("كيف كان يومك") || norm.contains("كيف يومك") ||
                norm == "وانت" || norm == "وانت كيفك" ->
                DailyChatReply(
                    text = "الحمد لله، أنا مساعد رقمي لا أعيش يومًا كالبشر، لكن يسعدني أن أتحدث معك بودّ وبما يوافق القيم الإسلامية. كيف حالك أنت؟ وكيف كان يومك؟",
                    domainTag = "دردشة يومية ودية",
                    detectedTopic = "تحية وسؤال عن الحال",
                    followUps = listOf("كان يومي جميلًا", "أشعر ببعض التعب", "أريد نصيحة لليوم")
                )

            norm.contains("مرحبا") || norm.contains("اهلا") || norm.contains("السلام عليكم") ||
                norm.contains("عرفني بنفسك") || norm.contains("ماذا يمكنك") || norm.contains("ما خدماتك") ||
                norm.contains("ما هي خدماتك") || norm.contains("ما مهامك") || norm.contains("ماذا تقدم") ||
                norm.contains("كيف تستطيع مساعدتي") || norm.contains("كيف تساعدني") ||
                norm.contains("ممكن تساعدني") || norm == "مساعده" || norm.contains("ايش تقدر") ||
                norm.contains("وش تقدر") || norm.contains("ماذا تستطيع") ->
                DailyChatReply(
                    text = "وعليكم السلام ورحمة الله وبركاته، أهلًا بك يا صديقي. أنا مساعد رقمي أرافقك في حديث ودود ذي طابع إسلامي، وأساعدك في الأسئلة الشرعية والأذكار والخواطر والنصيحة النافعة. ما الذي يشغل بالك اليوم؟",
                    domainTag = "ترحيب وتعريف",
                    detectedTopic = "الترحيب والتعريف بالمساعد",
                    followUps = listOf("كيف حالك؟", "أريد نصيحة لليوم", "ما دعاء يشرح الصدر؟")
                )

            (norm.contains("اشعر") || norm.contains("حاسس") || norm.contains("حاسة") ||
                norm.contains("متضايق") || norm.contains("مضايق") || norm.contains("مرهق") ||
                norm.contains("متعب") || norm.contains("حزين") || norm.contains("وحيد") ||
                norm.contains("قلقان")) &&
                listOf("ضيق", "حزن", "هم", "تعب", "قلق", "حيره", "وحده", "ارهاق", "متضايق", "حزين")
                    .any { norm.contains(it) } ->
                DailyChatReply(
                    text = "آسف أنك تمرّ بشعور ثقيل يا صديقي، وأسأل الله أن يشرح صدرك وييسّر أمرك. خذ نفسًا هادئًا، ولا تحمل كل شيء وحدك؛ تحدّث مع شخص تثق به إن استطعت. ومن الدعاء: «اللهم إني أعوذ بك من الهم والحزن». وإن أحببت، أخبرني ما الذي أتعبك وسأصغي إليك.",
                    domainTag = "مواساة ودعم إيماني",
                    detectedTopic = "مواساة عند الضيق والتعب",
                    followUps = listOf("أريد أن أتكلم عما حدث", "أعطني دعاءً للطمأنينة", "اقترح خطوة بسيطة أبدأ بها")
                )

            norm.contains("نجحت") || norm.contains("نجاح") || norm.contains("مبسوط") ||
                norm.contains("سعيد") || norm.contains("فرحان") || norm.contains("خبر جميل") ||
                norm.contains("انجزت") ->
                DailyChatReply(
                    text = "ما شاء الله، أفرح لخبرك الجميل يا صديقي! بارك الله لك فيما أنجزت، واجعل نجاحك عونًا لك على الخير، ولا تنس شكر الله ومن ساندك. ما أجمل شيء أسعدك اليوم؟",
                    domainTag = "دردشة يومية ودية",
                    detectedTopic = "الفرح والإنجاز",
                    followUps = listOf("أنجزت أمرًا كنت أؤجله", "أريد أن أشاركك خبرًا آخر", "كيف أحافظ على حماسي؟")
                )

            norm.contains("ملل") || norm.contains("طفشان") || norm.contains("طفش") ||
                norm.contains("زهقان") || norm.contains("اشعر بالفراغ") ->
                DailyChatReply(
                    text = "أفهمك يا صديقي، أحيانًا يمرّ وقت نشعر فيه بالملل. جرّب خطوة بسيطة: تمشية قصيرة، اتصالًا بمن تحب، قراءة شيء نافع، أو ذكرًا تحبه. أيّها أقرب إلى مزاجك الآن؟",
                    domainTag = "دردشة يومية ودية",
                    detectedTopic = "الملل واقتراح نشاط نافع",
                    followUps = listOf("اقترح لي نشاطًا هادئًا", "أريد شيئًا أفعله مع العائلة", "علمني ذكرًا قصيرًا")
                )

            norm.contains("صباح الخير") || norm.contains("صباح النور") || norm.contains("صباحكم") ->
                DailyChatReply(
                    text = "صباح النور والرضا يا صديقي. أسأل الله أن يجعل يومك مباركًا، ويعينك فيه على ما تحب من الخير. ما خطتك لهذا اليوم؟",
                    domainTag = "دردشة يومية ودية",
                    detectedTopic = "تحية الصباح",
                    followUps = listOf("أريد أن أبدأ يومي بنشاط", "ما أذكار الصباح؟", "لدي يوم مزدحم")
                )

            norm.contains("مساء الخير") || norm.contains("مساء النور") || norm.contains("مسائكم") ->
                DailyChatReply(
                    text = "مساء النور والطمأنينة يا صديقي. أسأل الله أن يجعل ما بقي من يومك خيرًا وراحة. كيف مضى يومك؟",
                    domainTag = "دردشة يومية ودية",
                    detectedTopic = "تحية المساء",
                    followUps = listOf("كان يومي متعبًا", "الحمد لله كان يومًا جميلًا", "ما دعاء المساء؟")
                )

            norm.contains("نصيحه") || norm.contains("عظني") || norm.contains("ذكرني") ||
                norm.contains("محتار") || norm.contains("ماذا افعل") || norm.contains("وش اسوي") ||
                norm.contains("بماذا تنصحني") ->
                DailyChatReply(
                    text = "نصيحتي لك يا صديقي: خذ الأمور خطوة خطوة، واستعن بالله، وابدأ بأيسر عمل نافع تستطيع الاستمرار عليه. وإذا كان الأمر قرارًا مهمًا، فاجمع المعلومات واستشر شخصًا تثق بحكمته. ما الموضوع الذي تريد نصيحة بشأنه؟",
                    domainTag = "نصيحة إيمانية وعملية",
                    detectedTopic = "طلب نصيحة",
                    followUps = listOf("أحتاج نصيحة في تنظيم وقتي", "أنا محتار في قرار", "أريد نصيحة لتقوية علاقتي بالله")
                )

            norm.contains("شكرا") || norm.contains("جزاك الله خيرا") || norm.contains("بارك الله") ->
                DailyChatReply(
                    text = "وإياك يا صديقي، بارك الله فيك وأسعد قلبك. يسعدني أن أكون عونًا لك، وأسأل الله أن ينفعك بما سمعت.",
                    domainTag = "شكر وثناء",
                    detectedTopic = "الرد على الشكر",
                    followUps = listOf("أريد أن أتابع الحديث", "أعطني دعاءً قصيرًا", "ما نصيحتك لليوم؟")
                )

            norm.contains("دعاء قصير") || norm.contains("دعاء") || norm.contains("اريد دعاء") ->
                DailyChatReply(
                    text = "من الدعاء الجامع: «اللهم آتنا في الدنيا حسنة وفي الآخرة حسنة وقنا عذاب النار». أسأل الله أن يشرح صدرك ويكتب لك الخير.",
                    domainTag = "دعاء مأثور",
                    detectedTopic = "دعاء قصير",
                    followUps = listOf("أريد دعاءً للهم", "ما دعاء الاستخارة؟", "أعطني ذكرًا قصيرًا")
                )

            else -> null
        }
    }
}
