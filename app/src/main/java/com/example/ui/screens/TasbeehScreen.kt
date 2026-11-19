package com.example.ui.screens

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.data.TasbeehState
import com.example.data.TtsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbeehScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var count by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }
    var cyclesCompleted by remember { mutableStateOf(0) }
    var selectedZikrIndex by remember { mutableIntStateOf(0) }
    var targetCount by remember { mutableIntStateOf(33) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    // حفظ المجموع الكلي واستعادته بعد إغلاق التطبيق
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val savedTotal by prefs.tasbeehTotalFlow.collectAsState(initial = -1)
    val savedState by prefs.tasbeehStateFlow.collectAsState(initial = null as TasbeehState?)
    var restored by remember { mutableStateOf(false) }
    var stateRestored by remember { mutableStateOf(false) }
    LaunchedEffect(savedTotal) {
        if (!restored && savedTotal >= 0) {
            totalCount = savedTotal
            restored = true
        }
    }
    LaunchedEffect(savedState) {
        val state = savedState
        if (!stateRestored && state != null) {
            count = state.count
            targetCount = state.target
            selectedZikrIndex = state.zikrIndex.coerceIn(0, 5)
            cyclesCompleted = state.cyclesCompleted
            vibrationEnabled = state.vibrationEnabled
            stateRestored = true
        }
    }

    fun persistState() {
        scope.launch {
            prefs.saveTasbeehState(
                TasbeehState(count, targetCount, selectedZikrIndex, cyclesCompleted, vibrationEnabled)
            )
        }
    }

    val zikrOptions = listOf(
        "سُبْحَانَ اللَّهِ",
        "الْحَمْدُ لِلَّهِ",
        "اللَّهُ أَكْبَرُ",
        "لَا إِلَهَ إِلَّا اللَّهُ",
        "أَسْتَغْفِرُ اللَّهَ",
        "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ"
    )

    val currentZikr = zikrOptions[selectedZikrIndex]
    val progress = if (targetCount > 0) count.toFloat() / targetCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    fun triggerVibration() {
        if (!vibrationEnabled) return
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(45)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المسبحة الإلكترونية المتطورة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vibrationEnabled = !vibrationEnabled; persistState() },
                        modifier = Modifier.testTag("vibration_toggle")
                    ) {
                        Icon(
                            imageVector = if (vibrationEnabled) Icons.Default.Vibration else Icons.Default.NotificationsOff,
                            contentDescription = "الاهتزاز",
                            tint = if (vibrationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(
                        onClick = {
                            count = 0
                            persistState()
                        },
                        modifier = Modifier.testTag("reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "إعادة ضبط")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                zikrOptions.forEachIndexed { index, zikr ->
                    val isSelected = selectedZikrIndex == index
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedZikrIndex = index
                            count = 0
                            persistState()
                        },
                        label = { Text(zikr, fontSize = 12.sp) },
                        modifier = Modifier.testTag("chip_$index")
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentZikr,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { TtsManager.toggle(currentZikr) }
                        )
                        IconButton(onClick = { TtsManager.toggle(currentZikr) }) {
                            Icon(
                                imageVector = if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = "قراءة الذكر",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text(
                            text = "الهدف: $targetCount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "الدورات: $cyclesCompleted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "المجموع: $totalCount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 12.dp,
                )

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            triggerVibration()
                            count++
                            totalCount++
                            scope.launch { prefs.saveTasbeehTotal(totalCount) }
                            if (count >= targetCount) {
                                cyclesCompleted++
                                count = 0
                            }
                            persistState()
                        }
                        .testTag("tasbeeh_click_area"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$count",
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اضغط هنا للتسبيح",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "اختر الهدف:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(33, 100, 500, 1000).forEach { target ->
                        OutlinedButton(
                            onClick = {
                                targetCount = target
                                count = 0
                                persistState()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (targetCount == target) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("$target", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
