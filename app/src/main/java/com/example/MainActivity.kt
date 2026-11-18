package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.PreferencesManager
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferencesManager = PreferencesManager(applicationContext)

        setContent {
            val darkMode by preferencesManager.darkModeFlow.collectAsState(initial = false)
            val favorites by preferencesManager.favoritesFlow.collectAsState(initial = emptySet())
            val autoReminders by preferencesManager.autoRemindersFlow.collectAsState(initial = true)
            val reminderInterval by preferencesManager.reminderIntervalFlow.collectAsState(initial = 60)
            val speechRate by preferencesManager.speechRateFlow.collectAsState(initial = 1.0f)
            val speechPitch by preferencesManager.speechPitchFlow.collectAsState(initial = 1.0f)
            val notificationSound by preferencesManager.notificationSoundFlow.collectAsState(initial = true)

            val navController = rememberNavController()

            MyApplicationTheme(darkTheme = darkMode) {
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
                            onCategoryClick = { categoryId ->
                                navController.navigate("azkar_list/$categoryId")
                            },
                            onTasbeehClick = {
                                navController.navigate("tasbeeh")
                            },
                            onRemindersClick = {
                                navController.navigate("reminders")
                            },
                            onQuranClick = {
                                navController.navigate("quran")
                            },
                            onAudioAthkarClick = {
                                navController.navigate("audio_athkar")
                            },
                            onAiRobotClick = {
                                navController.navigate("ai_robot")
                            },
                            onSpiritualCompanionClick = {
                                navController.navigate("spiritual_companion")
                            },
                            onAdvancedSettingsClick = {
                                navController.navigate("advanced_settings")
                            },
                            onSearchClick = {
                                navController.navigate("search")
                            },
                            onFavoritesClick = {
                                navController.navigate("favorites")
                            }
                        )
                    }
                    composable("quran") {
                        QuranScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("audio_athkar") {
                        AudioAthkarScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("ai_robot") {
                        AiVoiceAssistantScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("spiritual_companion") {
                        SpiritualCompanionScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("advanced_settings") {
                        AdvancedSettingsScreen(
                            darkMode = darkMode,
                            onToggleDarkMode = { enabled ->
                                lifecycleScope.launch {
                                    preferencesManager.setDarkMode(enabled)
                                }
                            },
                            speechRate = speechRate,
                            onSpeechRateChange = { rate ->
                                lifecycleScope.launch {
                                    preferencesManager.setSpeechRate(rate)
                                }
                            },
                            speechPitch = speechPitch,
                            onSpeechPitchChange = { pitch ->
                                lifecycleScope.launch {
                                    preferencesManager.setSpeechPitch(pitch)
                                }
                            },
                            notificationSound = notificationSound,
                            onToggleNotificationSound = { sound ->
                                lifecycleScope.launch {
                                    preferencesManager.setNotificationSound(sound)
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "azkar_list/{categoryId}",
                        arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val categoryId = backStackEntry.arguments?.getString("categoryId") ?: "morning"
                        AzkarListScreen(
                            categoryId = categoryId,
                            favorites = favorites,
                            onToggleFavorite = { zikrId ->
                                lifecycleScope.launch {
                                    preferencesManager.toggleFavorite(zikrId)
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("tasbeeh") {
                        TasbeehScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("reminders") {
                        RemindersScreen(
                            autoRemindersEnabled = autoReminders,
                            onToggleAutoReminders = { enabled ->
                                lifecycleScope.launch {
                                    preferencesManager.setAutoReminders(enabled)
                                }
                            },
                            reminderInterval = reminderInterval,
                            onIntervalChange = { interval ->
                                lifecycleScope.launch {
                                    preferencesManager.setReminderInterval(interval)
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("favorites") {
                        FavoritesScreen(
                            favorites = favorites,
                            onToggleFavorite = { zikrId ->
                                lifecycleScope.launch {
                                    preferencesManager.toggleFavorite(zikrId)
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            favorites = favorites,
                            onToggleFavorite = { zikrId ->
                                lifecycleScope.launch {
                                    preferencesManager.toggleFavorite(zikrId)
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
