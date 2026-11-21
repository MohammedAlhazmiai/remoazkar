package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PathToSuccessData
import com.example.data.TtsManager
import com.example.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathToSuccessScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var selectedId by rememberSaveable { mutableStateOf(PathToSuccessData.sections.first().id) }
    val currentSection = PathToSuccessData.sections.firstOrNull { it.id == selectedId } ?: PathToSuccessData.sections.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("طريق الناجحين", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val fullText = "${currentSection.title}\n\n${currentSection.subtitle}\n\n" +
                            currentSection.paragraphs.joinToString("\n\n") +
                            "\n\nالآية: ${currentSection.quranAyah}\nالحديث: ${currentSection.hadith}"
                        ShareUtil.share(context, fullText)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF123B31),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ScrollableTabRow(
                selectedTabIndex = PathToSuccessData.sections.indexOfFirst { it.id == selectedId }.coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                PathToSuccessData.sections.forEach { section ->
                    Tab(
                        selected = section.id == selectedId,
                        onClick = { selectedId = section.id },
                        text = {
                            Text(
                                text = section.title.substringBefore("."),
                                fontWeight = if (section.id == selectedId) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF174B3B))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = currentSection.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD36A)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = currentSection.subtitle,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = {
                                    val speech = "${currentSection.title}. ${currentSection.subtitle}. " +
                                        currentSection.paragraphs.joinToString(". ")
                                    TtsManager.toggle(speech)
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFFFD36A),
                                    contentColor = Color(0xFF123B31)
                                )
                            ) {
                                Icon(
                                    if (TtsManager.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (TtsManager.isSpeaking) "إيقاف القراءة" else "استمع للموضوع صوتياً")
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "﴿ ${currentSection.quranAyah} ﴾",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                lineHeight = 28.sp
                            )
                        }
                    }
                }

                items(currentSection.paragraphs, key = { it.hashCode() }) { paragraph ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = paragraph,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 16.sp,
                            lineHeight = 30.sp,
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "من هدي السنة النبوية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = currentSection.hadith,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
