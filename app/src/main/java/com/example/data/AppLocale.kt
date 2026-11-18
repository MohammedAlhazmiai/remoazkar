package com.example.data

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.LayoutDirection

/**
 * دعم متعدد اللغات لواجهة التطبيق.
 *
 * النصوص الدينية (القرآن، الأذكار، الأدعية) تبقى بالعربية دائمًا لأنها نصوص شرعية
 * لا تُترجم في سياق الذكر، بينما تُترجم كل عناصر الواجهة والقوائم والإعدادات.
 *
 * التصميم: خريطة مفتاح ← نص لكل لغة، مع رجوع تلقائي إلى الإنجليزية ثم العربية
 * عند غياب أي مفتاح، حتى لا تظهر أي واجهة فارغة أبدًا.
 */
object AppLocale {

    const val ARABIC = "ar"
    const val ENGLISH = "en"

    /** لغة واحدة معروضة في القائمة. */
    data class Language(
        val code: String,
        val nativeName: String,
        val englishName: String,
        val flag: String,
        val rtl: Boolean
    )

    /** اللغات المدعومة — تغطية واسعة لأغلب مناطق العالم. */
    val languages: List<Language> = listOf(
        Language(ARABIC, "العربية", "Arabic", "AR", true),
        Language(ENGLISH, "English", "English", "EN", false),
        Language("fr", "Français", "French", "FR", false),
        Language("es", "Español", "Spanish", "ES", false),
        Language("de", "Deutsch", "German", "DE", false),
        Language("tr", "Türkçe", "Turkish", "TR", false),
        Language("ur", "اردو", "Urdu", "UR", true),
        Language("fa", "فارسی", "Persian", "FA", true),
        Language("id", "Bahasa Indonesia", "Indonesian", "ID", false),
        Language("ms", "Bahasa Melayu", "Malay", "MS", false),
        Language("ru", "Русский", "Russian", "RU", false),
        Language("bn", "বাংলা", "Bengali", "BN", false),
        Language("hi", "हिन्दी", "Hindi", "HI", false),
        Language("pt", "Português", "Portuguese", "PT", false),
        Language("zh", "中文", "Chinese", "ZH", false),
        Language("sw", "Kiswahili", "Swahili", "SW", false)
    )

    fun languageFor(code: String): Language =
        languages.firstOrNull { it.code == code } ?: languages.first()

    fun isRtl(code: String): Boolean = languageFor(code).rtl

    fun layoutDirection(code: String): LayoutDirection =
        if (isRtl(code)) LayoutDirection.Rtl else LayoutDirection.Ltr

    /** كل مفاتيح الواجهة المترجمة — العربية هي الأساس. */
    private val base: Map<String, String> = mapOf(
        "app_name" to "ريمواذكار",
        "menu" to "القائمة",
        "settings" to "الإعدادات",
        "general" to "عام",
        "language" to "اللغة",
        "language_hint" to "اختر لغة واجهة التطبيق",
        "screen_mode" to "وضع الشاشة",
        "day_mode" to "نهاري",
        "night_mode" to "ليلي",
        "auto_mode" to "تلقائي",
        "fonts" to "خطوط النص",
        "font_size" to "حجم النص",
        "small" to "صغير",
        "medium" to "متوسط",
        "large" to "كبير",
        "font_family" to "نوع الخط",
        "tashkeel" to "التشكيل",
        "tashkeel_on" to "إظهار الحركات",
        "tashkeel_off" to "إخفاء الحركات",
        "share_app" to "مشاركة التطبيق",
        "share_hint" to "شارك التطبيق مع أصدقائك",
        "share_generic" to "مشاركة",
        "share_bluetooth" to "بلوتوث",
        "share_whatsapp" to "واتساب",
        "about_app" to "عن التطبيق",
        "developer" to "المطور",
        "support_developer" to "ادعم المطور",
        "telegram_channel" to "قناة المطور على تلجرام",
        "developer_blog" to "مدونة المطور",
        "developer_platform" to "منصة المطور",
        "about_text" to "تطبيق إسلامي شامل للأذكار والقرآن والأدعية، يعمل دون إنترنت، من برمجة وتطوير المطور محمد الحزمي.",
        "smart_reader" to "القارئ الذكي",
        "read_screen" to "اقرأ نص هذه الصفحة",
        "read_clipboard" to "اقرأ ما نسخته",
        "read_selection" to "اقرأ النص المحدد",
        "open_reader" to "افتح القارئ الذكي",
        "stop_reading" to "إيقاف القراءة",
        "quran" to "القرآن الكريم",
        "hisn" to "حصن المسلم",
        "tasbeeh" to "المسبحة",
        "reminders" to "التذكيرات",
        "hijri" to "التقويم الهجري",
        "custom_azkar" to "أذكاري",
        "adiya" to "الأدعية",
        "ruqyah" to "الرقية الشرعية",
        "wird" to "الورد اليومي",
        "spiritual" to "الرفيق الروحي",
        "search" to "البحث",
        "favorites" to "المفضلة",
        "advanced_settings" to "الإعدادات المتقدمة",
        "light_seal" to "بصمة النور",
        "motion_tasbeeh" to "مسبحة الحركة",
        "adaptive_azkar" to "الذكر الآن",
        "exclusive" to "ميزات حصرية",
        "close" to "إغلاق",
        "back" to "رجوع",
        "ok" to "حسنًا",
        "cancel" to "إلغاء",
        "save" to "حفظ",
        "read" to "اقرأ",
        "share" to "مشاركة",
        "copy" to "نسخ",
        "copied" to "تم النسخ"
    )

