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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.HisnRepository
import com.example.data.NotificationHelper
import com.example.data.PreferencesManager
import com.example.data.QuranRepository
import com.example.data.ReminderScheduler
import com.example.data.TtsManager
import com.example.ui.components.TtsMiniBar
import com.example.ui.screens.AdvancedSettingsScreen
import com.example.ui.screens.AiVoiceAssistantScreen
import com.example.ui.screens.CustomAzkarScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HijriScreen
import com.example.ui.screens.HisnChapterScreen
import com.example.ui.screens.HisnScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QuranReaderScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.RuqyahScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SpiritualCompanionScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TasbeehScreen
import com.example.ui.screens.WirdScreen
import com.example.ui.theme.MyApplicationTheme
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
        TtsManager.init(applicationContext)
        NotificationHelper.createChannels(applicationContext)
        ReminderScheduler.scheduleAll(applicationContext)
        ReminderScheduler.scheduleDailyFeatures(applicationContext)

        // تحميل بيانات القرآن وحصن المسلم في الذاكرة (خارج الخيط الرئيسي)
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            QuranRepository.ensureLoaded(applicationContext)
            HisnRepository.ensureLoaded(applicationContext)
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

            // مزامنة إعدادات الصوت مع محرّك القراءة
            LaunchedEffect(speechRate, speechPitch) {
                TtsManager.setParams(speechRate, speechPitch)
            }

            // مزامنة إعداد صوت الإشعارات مع المرآة المتزامنة التي يقرأها مستقبِل الإشعارات
            LaunchedEffect(notificationSound) {
                PreferencesManager.mirrorNotificationSound(this@MainActivity, notificationSound)
            }

            MyApplicationTheme(darkTheme = darkMode) {
                AppRoot(
                    preferencesManager = preferencesManager,
                    favorites = favorites,
                    darkMode = darkMode,
                    speechRate = speechRate,
                    speechPitch = speechPitch,
                    notificationSound = notificationSound,
                    fontSizeIndex = fontSizeIndex
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TtsManager.stop()
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
    fontSizeIndex: Int
) {
    val navController = rememberNavController()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val toggleFavorite: (String) -> Unit = { key ->
        scope.launch { preferencesManager.toggleFavorite(key) }
    }

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
                    onAdvancedSettingsClick = { navController.navigate("advanced_settings") },
                    onSearchClick = { navController.navigate("search") },
                    onFavoritesClick = { navController.navigate("favorites") },
                    onChapterClick = { chapterId -> navController.navigate("hisn_chapter/$chapterId") }
                )
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
        }

        // شريط القراءة العائم — يظهر في كل الشاشات أثناء قراءة الروبوت
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
