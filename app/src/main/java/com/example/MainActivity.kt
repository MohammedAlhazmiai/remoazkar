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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ai.AppCheckInitializer
import com.example.data.AthkarReminderScheduler
import com.example.data.AppBackgroundStore
import com.example.data.AppUpdateChecker
import com.example.data.AppLocale
import com.example.data.HijriCalendar
import com.example.data.HisnRepository
import com.example.data.LocalAppLanguage
import com.example.data.LocalAppStrings
import com.example.data.LocalTashkeelEnabled
import com.example.data.NotificationHelper
import com.example.data.PrayerAlarmScheduler
import com.example.data.PreferencesManager
import com.example.data.PreferencesSnapshot
import com.example.data.QuranRepository
import com.example.data.ReminderScheduler
import com.example.data.TtsManager
import com.example.data.UpdateCheckResult
import com.example.service.FloatingTasbeehService
import com.example.ui.components.AppBackgroundLayer
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.AppSettingsDrawer
import com.example.ui.components.SmartReaderBus
import com.example.ui.components.SmartReaderFab
import com.example.ui.components.TtsMiniBar
import com.example.ui.components.UpdateDialogState
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AudioAzkarScreen
import com.example.ui.screens.AdiyaScreen
import com.example.ui.screens.AdvancedSettingsScreen
import com.example.ui.screens.AgeCalculatorScreen
import com.example.ui.screens.AiVoiceAssistantScreen
import com.example.ui.screens.CustomAzkarScreen
import com.example.ui.screens.DeveloperLinkScreen
import com.example.ui.screens.ExclusiveFeaturesScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HijriScreen
import com.example.ui.screens.HisnChapterScreen
import com.example.ui.screens.HisnScreen
import com.example.ui.screens.IslamicAiScreen
import com.example.ui.screens.KhatmahScreen
import com.example.ui.screens.NamesOfAllahScreen
import com.example.ui.screens.ProphetDuasScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PrayerTimesScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.QuranReaderScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.RuqyahScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SpiritualCompanionScreen
import com.example.ui.screens.PathToSuccessScreen
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

    // مسار داخلي مطلوب فتحه عند الدخول من اختصار المشغّل (الضغط المطوّل على أيقونة التطبيق)
    private val pendingRouteState = androidx.compose.runtime.mutableStateOf<String?>(null)
    private var pendingRoute: String?
        get() = pendingRouteState.value
        set(value) { pendingRouteState.value = value }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesManager = PreferencesManager(applicationContext)

        // معالجة الدخول من اختصارات المشغّل (المسبحة العائمة / حاسبة العمر / الأذكار الصوتية)
        handleShortcutIntent(intent)

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
        runStartupStep("prayer alarm scheduling") {
            PrayerAlarmScheduler.scheduleAll(applicationContext)
        }
        runStartupStep("smart reader history") { SmartReaderBus.load(applicationContext) }
        runStartupStep("hijri offset") { HijriCalendar.initOffset(applicationContext) }

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
            val preferences by preferencesManager.snapshotFlow.collectAsStateWithLifecycle(
                initialValue = PreferencesSnapshot()
            )
            val darkMode = preferences.darkMode
            val favorites = preferences.favorites
            val speechRate = preferences.speechRate
            val speechPitch = preferences.speechPitch
            val notificationSound = preferences.notificationSound
            val fontSizeIndex = preferences.fontSizeIndex
            val autoReminders = preferences.autoReminders
            val reminderInterval = preferences.reminderInterval
            val azkarTime = preferences.azkarTime
            val language = preferences.language
            val fontFamilyIndex = preferences.fontFamilyIndex
            val tashkeel = preferences.tashkeel
            val khatmahDays = preferences.khatmahDays
            val khatmahStart = preferences.khatmahStart
            val khatmahPagesDone = preferences.khatmahPagesDone
            val khatmahActive = preferences.khatmahActive

            // تتبّع الصفحة الحالية لتطبيق خلفيتها الخاصة (لكل صفحة خلفية مستقلة).
            val navController = rememberNavController()

            // الانتقال إلى الصفحة المطلوبة عند الدخول من اختصار المشغّل
            LaunchedEffect(pendingRoute) {
                val target = pendingRoute
                if (target != null) {
                    pendingRoute = null
                    runCatching { navController.navigate(target) }
                }
            }

            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            var previousRoute by rememberSaveable { mutableStateOf<String?>(null) }
            LaunchedEffect(currentRoute) {
                val prev = previousRoute
                if (currentRoute == "home" && prev != null && prev != "home" && prev != "splash") {
                    AppBackgroundStore.rotateHomeBackground()
                }
                if (currentRoute != null) {
                    previousRoute = currentRoute
                }
            }
            val pageKey = AppBackgroundStore.pageKeyForRoute(currentRoute)
            val backgroundRevision = AppBackgroundStore.revision
            val hasCustomPageColor = preferences.pageColor != 0L
            val pageBackgroundEnabled = remember(backgroundRevision, pageKey, hasCustomPageColor) {
                AppBackgroundStore.shouldRenderBackgroundLayer(
                    context = this@MainActivity,
                    pageKey = pageKey,
                    hasCustomPageColor = hasCustomPageColor
                )
            }

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
                    fontFamily = AppFonts.familyFor(fontFamilyIndex),
                    pageColor = if (preferences.pageColor != 0L) Color(preferences.pageColor) else null,
                    fontColor = if (preferences.fontColor != 0L) Color(preferences.fontColor) else null,
                    transparentBackground = pageBackgroundEnabled
                ) {
                    Box(Modifier.fillMaxSize()) {
                        AppBackgroundLayer(
                            pageKey = pageKey,
                            darkMode = darkMode,
                            hasCustomPageColor = hasCustomPageColor
                        )
                        AppRoot(
                            navController = navController,
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
                            khatmahActive = khatmahActive,
                            pageColor = preferences.pageColor,
                            fontColor = preferences.fontColor
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TtsManager.stop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    /** يوجّه المستخدم حسب الاختصار الذي ضغط عليه من شاشة المشغّل. */
    private fun handleShortcutIntent(intent: android.content.Intent?) {
        when (intent?.action) {
            ACTION_SHORTCUT_AGE -> pendingRoute = "age_calculator"
            ACTION_SHORTCUT_AUDIO -> pendingRoute = "audio_azkar"
            ACTION_SHORTCUT_TASBEEH -> {
                if (FloatingTasbeehService.canDrawOverlays(this)) {
                    runCatching {
                        ContextCompat.startForegroundService(
                            this,
                            android.content.Intent(this, FloatingTasbeehService::class.java)
                        )
                    }
                } else {
                    runCatching {
                        startActivity(
                            android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:$packageName")
                            )
                        )
                    }
                }
            }
        }
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

    companion object {
        const val ACTION_SHORTCUT_AGE = "com.aistudio.zadulmuslim.kxmpzq.SHORTCUT_AGE_CALCULATOR"
        const val ACTION_SHORTCUT_TASBEEH = "com.aistudio.zadulmuslim.kxmpzq.SHORTCUT_FLOATING_TASBEEH"
        const val ACTION_SHORTCUT_AUDIO = "com.aistudio.zadulmuslim.kxmpzq.SHORTCUT_AUDIO_AZKAR"
    }
}

@Composable
private fun AppRoot(
    navController: NavHostController,
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
    khatmahActive: Boolean,
    pageColor: Long,
    fontColor: Long
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val updateChecker = remember(context.applicationContext) {
        AppUpdateChecker(context.applicationContext)
    }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    var updateDialog by remember { mutableStateOf<UpdateDialogState?>(null) }
    var automaticUpdateCheckStarted by rememberSaveable { mutableStateOf(false) }

    val requestUpdateCheck: () -> Unit = {
        updateDialog = UpdateDialogState.Checking
        scope.launch {
            updateDialog = when (val result = updateChecker.check(force = true)) {
                is UpdateCheckResult.Available -> UpdateDialogState.Available(result.update)
                UpdateCheckResult.UpToDate -> UpdateDialogState.UpToDate
                UpdateCheckResult.NoPublishedRelease -> UpdateDialogState.NoPublishedRelease
                UpdateCheckResult.Skipped -> UpdateDialogState.UpToDate
                UpdateCheckResult.Failed -> UpdateDialogState.Failed
            }
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute == "home" && !automaticUpdateCheckStarted) {
            automaticUpdateCheckStarted = true
            if (updateChecker.shouldAutomaticallyCheck()) {
                when (val result = updateChecker.check()) {
                    is UpdateCheckResult.Available -> {
                        if (!updateChecker.wasAutomaticallyPromptedFor(result.update.version)) {
                            updateChecker.markAutomaticallyPrompted(result.update.version)
                            updateDialog = UpdateDialogState.Available(result.update)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

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
        onCheckForUpdates = requestUpdateCheck,
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
                        onProphetDuasClick = { navController.navigate("prophet_duas") },
                        onAudioAzkarClick = { navController.navigate("audio_azkar") },
                        onPathToSuccessClick = { navController.navigate("path_to_success") },
                        onAgeCalculatorClick = { navController.navigate("age_calculator") },
                        onPrayerTimesClick = { navController.navigate("prayer_times") },
                        onQiblaClick = { navController.navigate("qibla") },
                        onIslamicAiClick = { navController.navigate("islamic_ai_assistant") },
                        onFloatingTasbeehClick = {
                            val intent = android.content.Intent(context, FloatingTasbeehService::class.java)
                                .setAction(FloatingTasbeehService.ACTION_START)
                            if (FloatingTasbeehService.canDrawOverlays(context)) {
                                FloatingTasbeehService.setEnabled(context, true)
                                runCatching { ContextCompat.startForegroundService(context, intent) }
                            } else {
                                runCatching {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                }
                            }
                        }
                    )
                }

                composable("age_calculator") {
                    AgeCalculatorScreen(onBackClick = { navController.popBackStack() })
                }

                composable("prayer_times") {
                    PrayerTimesScreen(onBackClick = { navController.popBackStack() })
                }

                composable("qibla") {
                    QiblaScreen(onBackClick = { navController.popBackStack() })
                }

                composable("audio_azkar") {
                    AudioAzkarScreen(onBackClick = { navController.popBackStack() })
                }

                composable("path_to_success") {
                    PathToSuccessScreen(onBackClick = { navController.popBackStack() })
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

                composable("islamic_ai_assistant") {
                    IslamicAiScreen(
                        onBackClick = { navController.popBackStack() },
                        onSupportDeveloperClick = { navController.navigate("support_developer") }
                    )
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
                        onSupportDeveloper = { navController.navigate("support_developer") },
                        onTelegram = { navController.navigate("dev_link/telegram") },
                        onBlog = { navController.navigate("dev_link/blog") },
                        onPlatform = { navController.navigate("dev_link/platform") },
                        pageColor = pageColor,
                        fontColor = fontColor,
                        onPageColorChange = { value ->
                            scope.launch { preferencesManager.setPageColor(value) }
                        },
                        onFontColorChange = { value ->
                            scope.launch { preferencesManager.setFontColor(value) }
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

            if (currentRoute != "islamic_ai_assistant") {
                // زر القارئ الذكي العائم — ظاهر في كل صفحات التطبيق ما عدا شاشة المحادثة حتى لا يغطي زر الإرسال
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

    updateDialog?.let { dialog ->
        AppUpdateDialog(
            state = dialog,
            onDismiss = { updateDialog = null },
            onDownload = { update ->
                updateDialog = null
                val downloadIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(update.downloadUrl)
                )
                runCatching { context.startActivity(downloadIntent) }.onFailure {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(update.releaseUrl)
                            )
                        )
                    }
                }
            }
        )
    }
}