    private val en: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "Menu", "settings" to "Settings",
        "general" to "General", "language" to "Language",
        "language_hint" to "Choose the app interface language",
        "screen_mode" to "Screen mode", "day_mode" to "Day", "night_mode" to "Night",
        "auto_mode" to "Auto", "fonts" to "Text fonts", "font_size" to "Text size",
        "small" to "Small", "medium" to "Medium", "large" to "Large",
        "font_family" to "Font family", "tashkeel" to "Diacritics",
        "tashkeel_on" to "Show diacritics", "tashkeel_off" to "Hide diacritics",
        "share_app" to "Share the app", "share_hint" to "Share the app with your friends",
        "share_generic" to "Share", "share_bluetooth" to "Bluetooth", "share_whatsapp" to "WhatsApp",
        "about_app" to "About", "developer" to "Developer",
        "support_developer" to "Support the developer",
        "telegram_channel" to "Developer Telegram channel",
        "developer_blog" to "Developer blog", "developer_platform" to "Developer platform",
        "about_text" to "A complete Islamic app for azkar, Quran and supplications, working fully offline. Built by Mohammed Alhazmi.",
        "smart_reader" to "Smart Reader", "read_screen" to "Read this page",
        "read_clipboard" to "Read my clipboard", "read_selection" to "Read selected text",
        "open_reader" to "Open Smart Reader", "stop_reading" to "Stop reading",
        "quran" to "The Quran", "hisn" to "Fortress of the Muslim", "tasbeeh" to "Tasbeeh counter",
        "reminders" to "Reminders", "hijri" to "Hijri calendar", "custom_azkar" to "My azkar",
        "adiya" to "Supplications", "ruqyah" to "Ruqyah", "wird" to "Daily wird",
        "spiritual" to "Spiritual companion", "search" to "Search", "favorites" to "Favorites",
        "advanced_settings" to "Advanced settings", "light_seal" to "Light Seal",
        "motion_tasbeeh" to "Motion Tasbeeh", "adaptive_azkar" to "Zikr Now",
        "exclusive" to "Exclusive features", "close" to "Close", "back" to "Back",
        "ok" to "OK", "cancel" to "Cancel", "save" to "Save", "read" to "Read",
        "share" to "Share", "copy" to "Copy", "copied" to "Copied"
    )

    private val fr: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "Menu", "settings" to "Paramètres",
        "general" to "Général", "language" to "Langue",
        "language_hint" to "Choisissez la langue de l'interface",
        "screen_mode" to "Mode d'écran", "day_mode" to "Jour", "night_mode" to "Nuit",
        "auto_mode" to "Auto", "fonts" to "Polices", "font_size" to "Taille du texte",
        "small" to "Petit", "medium" to "Moyen", "large" to "Grand",
        "font_family" to "Police", "tashkeel" to "Diacritiques",
        "tashkeel_on" to "Afficher les diacritiques", "tashkeel_off" to "Masquer les diacritiques",
        "share_app" to "Partager l'application", "share_hint" to "Partagez avec vos amis",
        "share_generic" to "Partager", "share_bluetooth" to "Bluetooth", "share_whatsapp" to "WhatsApp",
        "about_app" to "À propos", "developer" to "Développeur",
        "support_developer" to "Soutenir le développeur",
        "telegram_channel" to "Chaîne Telegram du développeur",
        "developer_blog" to "Blog du développeur", "developer_platform" to "Plateforme du développeur",
        "about_text" to "Application islamique complète pour les azkar, le Coran et les invocations, entièrement hors ligne. Développée par Mohammed Alhazmi.",
        "smart_reader" to "Lecteur intelligent", "read_screen" to "Lire cette page",
        "read_clipboard" to "Lire le presse-papiers", "read_selection" to "Lire la sélection",
        "open_reader" to "Ouvrir le lecteur", "stop_reading" to "Arrêter",
        "quran" to "Le Coran", "hisn" to "Forteresse du musulman", "tasbeeh" to "Compteur",
        "reminders" to "Rappels", "hijri" to "Calendrier hégirien", "custom_azkar" to "Mes azkar",
        "adiya" to "Invocations", "ruqyah" to "Roqya", "wird" to "Wird quotidien",
        "spiritual" to "Compagnon spirituel", "search" to "Recherche", "favorites" to "Favoris",
        "advanced_settings" to "Paramètres avancés", "light_seal" to "Sceau de lumière",
        "motion_tasbeeh" to "Tasbeeh par mouvement", "adaptive_azkar" to "Zikr maintenant",
        "exclusive" to "Fonctions exclusives", "close" to "Fermer", "back" to "Retour",
        "ok" to "OK", "cancel" to "Annuler", "save" to "Enregistrer", "read" to "Lire",
        "share" to "Partager", "copy" to "Copier", "copied" to "Copié"
    )

    private val es: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "Menú", "settings" to "Ajustes",
        "general" to "General", "language" to "Idioma",
        "language_hint" to "Elige el idioma de la interfaz",
        "screen_mode" to "Modo de pantalla", "day_mode" to "Día", "night_mode" to "Noche",
        "auto_mode" to "Auto", "fonts" to "Fuentes", "font_size" to "Tamaño del texto",
        "small" to "Pequeño", "medium" to "Mediano", "large" to "Grande",
        "font_family" to "Tipo de fuente", "tashkeel" to "Diacríticos",
        "tashkeel_on" to "Mostrar diacríticos", "tashkeel_off" to "Ocultar diacríticos",
        "share_app" to "Compartir la app", "share_hint" to "Comparte con tus amigos",
        "share_generic" to "Compartir", "share_bluetooth" to "Bluetooth", "share_whatsapp" to "WhatsApp",
        "about_app" to "Acerca de", "developer" to "Desarrollador",
        "support_developer" to "Apoyar al desarrollador",
        "telegram_channel" to "Canal de Telegram del desarrollador",
        "developer_blog" to "Blog del desarrollador", "developer_platform" to "Plataforma del desarrollador",
        "about_text" to "Aplicación islámica completa para azkar, Corán y súplicas, totalmente sin conexión. Creada por Mohammed Alhazmi.",
        "smart_reader" to "Lector inteligente", "read_screen" to "Leer esta página",
        "read_clipboard" to "Leer el portapapeles", "read_selection" to "Leer la selección",
        "open_reader" to "Abrir el lector", "stop_reading" to "Detener",
        "quran" to "El Corán", "hisn" to "Fortaleza del musulmán", "tasbeeh" to "Contador",
        "reminders" to "Recordatorios", "hijri" to "Calendario hijri", "custom_azkar" to "Mis azkar",
        "adiya" to "Súplicas", "ruqyah" to "Ruqyah", "wird" to "Wird diario",
        "spiritual" to "Compañero espiritual", "search" to "Buscar", "favorites" to "Favoritos",
        "advanced_settings" to "Ajustes avanzados", "light_seal" to "Sello de luz",
        "motion_tasbeeh" to "Tasbeeh por movimiento", "adaptive_azkar" to "Zikr ahora",
        "exclusive" to "Funciones exclusivas", "close" to "Cerrar", "back" to "Atrás",
        "ok" to "OK", "cancel" to "Cancelar", "save" to "Guardar", "read" to "Leer",
        "share" to "Compartir", "copy" to "Copiar", "copied" to "Copiado"
    )

    private val de: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "Menü", "settings" to "Einstellungen",
        "general" to "Allgemein", "language" to "Sprache",
        "language_hint" to "Sprache der Benutzeroberfläche wählen",
        "screen_mode" to "Bildschirmmodus", "day_mode" to "Tag", "night_mode" to "Nacht",
        "auto_mode" to "Auto", "fonts" to "Schriftarten", "font_size" to "Textgröße",
        "small" to "Klein", "medium" to "Mittel", "large" to "Groß",
        "font_family" to "Schriftart", "tashkeel" to "Diakritika",
        "tashkeel_on" to "Diakritika zeigen", "tashkeel_off" to "Diakritika verbergen",
        "share_app" to "App teilen", "share_hint" to "Teile die App mit Freunden",
        "share_generic" to "Teilen", "share_bluetooth" to "Bluetooth", "share_whatsapp" to "WhatsApp",
        "about_app" to "Über", "developer" to "Entwickler",
        "support_developer" to "Entwickler unterstützen",
        "telegram_channel" to "Telegram-Kanal des Entwicklers",
        "developer_blog" to "Entwickler-Blog", "developer_platform" to "Entwickler-Plattform",
        "about_text" to "Vollständige islamische App für Azkar, Koran und Bittgebete, komplett offline. Entwickelt von Mohammed Alhazmi.",
        "smart_reader" to "Smarter Vorleser", "read_screen" to "Diese Seite vorlesen",
        "read_clipboard" to "Zwischenablage vorlesen", "read_selection" to "Auswahl vorlesen",
        "open_reader" to "Vorleser öffnen", "stop_reading" to "Stoppen",
        "quran" to "Der Koran", "hisn" to "Festung des Muslims", "tasbeeh" to "Zähler",
        "reminders" to "Erinnerungen", "hijri" to "Hijri-Kalender", "custom_azkar" to "Meine Azkar",
        "adiya" to "Bittgebete", "ruqyah" to "Ruqya", "wird" to "Täglicher Wird",
        "spiritual" to "Spiritueller Begleiter", "search" to "Suche", "favorites" to "Favoriten",
        "advanced_settings" to "Erweiterte Einstellungen", "light_seal" to "Lichtsiegel",
        "motion_tasbeeh" to "Bewegungs-Tasbeeh", "adaptive_azkar" to "Zikr jetzt",
        "exclusive" to "Exklusive Funktionen", "close" to "Schließen", "back" to "Zurück",
        "ok" to "OK", "cancel" to "Abbrechen", "save" to "Speichern", "read" to "Vorlesen",
        "share" to "Teilen", "copy" to "Kopieren", "copied" to "Kopiert"
    )

    private val tr: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "Menü", "settings" to "Ayarlar",
        "general" to "Genel", "language" to "Dil",
        "language_hint" to "Uygulama arayüz dilini seçin",
        "screen_mode" to "Ekran modu", "day_mode" to "Gündüz", "night_mode" to "Gece",
        "auto_mode" to "Otomatik", "fonts" to "Yazı tipleri", "font_size" to "Yazı boyutu",
        "small" to "Küçük", "medium" to "Orta", "large" to "Büyük",
        "font_family" to "Yazı tipi", "tashkeel" to "Harekeler",
        "tashkeel_on" to "Harekeleri göster", "tashkeel_off" to "Harekeleri gizle",
        "share_app" to "Uygulamayı paylaş", "share_hint" to "Arkadaşlarınla paylaş",
        "share_generic" to "Paylaş", "share_bluetooth" to "Bluetooth", "share_whatsapp" to "WhatsApp",
        "about_app" to "Hakkında", "developer" to "Geliştirici",
        "support_developer" to "Geliştiriciyi destekle",
        "telegram_channel" to "Geliştiricinin Telegram kanalı",
        "developer_blog" to "Geliştirici blogu", "developer_platform" to "Geliştirici platformu",
        "about_text" to "Zikirler, Kur'an ve dualar için tamamen çevrimdışı çalışan kapsamlı İslami uygulama. Mohammed Alhazmi tarafından geliştirildi.",
        "smart_reader" to "Akıllı Okuyucu", "read_screen" to "Bu sayfayı oku",
        "read_clipboard" to "Panoyu oku", "read_selection" to "Seçimi oku",
        "open_reader" to "Okuyucuyu aç", "stop_reading" to "Durdur",
        "quran" to "Kur'an-ı Kerim", "hisn" to "Müslümanın Kalesi", "tasbeeh" to "Tesbih",
        "reminders" to "Hatırlatmalar", "hijri" to "Hicri takvim", "custom_azkar" to "Zikirlerim",
        "adiya" to "Dualar", "ruqyah" to "Rukye", "wird" to "Günlük vird",
        "spiritual" to "Manevi yol arkadaşı", "search" to "Ara", "favorites" to "Favoriler",
        "advanced_settings" to "Gelişmiş ayarlar", "light_seal" to "Işık Mührü",
        "motion_tasbeeh" to "Hareketli Tesbih", "adaptive_azkar" to "Şimdi zikret",
        "exclusive" to "Özel özellikler", "close" to "Kapat", "back" to "Geri",
        "ok" to "Tamam", "cancel" to "İptal", "save" to "Kaydet", "read" to "Oku",
        "share" to "Paylaş", "copy" to "Kopyala", "copied" to "Kopyalandı"
    )

    private val ur: Map<String, String> = mapOf(
        "app_name" to "ریمو اذکار", "menu" to "مینو", "settings" to "ترتیبات",
        "general" to "عمومی", "language" to "زبان",
        "language_hint" to "ایپ کی زبان منتخب کریں",
        "screen_mode" to "اسکرین موڈ", "day_mode" to "دن", "night_mode" to "رات",
        "auto_mode" to "خودکار", "fonts" to "فونٹس", "font_size" to "متن کا سائز",
        "small" to "چھوٹا", "medium" to "درمیانہ", "large" to "بڑا",
        "font_family" to "فونٹ کی قسم", "tashkeel" to "اعراب",
        "tashkeel_on" to "اعراب دکھائیں", "tashkeel_off" to "اعراب چھپائیں",
        "share_app" to "ایپ شیئر کریں", "share_hint" to "دوستوں کے ساتھ شیئر کریں",
        "share_generic" to "شیئر", "share_bluetooth" to "بلوٹوتھ", "share_whatsapp" to "واٹس ایپ",
        "about_app" to "ایپ کے بارے میں", "developer" to "ڈویلپر",
        "support_developer" to "ڈویلپر کی مدد کریں",
        "telegram_channel" to "ڈویلپر کا ٹیلی گرام چینل",
        "developer_blog" to "ڈویلپر کا بلاگ", "developer_platform" to "ڈویلپر کا پلیٹ فارم",
        "about_text" to "اذکار، قرآن اور دعاؤں کے لیے مکمل اسلامی ایپ، مکمل طور پر آف لائن۔ محمد الحزمی نے بنائی۔",
        "smart_reader" to "سمارٹ ریڈر", "read_screen" to "یہ صفحہ پڑھیں",
        "read_clipboard" to "کلپ بورڈ پڑھیں", "read_selection" to "منتخب متن پڑھیں",
        "open_reader" to "ریڈر کھولیں", "stop_reading" to "روکیں",
        "quran" to "قرآن کریم", "hisn" to "حصن المسلم", "tasbeeh" to "تسبیح",
        "reminders" to "یاد دہانیاں", "hijri" to "ہجری کیلنڈر", "custom_azkar" to "میرے اذکار",
        "adiya" to "دعائیں", "ruqyah" to "رقیہ", "wird" to "روزانہ ورد",
        "spiritual" to "روحانی ساتھی", "search" to "تلاش", "favorites" to "پسندیدہ",
        "advanced_settings" to "اعلیٰ ترتیبات", "light_seal" to "نور کی مہر",
        "motion_tasbeeh" to "حرکت تسبیح", "adaptive_azkar" to "ابھی ذکر کریں",
        "exclusive" to "خصوصی خصوصیات", "close" to "بند کریں", "back" to "واپس",
        "ok" to "ٹھیک ہے", "cancel" to "منسوخ", "save" to "محفوظ", "read" to "پڑھیں",
        "share" to "شیئر", "copy" to "کاپی", "copied" to "کاپی ہو گیا"
    )

    private val fa: Map<String, String> = mapOf(
        "app_name" to "ریمو اذکار", "menu" to "منو", "settings" to "تنظیمات",
        "general" to "عمومی", "language" to "زبان",
        "language_hint" to "زبان رابط برنامه را انتخاب کنید",
        "screen_mode" to "حالت صفحه", "day_mode" to "روز", "night_mode" to "شب",
        "auto_mode" to "خودکار", "fonts" to "قلم‌ها", "font_size" to "اندازه متن",
        "small" to "کوچک", "medium" to "متوسط", "large" to "بزرگ",
        "font_family" to "نوع قلم", "tashkeel" to "اعراب",
        "tashkeel_on" to "نمایش اعراب", "tashkeel_off" to "پنهان کردن اعراب",
        "share_app" to "اشتراک برنامه", "share_hint" to "با دوستانتان به اشتراک بگذارید",
        "share_generic" to "اشتراک", "share_bluetooth" to "بلوتوث", "share_whatsapp" to "واتساپ",
        "about_app" to "درباره", "developer" to "توسعه‌دهنده",
        "support_developer" to "حمایت از توسعه‌دهنده",
        "telegram_channel" to "کانال تلگرام توسعه‌دهنده",
        "developer_blog" to "وبلاگ توسعه‌دهنده", "developer_platform" to "پلتفرم توسعه‌دهنده",
        "about_text" to "برنامه اسلامی جامع برای اذکار، قرآن و دعاها، کاملاً آفلاین. ساخته محمد الحزمی.",
        "smart_reader" to "خواننده هوشمند", "read_screen" to "خواندن این صفحه",
        "read_clipboard" to "خواندن کلیپ‌بورد", "read_selection" to "خواندن متن انتخابی",
        "open_reader" to "باز کردن خواننده", "stop_reading" to "توقف",
        "quran" to "قرآن کریم", "hisn" to "حصن المسلم", "tasbeeh" to "تسبیح",
        "reminders" to "یادآوری‌ها", "hijri" to "تقویم هجری", "custom_azkar" to "اذکار من",
        "adiya" to "دعاها", "ruqyah" to "رقیه", "wird" to "ورد روزانه",
        "spiritual" to "همراه معنوی", "search" to "جستجو", "favorites" to "موردعلاقه",
        "advanced_settings" to "تنظیمات پیشرفته", "light_seal" to "مهر نور",
        "motion_tasbeeh" to "تسبیح حرکتی", "adaptive_azkar" to "اکنون ذکر کن",
        "exclusive" to "ویژگی‌های ویژه", "close" to "بستن", "back" to "بازگشت",
        "ok" to "باشه", "cancel" to "لغو", "save" to "ذخیره", "read" to "خواندن",
        "share" to "اشتراک", "copy" to "کپی", "copied" to "کپی شد"
    )

    private val id: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "Menu", "settings" to "Pengaturan",
        "general" to "Umum", "language" to "Bahasa",
        "language_hint" to "Pilih bahasa antarmuka aplikasi",
        "screen_mode" to "Mode layar", "day_mode" to "Siang", "night_mode" to "Malam",
        "auto_mode" to "Otomatis", "fonts" to "Font", "font_size" to "Ukuran teks",
        "small" to "Kecil", "medium" to "Sedang", "large" to "Besar",
        "font_family" to "Jenis font", "tashkeel" to "Harakat",
        "tashkeel_on" to "Tampilkan harakat", "tashkeel_off" to "Sembunyikan harakat",
        "share_app" to "Bagikan aplikasi", "share_hint" to "Bagikan dengan teman",
        "share_generic" to "Bagikan", "share_bluetooth" to "Bluetooth", "share_whatsapp" to "WhatsApp",
        "about_app" to "Tentang", "developer" to "Pengembang",
        "support_developer" to "Dukung pengembang",
        "telegram_channel" to "Kanal Telegram pengembang",
        "developer_blog" to "Blog pengembang", "developer_platform" to "Platform pengembang",
        "about_text" to "Aplikasi Islami lengkap untuk azkar, Al-Qur'an dan doa, sepenuhnya offline. Dibuat oleh Mohammed Alhazmi.",
        "smart_reader" to "Pembaca Cerdas", "read_screen" to "Bacakan halaman ini",
        "read_clipboard" to "Bacakan papan klip", "read_selection" to "Bacakan teks terpilih",
        "open_reader" to "Buka pembaca", "stop_reading" to "Berhenti",
        "quran" to "Al-Qur'an", "hisn" to "Benteng Muslim", "tasbeeh" to "Tasbih",
        "reminders" to "Pengingat", "hijri" to "Kalender Hijriah", "custom_azkar" to "Azkar saya",
        "adiya" to "Doa", "ruqyah" to "Ruqyah", "wird" to "Wird harian",
        "spiritual" to "Sahabat spiritual", "search" to "Cari", "favorites" to "Favorit",
        "advanced_settings" to "Pengaturan lanjutan", "light_seal" to "Segel Cahaya",
        "motion_tasbeeh" to "Tasbih Gerak", "adaptive_azkar" to "Zikir Sekarang",
        "exclusive" to "Fitur eksklusif", "close" to "Tutup", "back" to "Kembali",
        "ok" to "OK", "cancel" to "Batal", "save" to "Simpan", "read" to "Baca",
        "share" to "Bagikan", "copy" to "Salin", "copied" to "Tersalin"
    )

    private val ms: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "Menu", "settings" to "Tetapan",
        "general" to "Umum", "language" to "Bahasa",
        "language_hint" to "Pilih bahasa antara muka aplikasi",
        "screen_mode" to "Mod skrin", "day_mode" to "Siang", "night_mode" to "Malam",
        "auto_mode" to "Auto", "fonts" to "Fon", "font_size" to "Saiz teks",
        "small" to "Kecil", "medium" to "Sederhana", "large" to "Besar",
        "font_family" to "Jenis fon", "tashkeel" to "Baris",
        "tashkeel_on" to "Tunjuk baris", "tashkeel_off" to "Sembunyi baris",
        "share_app" to "Kongsi aplikasi", "share_hint" to "Kongsi dengan rakan",
        "share_generic" to "Kongsi", "share_bluetooth" to "Bluetooth", "share_whatsapp" to "WhatsApp",
        "about_app" to "Tentang", "developer" to "Pembangun",
        "support_developer" to "Sokong pembangun",
        "telegram_channel" to "Saluran Telegram pembangun",
        "developer_blog" to "Blog pembangun", "developer_platform" to "Platform pembangun",
        "about_text" to "Aplikasi Islam lengkap untuk azkar, Al-Quran dan doa, berfungsi sepenuhnya luar talian. Dibina oleh Mohammed Alhazmi.",
        "smart_reader" to "Pembaca Pintar", "read_screen" to "Baca halaman ini",
        "read_clipboard" to "Baca papan keratan", "read_selection" to "Baca teks dipilih",
        "open_reader" to "Buka pembaca", "stop_reading" to "Berhenti",
        "quran" to "Al-Quran", "hisn" to "Benteng Muslim", "tasbeeh" to "Tasbih",
        "reminders" to "Peringatan", "hijri" to "Kalendar Hijrah", "custom_azkar" to "Azkar saya",
        "adiya" to "Doa", "ruqyah" to "Ruqyah", "wird" to "Wird harian",
        "spiritual" to "Sahabat rohani", "search" to "Cari", "favorites" to "Kegemaran",
        "advanced_settings" to "Tetapan lanjutan", "light_seal" to "Meterai Cahaya",
        "motion_tasbeeh" to "Tasbih Gerakan", "adaptive_azkar" to "Zikir Sekarang",
        "exclusive" to "Ciri eksklusif", "close" to "Tutup", "back" to "Kembali",
        "ok" to "OK", "cancel" to "Batal", "save" to "Simpan", "read" to "Baca",
        "share" to "Kongsi", "copy" to "Salin", "copied" to "Disalin"
    )

    private val ru: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "Меню", "settings" to "Настройки",
        "general" to "Общие", "language" to "Язык",
        "language_hint" to "Выберите язык интерфейса",
        "screen_mode" to "Режим экрана", "day_mode" to "День", "night_mode" to "Ночь",
        "auto_mode" to "Авто", "fonts" to "Шрифты", "font_size" to "Размер текста",
        "small" to "Маленький", "medium" to "Средний", "large" to "Большой",
        "font_family" to "Шрифт", "tashkeel" to "Огласовки",
        "tashkeel_on" to "Показать огласовки", "tashkeel_off" to "Скрыть огласовки",
        "share_app" to "Поделиться приложением", "share_hint" to "Поделитесь с друзьями",
        "share_generic" to "Поделиться", "share_bluetooth" to "Bluetooth", "share_whatsapp" to "WhatsApp",
        "about_app" to "О приложении", "developer" to "Разработчик",
        "support_developer" to "Поддержать разработчика",
        "telegram_channel" to "Telegram-канал разработчика",
        "developer_blog" to "Блог разработчика", "developer_platform" to "Платформа разработчика",
        "about_text" to "Полное исламское приложение для азкаров, Корана и дуа, работает полностью офлайн. Создано Mohammed Alhazmi.",
        "smart_reader" to "Умный чтец", "read_screen" to "Прочитать эту страницу",
        "read_clipboard" to "Прочитать буфер обмена", "read_selection" to "Прочитать выделенное",
        "open_reader" to "Открыть чтеца", "stop_reading" to "Остановить",
        "quran" to "Коран", "hisn" to "Крепость мусульманина", "tasbeeh" to "Тасбих",
        "reminders" to "Напоминания", "hijri" to "Хиджрийский календарь", "custom_azkar" to "Мои азкары",
        "adiya" to "Дуа", "ruqyah" to "Рукья", "wird" to "Ежедневный вирд",
        "spiritual" to "Духовный спутник", "search" to "Поиск", "favorites" to "Избранное",
        "advanced_settings" to "Дополнительные настройки", "light_seal" to "Печать света",
        "motion_tasbeeh" to "Тасбих движением", "adaptive_azkar" to "Зикр сейчас",
        "exclusive" to "Эксклюзивные функции", "close" to "Закрыть", "back" to "Назад",
        "ok" to "ОК", "cancel" to "Отмена", "save" to "Сохранить", "read" to "Читать",
        "share" to "Поделиться", "copy" to "Копировать", "copied" to "Скопировано"
    )

    private val bn: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "মেনু", "settings" to "সেটিংস",
        "general" to "সাধারণ", "language" to "ভাষা",
        "language_hint" to "অ্যাপের ভাষা নির্বাচন করুন",
        "screen_mode" to "স্ক্রিন মোড", "day_mode" to "দিন", "night_mode" to "রাত",
        "auto_mode" to "স্বয়ংক্রিয়", "fonts" to "ফন্ট", "font_size" to "লেখার আকার",
        "small" to "ছোট", "medium" to "মাঝারি", "large" to "বড়",
        "font_family" to "ফন্টের ধরন", "tashkeel" to "হারাকাত",
        "tashkeel_on" to "হারাকাত দেখান", "tashkeel_off" to "হারাকাত লুকান",
        "share_app" to "অ্যাপ শেয়ার করুন", "share_hint" to "বন্ধুদের সাথে শেয়ার করুন",
        "share_generic" to "শেয়ার", "share_bluetooth" to "ব্লুটুথ", "share_whatsapp" to "হোয়াটসঅ্যাপ",
        "about_app" to "সম্পর্কে", "developer" to "ডেভেলপার",
        "support_developer" to "ডেভেলপারকে সহায়তা করুন",
        "telegram_channel" to "ডেভেলপারের টেলিগ্রাম চ্যানেল",
        "developer_blog" to "ডেভেলপারের ব্লগ", "developer_platform" to "ডেভেলপারের প্ল্যাটফর্ম",
        "about_text" to "আযকার, কুরআন ও দুআর জন্য সম্পূর্ণ ইসলামিক অ্যাপ, সম্পূর্ণ অফলাইন। Mohammed Alhazmi তৈরি করেছেন।",
        "smart_reader" to "স্মার্ট রিডার", "read_screen" to "এই পৃষ্ঠা পড়ুন",
        "read_clipboard" to "ক্লিপবোর্ড পড়ুন", "read_selection" to "নির্বাচিত লেখা পড়ুন",
        "open_reader" to "রিডার খুলুন", "stop_reading" to "থামুন",
        "quran" to "আল-কুরআন", "hisn" to "মুসলিমের দুর্গ", "tasbeeh" to "তাসবিহ",
        "reminders" to "স্মরণিকা", "hijri" to "হিজরি ক্যালেন্ডার", "custom_azkar" to "আমার আযকার",
        "adiya" to "দুআ", "ruqyah" to "রুকইয়া", "wird" to "দৈনিক ওয়ার্দ",
        "spiritual" to "আধ্যাত্মিক সঙ্গী", "search" to "খুঁজুন", "favorites" to "প্রিয়",
        "advanced_settings" to "উন্নত সেটিংস", "light_seal" to "আলোর সীল",
        "motion_tasbeeh" to "গতির তাসবিহ", "adaptive_azkar" to "এখন যিকির করুন",
        "exclusive" to "বিশেষ বৈশিষ্ট্য", "close" to "বন্ধ", "back" to "ফিরে যান",
        "ok" to "ঠিক আছে", "cancel" to "বাতিল", "save" to "সংরক্ষণ", "read" to "পড়ুন",
        "share" to "শেয়ার", "copy" to "কপি", "copied" to "কপি হয়েছে"
    )

    private val hi: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "मेनू", "settings" to "सेटिंग्स",
        "general" to "सामान्य", "language" to "भाषा",
        "language_hint" to "ऐप की भाषा चुनें",
        "screen_mode" to "स्क्रीन मोड", "day_mode" to "दिन", "night_mode" to "रात",
        "auto_mode" to "स्वचालित", "fonts" to "फ़ॉन्ट", "font_size" to "पाठ का आकार",
        "small" to "छोटा", "medium" to "मध्यम", "large" to "बड़ा",
        "font_family" to "फ़ॉन्ट प्रकार", "tashkeel" to "हरकत",
        "tashkeel_on" to "हरकत दिखाएँ", "tashkeel_off" to "हरकत छिपाएँ",
        "share_app" to "ऐप साझा करें", "share_hint" to "दोस्तों के साथ साझा करें",
        "share_generic" to "साझा करें", "share_bluetooth" to "ब्लूटूथ", "share_whatsapp" to "व्हाट्सएप",
        "about_app" to "परिचय", "developer" to "डेवलपर",
        "support_developer" to "डेवलपर का समर्थन करें",
        "telegram_channel" to "डेवलपर का टेलीग्राम चैनल",
        "developer_blog" to "डेवलपर ब्लॉग", "developer_platform" to "डेवलपर प्लेटफ़ॉर्म",
        "about_text" to "अज़कार, क़ुरआन और दुआओं के लिए पूर्ण इस्लामी ऐप, पूरी तरह ऑफ़लाइन। Mohammed Alhazmi द्वारा निर्मित।",
        "smart_reader" to "स्मार्ट रीडर", "read_screen" to "यह पृष्ठ पढ़ें",
        "read_clipboard" to "क्लिपबोर्ड पढ़ें", "read_selection" to "चयनित पाठ पढ़ें",
        "open_reader" to "रीडर खोलें", "stop_reading" to "रोकें",
        "quran" to "क़ुरआन", "hisn" to "मुस्लिम का किला", "tasbeeh" to "तस्बीह",
        "reminders" to "अनुस्मारक", "hijri" to "हिजरी कैलेंडर", "custom_azkar" to "मेरे अज़कार",
        "adiya" to "दुआएँ", "ruqyah" to "रुक़्या", "wird" to "दैनिक विर्द",
        "spiritual" to "आध्यात्मिक साथी", "search" to "खोजें", "favorites" to "पसंदीदा",
        "advanced_settings" to "उन्नत सेटिंग्स", "light_seal" to "प्रकाश मुद्रा",
        "motion_tasbeeh" to "गति तस्बीह", "adaptive_azkar" to "अभी ज़िक्र करें",
        "exclusive" to "विशेष सुविधाएँ", "close" to "बंद करें", "back" to "वापस",
        "ok" to "ठीक", "cancel" to "रद्द", "save" to "सहेजें", "read" to "पढ़ें",
        "share" to "साझा", "copy" to "कॉपी", "copied" to "कॉपी हुआ"
    )

    private val pt: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "Menu", "settings" to "Configurações",
        "general" to "Geral", "language" to "Idioma",
        "language_hint" to "Escolha o idioma da interface",
        "screen_mode" to "Modo de tela", "day_mode" to "Dia", "night_mode" to "Noite",
        "auto_mode" to "Auto", "fonts" to "Fontes", "font_size" to "Tamanho do texto",
        "small" to "Pequeno", "medium" to "Médio", "large" to "Grande",
        "font_family" to "Tipo de fonte", "tashkeel" to "Diacríticos",
        "tashkeel_on" to "Mostrar diacríticos", "tashkeel_off" to "Ocultar diacríticos",
        "share_app" to "Compartilhar o app", "share_hint" to "Compartilhe com amigos",
        "share_generic" to "Compartilhar", "share_bluetooth" to "Bluetooth", "share_whatsapp" to "WhatsApp",
        "about_app" to "Sobre", "developer" to "Desenvolvedor",
        "support_developer" to "Apoiar o desenvolvedor",
        "telegram_channel" to "Canal Telegram do desenvolvedor",
        "developer_blog" to "Blog do desenvolvedor", "developer_platform" to "Plataforma do desenvolvedor",
        "about_text" to "Aplicativo islâmico completo para azkar, Alcorão e súplicas, totalmente offline. Criado por Mohammed Alhazmi.",
        "smart_reader" to "Leitor Inteligente", "read_screen" to "Ler esta página",
        "read_clipboard" to "Ler a área de transferência", "read_selection" to "Ler a seleção",
        "open_reader" to "Abrir o leitor", "stop_reading" to "Parar",
        "quran" to "O Alcorão", "hisn" to "Fortaleza do muçulmano", "tasbeeh" to "Contador",
        "reminders" to "Lembretes", "hijri" to "Calendário hijri", "custom_azkar" to "Meus azkar",
        "adiya" to "Súplicas", "ruqyah" to "Ruqyah", "wird" to "Wird diário",
        "spiritual" to "Companheiro espiritual", "search" to "Pesquisar", "favorites" to "Favoritos",
        "advanced_settings" to "Configurações avançadas", "light_seal" to "Selo de Luz",
        "motion_tasbeeh" to "Tasbeeh por movimento", "adaptive_azkar" to "Zikr agora",
        "exclusive" to "Recursos exclusivos", "close" to "Fechar", "back" to "Voltar",
        "ok" to "OK", "cancel" to "Cancelar", "save" to "Salvar", "read" to "Ler",
        "share" to "Compartilhar", "copy" to "Copiar", "copied" to "Copiado"
    )

    private val zh: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "菜单", "settings" to "设置",
        "general" to "常规", "language" to "语言",
        "language_hint" to "选择应用界面语言",
        "screen_mode" to "屏幕模式", "day_mode" to "白天", "night_mode" to "夜间",
        "auto_mode" to "自动", "fonts" to "字体", "font_size" to "文字大小",
        "small" to "小", "medium" to "中", "large" to "大",
        "font_family" to "字体类型", "tashkeel" to "发音符号",
        "tashkeel_on" to "显示发音符号", "tashkeel_off" to "隐藏发音符号",
        "share_app" to "分享应用", "share_hint" to "与朋友分享",
        "share_generic" to "分享", "share_bluetooth" to "蓝牙", "share_whatsapp" to "WhatsApp",
        "about_app" to "关于", "developer" to "开发者",
        "support_developer" to "支持开发者",
        "telegram_channel" to "开发者 Telegram 频道",
        "developer_blog" to "开发者博客", "developer_platform" to "开发者平台",
        "about_text" to "完整的伊斯兰应用，包含赞念、古兰经和祈祷，完全离线运行。由 Mohammed Alhazmi 开发。",
        "smart_reader" to "智能朗读", "read_screen" to "朗读此页面",
        "read_clipboard" to "朗读剪贴板", "read_selection" to "朗读选中文字",
        "open_reader" to "打开朗读器", "stop_reading" to "停止",
        "quran" to "古兰经", "hisn" to "穆斯林堡垒", "tasbeeh" to "念珠",
        "reminders" to "提醒", "hijri" to "伊斯兰历", "custom_azkar" to "我的赞念",
        "adiya" to "祈祷", "ruqyah" to "鲁格叶", "wird" to "每日功课",
        "spiritual" to "精神伴侣", "search" to "搜索", "favorites" to "收藏",
        "advanced_settings" to "高级设置", "light_seal" to "光之印记",
        "motion_tasbeeh" to "动作念珠", "adaptive_azkar" to "现在赞念",
        "exclusive" to "专属功能", "close" to "关闭", "back" to "返回",
        "ok" to "确定", "cancel" to "取消", "save" to "保存", "read" to "朗读",
        "share" to "分享", "copy" to "复制", "copied" to "已复制"
    )

    private val sw: Map<String, String> = mapOf(
        "app_name" to "Remo Azkar", "menu" to "Menyu", "settings" to "Mipangilio",
        "general" to "Jumla", "language" to "Lugha",
        "language_hint" to "Chagua lugha ya kiolesura",
        "screen_mode" to "Hali ya skrini", "day_mode" to "Mchana", "night_mode" to "Usiku",
        "auto_mode" to "Otomatiki", "fonts" to "Fonti", "font_size" to "Ukubwa wa maandishi",
        "small" to "Ndogo", "medium" to "Wastani", "large" to "Kubwa",
        "font_family" to "Aina ya fonti", "tashkeel" to "Alama za vokali",
        "tashkeel_on" to "Onyesha alama", "tashkeel_off" to "Ficha alama",
        "share_app" to "Shiriki programu", "share_hint" to "Shiriki na marafiki",
        "share_generic" to "Shiriki", "share_bluetooth" to "Bluetooth", "share_whatsapp" to "WhatsApp",
        "about_app" to "Kuhusu", "developer" to "Msanidi",
        "support_developer" to "Saidia msanidi",
        "telegram_channel" to "Kituo cha Telegram cha msanidi",
        "developer_blog" to "Blogu ya msanidi", "developer_platform" to "Jukwaa la msanidi",
        "about_text" to "Programu kamili ya Kiislamu kwa azkar, Qur'an na dua, inafanya kazi bila mtandao. Imeundwa na Mohammed Alhazmi.",
        "smart_reader" to "Msomaji Mahiri", "read_screen" to "Soma ukurasa huu",
        "read_clipboard" to "Soma ubao wa kunakili", "read_selection" to "Soma maandishi yaliyochaguliwa",
        "open_reader" to "Fungua msomaji", "stop_reading" to "Simamisha",
        "quran" to "Qur'an Tukufu", "hisn" to "Ngome ya Muislamu", "tasbeeh" to "Tasbih",
        "reminders" to "Vikumbusho", "hijri" to "Kalenda ya Hijri", "custom_azkar" to "Azkar zangu",
        "adiya" to "Dua", "ruqyah" to "Ruqyah", "wird" to "Wird ya kila siku",
        "spiritual" to "Mwenza wa kiroho", "search" to "Tafuta", "favorites" to "Vipendwa",
        "advanced_settings" to "Mipangilio ya juu", "light_seal" to "Muhuri wa Nuru",
        "motion_tasbeeh" to "Tasbih ya Mwendo", "adaptive_azkar" to "Zikr Sasa",
        "exclusive" to "Vipengele maalum", "close" to "Funga", "back" to "Rudi",
        "ok" to "Sawa", "cancel" to "Ghairi", "save" to "Hifadhi", "read" to "Soma",
        "share" to "Shiriki", "copy" to "Nakili", "copied" to "Imenakiliwa"
    )

    private val translations: Map<String, Map<String, String>> = mapOf(
        ARABIC to base,
        ENGLISH to en,
        "fr" to fr,
        "es" to es,
        "de" to de,
        "tr" to tr,
        "ur" to ur,
        "fa" to fa,
        "id" to id,
        "ms" to ms,
        "ru" to ru,
        "bn" to bn,
        "hi" to hi,
        "pt" to pt,
        "zh" to zh,
        "sw" to sw
    )

    /**
     * يعيد نص المفتاح باللغة المطلوبة مع رجوع آمن:
     * اللغة المطلوبة ← الإنجليزية ← العربية ← المفتاح نفسه.
     */
    fun t(language: String, key: String): String =
        translations[language]?.get(key)
            ?: en[key]
            ?: base[key]
            ?: key

    /** يعيد كل نصوص لغة معيّنة مدموجة مع الرجوع الآمن. */
    fun stringsFor(language: String): Map<String, String> {
        val merged = LinkedHashMap<String, String>()
        base.keys.forEach { key -> merged[key] = t(language, key) }
        return merged
    }
}

