package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.AthkarWorker
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    autoRemindersEnabled: Boolean,
    onToggleAutoReminders: (Boolean) -> Unit,
    reminderInterval: Int,
    onIntervalChange: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    fun updateWorkManager(enabled: Boolean, interval: Int) {
        val workManager = WorkManager.getInstance(context)
        if (enabled) {
            val workRequest = PeriodicWorkRequestBuilder<AthkarWorker>(
                interval.toLong(), TimeUnit.MINUTES
            ).build()
            workManager.enqueueUniquePeriodicWork(
                "athkar_periodic_work",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        } else {
            workManager.cancelUniqueWork("athkar_periodic_work")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة التنبيهات والأذكار التلقائية", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "يعمل نظام WorkManager في الخلفية لعرض الأذكار والتنبيهات بانتظام على شاشة القفل والإشعارات.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("تفعيل التنبيهات التلقائية", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("إشعارات دورية منتظمة عبر WorkManager", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = autoRemindersEnabled,
                        onCheckedChange = { enabled ->
                            onToggleAutoReminders(enabled)
                            updateWorkManager(enabled, reminderInterval)
                        },
                        modifier = Modifier.testTag("auto_reminders_switch")
                    )
                }
            }

            if (autoRemindersEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text("تكرار التنبيه كل:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        val intervals = listOf(
                            15 to "15 دقيقة",
                            30 to "30 دقيقة",
                            60 to "ساعة واحدة",
                            120 to "ساعتان"
                        )

                        intervals.forEach { (minutes, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onIntervalChange(minutes)
                                        updateWorkManager(true, minutes)
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 15.sp)
                                RadioButton(
                                    selected = reminderInterval == minutes,
                                    onClick = {
                                        onIntervalChange(minutes)
                                        updateWorkManager(true, minutes)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text("نموذج الإشعار المنبثق:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("«سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ»", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
