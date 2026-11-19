package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AthkarReminderScheduler
import com.example.data.AppLocale
import com.example.data.HisnRepository
import com.example.data.LocalAppLanguage
import com.example.data.LocalAppStrings
import com.example.data.LocalTashkeelEnabled
import com.example.data.NotificationHelper
import com.example.data.PreferencesManager
import com.example.data.QuranRepository
import com.example.data.ReminderScheduler
import com.example.data.TtsManager
import com.example.ui.components.AppSettingsDrawer
import com.example.ui.components.SmartReaderBus
import com.example.ui.components.SmartReaderFab
import com.example.ui.components.TtsMiniBar
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AdiyaScreen
import com.example.ui.screens.AdvancedSettingsScreen
import com.example.ui.screens.AiVoiceAssistantScreen
import com.example.ui.screens.CustomAzkarScreen
import com.example.ui.screens.DeveloperLinkScreen
import com.example.ui.screens.ExclusiveFeaturesScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HijriScreen
import com.example.ui.screens.HisnChapterScreen
import com.example.ui.screens.HisnScreen
import com.example.ui.screens.KhatmahScreen
import com.example.ui.screens.NamesOfAllahScreen
import com.example.ui.screens.ProphetDuasScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QuranReaderScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.RuqyahScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SpiritualCompanionScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SupportDeveloperScreen
import com.example.ui.screens.TasbeehScreen
import com.example.ui.screens.WirdScreen
import com.example.ui.theme.AppFonts
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ShareUtil
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesManager = PreferencesManager(applicationContext)

        // --- تهيئة المحرّكات الأساسية مرة واحدة فقط ---
        // هذه الميزات مساعدة وليست شرطًا لعرض التطبيق؛ فشل إحداها لا ينبغي أن
        // يمنع إنشاء واجهة Compose أو يترك المستخدم أمام شاشة إغلاق التطبيق.
        runStartupStep("TTS initialization") { TtsManager.init(applicationContext) }
        runStartupStep("notification channels") {
            NotificationHelper.createChannels(applicationContext)
        }
        // scheduleAll يتضمن أيضًا جدولة ميزات الكهف والملك.
        runStartupStep("reminder scheduling") {
            ReminderScheduler.scheduleAll(applicationContext)
        }
        runStartupStep("smart reader history") { SmartReaderBus.load(applicationContext) }

        // تحميل بيانات القرآن وحصن المسلم في الذاكرة (خارج الخيط الرئيسي)
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runStartupStep("Quran asset loading") {
                QuranRepository.ensureLoaded(applicationContext)
            }
            runStartupStep("Hisn asset loading") {
                HisnRepository.ensureLoaded(applicationContext)
            }
        }

        // طلب إذن الإشعارات على أندرويد 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val darkMode by preferencesManager.darkModeFlow.collectAsState(initial = false)
            val favorites by preferencesManager.favoritesFlow.collectAsState(initial = emptySet())
            val speechRate by preferencesManager.speechRateFlow.collectAsState(initial = 1.0f)
            val speechPitch by preferencesManager.speechPitchFlow.collectAsState(initial = 1.0f)
            val notificationSound by preferencesManager.notificationSoundFlow.collectAsState(initial = true)
            val fontSizeIndex by preferencesManager.fontSizeFlow.collectAsState(initial = 1)
            val autoReminders by preferencesManager.autoRemindersFlow.collectAsState(initial = true)
            val reminderInterval by preferencesManager.reminderIntervalFlow.collectAsState(initial = 60)
            val azkarTime by preferencesManager.azkarTimeFlow.collectAsState(initial = 6 to 30)
            val language by preferencesManager.languageFlow.collectAsState(initial = AppLocale.ARABIC)
            val fontFamilyIndex by preferencesManager.fontFamilyFlow.collectAsState(initial = 0)
            val tashkeel by preferencesManager.tashkeelFlow.collectAsState(initial = true)
            val khatmahDays by preferencesManager.khatmahDaysFlow.collectAsState(initial = 30)
            val khatmahStart by preferencesManager.khatmahStartFlow.collectAsState(initial = 0L)
            val khatmahPagesDone by preferencesManager.khatmahPagesDoneFlow.collectAsState(initial = 0)
            val khatmahActive by preferencesManager.khatmahActiveFlow.collectAsState(initial = false)

            // مزامنة إشعارات الأذكار التلقائية (WorkManager + منبّه يومي بوقت يدوي)
            LaunchedEffect(autoReminders, reminderInterval, azkarTime) {
                try {
                    AthkarReminderScheduler.sync(
                        this@MainActivity,
                        autoReminders,
                        reminderInterval,
                        azkarTime.first,
                        azkarTime.second
                    )
                } catch (error: Exception) {
                    android.util.Log.e(
                        "RemoAzkarStartup",
                        "Reminder synchronization failed; continuing without reminders",
                        error
                    )
                }
            }

            // مزامنة إعدادات الصوت مع محرّك القراءة
            LaunchedEffect(speechRate, speechPitch) {
                try {
                    TtsManager.setParams(speechRate, speechPitch)
                } catch (error: Exception) {
                    android.util.Log.e("RemoAzkarStartup", "Unable to apply TTS settings", error)
                }
            }

            // مزامنة إعداد صوت الإشعارات مع المرآة المتزامنة
            LaunchedEffect(notificationSound) {
                try {
                    PreferencesManager.mirrorNotificationSound(this@MainActivity, notificationSound)
                } catch (error: Exception) {
                    android.util.Log.e("RemoAzkarStartup", "Unable to mirror notification setting", error)
                }
            }

            CompositionLocalProvider(
                LocalAppStrings provides AppLocale.stringsFor(language),
                LocalAppLanguage provides language,
                LocalTashkeelEnabled provides tashkeel,
                LocalLayoutDirection provides AppLocale.layoutDirection(language)
            ) {
                MyApplicationTheme(
                    darkTheme = darkMode,
                    fontFamily = AppFonts.familyFor(fontFamilyIndex)
                ) {
                    AppRoot(
                        preferencesManager = preferencesManager,
                        favorites = favorites,
                        darkMode = darkMode,
                        speechRate = speechRate,
                        speechPitch = speechPitch,
                        notificationSound = notificationSound,
                        fontSizeIndex = fontSizeIndex,
                        language = language,
                        fontFamilyIndex = fontFamilyIndex,
                        tashkeel = tashkeel,
                        khatmahDays = khatmahDays,
                        khatmahStart = khatmahStart,
                        khatmahPagesDone = khatmahPagesDone,
                        khatmahActive = khatmahActive
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TtsManager.stop()
    }

    private fun runStartupStep(name: String, action: () -> Unit) {
        try {
            action()
        } catch (error: Exception) {
            android.util.Log.e(
                "RemoAzkarStartup",
                "Startup step '$name' failed; continuing without it",
                error
            )
        }
    }
}

@Composable
private fun AppRoot(
    preferencesManager: PreferencesManager,
    favorites: Set<String>,
    darkMode: Boolean,
    speechRate: Float,
    speechPitch: Float,
    notificationSound: Boolean,
    fontSizeIndex: Int,
    language: String,
    fontFamilyIndex: Int,
    tashkeel: Boolean,
    khatmahDays: Int,
    khatmahStart: Long,
    khatmahPagesDone: Int,
    khatmahActive: Boolean
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val toggleFavorite: (String) -> Unit = { key ->
        scope.launch { preferencesManager.toggleFavorite(key) }
    }

    // إغلاق القائمة ثم الانتقال إلى المسار المطلوب
    val go: (String) -> Unit = { route ->
        scope.launch { drawerState.close() }
        navController.navigate(route)
    }

    AppSettingsDrawer(
        drawerState = drawerState,
        scope = scope,
        language = language,
        onLanguageChange = { code -> scope.launch { preferencesManager.setLanguage(code) } },
        darkMode = darkMode,
        onDarkModeChange = { enabled -> scope.launch { preferencesManager.setDarkMode(enabled) } },
        fontFamilyIndex = fontFamilyIndex,
        onFontFamilyChange = { index -> scope.launch { preferencesManager.setFontFamily(index) } },
        fontSizeIndex = fontSizeIndex,
        onFontSizeChange = { index -> scope.launch { preferencesManager.setFontSize(index) } },
        tashkeel = tashkeel,
        onTashkeelChange = { enabled -> scope.launch { preferencesManager.setTashkeel(enabled) } },
        onShareGeneric = { ShareUtil.share(context, ShareUtil.inviteMessage()) },
        onShareWhatsapp = { ShareUtil.shareWhatsapp(context, ShareUtil.inviteMessage()) },
        onShareBluetooth = { ShareUtil.shareBluetooth(context, ShareUtil.inviteMessage()) },
        onSmartReader = { go("ai_robot") },
        onExclusive = { go("exclusive") },
        onAdvancedSettings = { go("advanced_settings") },
        onAbout = { go("about") },
        onSupportDeveloper = { go("support_developer") },
        onTelegram = { go("dev_link/telegram") },
        onBlog = { go("dev_link/blog") },
        onPlatform = { go("dev_link/platform") }
    ) {
        Box(Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "splash") {

                composable("splash") {
                    SplashScreen(
                        onEnterClick = {
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        onQuranClick = { navController.navigate("quran") },
                        onHisnClick = { navController.navigate("hisn") },
                        onTasbeehClick = { navController.navigate("tasbeeh") },
                        onRemindersClick = { navController.navigate("reminders") },
                        onHijriClick = { navController.navigate("hijri") },
                        onCustomAzkarClick = { navController.navigate("custom_azkar") },
                        onAiRobotClick = { navController.navigate("ai_robot") },
                        onSpiritualCompanionClick = { navController.navigate("spiritual_companion") },
                        onWirdClick = { navController.navigate("wird") },
                        onRuqyahClick = { navController.navigate("ruqyah") },
                        onAdiyaClick = { navController.navigate("adiya") },
                        onAdvancedSettingsClick = { navController.navigate("advanced_settings") },
                        onSearchClick = { navController.navigate("search") },
                        onFavoritesClick = { navController.navigate("favorites") },
                        onChapterClick = { chapterId -> navController.navigate("hisn_chapter/$chapterId") },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onKhatmahClick = { navController.navigate("khatmah") },
                        onNamesOfAllahClick = { navController.navigate("names_of_allah") },
                        onProphetDuasClick = { navController.navigate("prophet_duas") }
                    )
                }

                // ---------- الصفحات الدينية الجديدة ----------
                composable("khatmah") {
                    KhatmahScreen(
                        days = khatmahDays,
                        startMillis = khatmahStart,
                        pagesDone = khatmahPagesDone,
                        active = khatmahActive,
                        onStart = { days ->
                            scope.launch {
                                preferencesManager.startKhatmah(
                                    days = days,
                                    startMillis = System.currentTimeMillis()
                                )
                            }
                        },
                        onPagesDoneChange = { pages ->
                            scope.launch {
                                preferencesManager.setKhatmahPagesDone(pages)
                            }
                        },
                        onStop = {
                            scope.launch { preferencesManager.stopKhatmah() }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable("names_of_allah") {
                    NamesOfAllahScreen(onBackClick = { navController.popBackStack() })
                }
                composable("prophet_duas") {
                    ProphetDuasScreen(onBackClick = { navController.popBackStack() })
                }

                composable("quran") {
                    QuranScreen(
                        onSurahClick = { surah -> navController.navigate("quran_reader/$surah/0") },
                        onPageClick = { page -> navController.navigate("quran_reader/0/$page") },
                        onSearchClick = { navController.navigate("search") },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "quran_reader/{surah}/{page}",
                    arguments = listOf(
                        navArgument("surah") { type = NavType.IntType; defaultValue = 0 },
                        navArgument("page") { type = NavType.IntType; defaultValue = 0 }
                    )
                ) { entry ->
                    val surah = entry.arguments?.getInt("surah") ?: 0
                    val page = entry.arguments?.getInt("page") ?: 0
                    QuranReaderScreen(
                        surahNumber = surah,
                        pageNumber = page,
                        favorites = favorites,
                        onToggleFavorite = toggleFavorite,
                        fontSizeIndex = fontSizeIndex,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("hisn") {
                    HisnScreen(
                        onChapterClick = { id -> navController.navigate("hisn_chapter/$id") },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "hisn_chapter/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = 0 })
                ) { entry ->
                    val id = entry.arguments?.getInt("id") ?: 0
                    HisnChapterScreen(
                        chapterId = id,
                        favorites = favorites,
                        onToggleFavorite = toggleFavorite,
                        fontSizeIndex = fontSizeIndex,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("tasbeeh") {
                    TasbeehScreen(onBackClick = { navController.popBackStack() })
                }

                composable("reminders") {
                    RemindersScreen(onBackClick = { navController.popBackStack() })
                }

                composable("hijri") {
                    HijriScreen(onBackClick = { navController.popBackStack() })
                }

                composable("custom_azkar") {
                    CustomAzkarScreen(onBackClick = { navController.popBackStack() })
                }

                composable("ai_robot") {
                    AiVoiceAssistantScreen(onBackClick = { navController.popBackStack() })
                }

                composable("wird") {
                    WirdScreen(onBackClick = { navController.popBackStack() })
                }

                composable("ruqyah") {
                    RuqyahScreen(
                        fontSizeIndex = fontSizeIndex,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("adiya") {
                    AdiyaScreen(
                        fontSizeIndex = fontSizeIndex,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("spiritual_companion") {
                    SpiritualCompanionScreen(onBackClick = { navController.popBackStack() })
                }

                composable("advanced_settings") {
                    AdvancedSettingsScreen(
                        darkMode = darkMode,
                        onToggleDarkMode = { enabled ->
                            scope.launch { preferencesManager.setDarkMode(enabled) }
                        },
                        speechRate = speechRate,
                        onSpeechRateChange = { rate ->
                            scope.launch { preferencesManager.setSpeechRate(rate) }
                        },
                        speechPitch = speechPitch,
                        onSpeechPitchChange = { pitch ->
                            scope.launch { preferencesManager.setSpeechPitch(pitch) }
                        },
                        notificationSound = notificationSound,
                        onToggleNotificationSound = { sound ->
                            scope.launch { preferencesManager.setNotificationSound(sound) }
                        },
                        fontSizeIndex = fontSizeIndex,
                        onFontSizeChange = { index ->
                            scope.launch { preferencesManager.setFontSize(index) }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("favorites") {
                    FavoritesScreen(
                        favorites = favorites,
                        onToggleFavorite = toggleFavorite,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("search") {
                    SearchScreen(
                        favorites = favorites,
                        onToggleFavorite = toggleFavorite,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // ---------- الميزات الحصرية ----------
                composable("exclusive") {
                    ExclusiveFeaturesScreen(onBackClick = { navController.popBackStack() })
                }

                // ---------- صفحات المطور ----------
                composable("about") {
                    AboutScreen(
                        onBackClick = { navController.popBackStack() },
                        onSupportClick = { navController.navigate("support_developer") },
                        onTelegramClick = { navController.navigate("dev_link/telegram") },
                        onBlogClick = { navController.navigate("dev_link/blog") },
                        onPlatformClick = { navController.navigate("dev_link/platform") }
                    )
                }

                composable("support_developer") {
                    SupportDeveloperScreen(onBackClick = { navController.popBackStack() })
                }

                composable(
                    route = "dev_link/{type}",
                    arguments = listOf(navArgument("type") { type = NavType.StringType; defaultValue = "platform" })
                ) { entry ->
                    val type = entry.arguments?.getString("type") ?: "platform"
                    DeveloperLinkScreen(
                        type = type,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // زر القارئ الذكي العائم — ظاهر في كل صفحات التطبيق
            SmartReaderFab(
                onOpenReader = { navController.navigate("ai_robot") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 92.dp)
            )

            // شريط القراءة العائم — يظهر أثناء قراءة القارئ الذكي
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                color = androidx.compose.ui.graphics.Color.Transparent
            ) {
                TtsMiniBar()
            }
        }
    }
}