/** نصوص اللغة الحالية متاحة لأي كومبوزابل عبر هذا المزوّد. */
val LocalAppStrings = staticCompositionLocalOf<Map<String, String>> {
    AppLocale.stringsFor(AppLocale.ARABIC)
}

/** اللغة الحالية (رمز ISO). */
val LocalAppLanguage = staticCompositionLocalOf { AppLocale.ARABIC }

/** اختصار لقراءة نص مترجم داخل كومبوزابل. */
@androidx.compose.runtime.Composable
fun tr(key: String): String = LocalAppStrings.current[key] ?: key

/**
 * حالة خيار التشكيل (الحركات) — يتحكّم بها المستخدم من القائمة الجانبية.
 * عند إيقافها تُعرض النصوص العربية بدون حركات لتسهيل القراءة السريعة.
 */
val LocalTashkeelEnabled = staticCompositionLocalOf { true }

private val TASHKEEL_REGEX = Regex(
    "[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06DC\u06DF-\u06E4\u06E7\u06E8\u06EA-\u06ED]"
)

/** يزيل التشكيل من نص عربي. */
fun stripTashkeel(text: String): String = text.replace(TASHKEEL_REGEX, "")

/** يُطبّق إعداد التشكيل الحالي على نص قبل عرضه. */
@androidx.compose.runtime.Composable
fun tashkeelText(text: String): String =
    if (LocalTashkeelEnabled.current) text else stripTashkeel(text)
