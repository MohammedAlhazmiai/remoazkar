package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppLocale
import com.example.data.tr
import com.example.ui.theme.AppFonts
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * القائمة الجانبية للإعدادات — تُفتح من زر القائمة في أعلى الشاشة الرئيسية.
 *
 * كل عنصر فيها فعّال ومربوط بحالة حقيقية محفوظة في DataStore:
 *  - اللغة: 16 لغة مع تغيير فوري لاتجاه الواجهة.
 *  - وضع الشاشة: نهاري/ليلي.
 *  - خطوط النص: حجم النص، عائلة الخط (8 خطوط عربية)، والتشكيلات.
 *  - مشاركة التطبيق: عامة، واتساب، بلوتوث.
 *  - عن التطبيق: صفحات المطور الأربع.
 */
@Composable
fun AppSettingsDrawer(
    drawerState: DrawerState,
    scope: CoroutineScope,
    language: String,
    onLanguageChange: (String) -> Unit,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    fontFamilyIndex: Int,
    onFontFamilyChange: (Int) -> Unit,
    fontSizeIndex: Int,
    onFontSizeChange: (Int) -> Unit,
    tashkeel: Boolean,
    onTashkeelChange: (Boolean) -> Unit,
    onShareGeneric: () -> Unit,
    onShareWhatsapp: () -> Unit,
    onShareBluetooth: () -> Unit,
    onSmartReader: () -> Unit,
    onExclusive: () -> Unit,
    onAdvancedSettings: () -> Unit,
    onAbout: () -> Unit,
    onSupportDeveloper: () -> Unit,
    onTelegram: () -> Unit,
    onBlog: () -> Unit,
    onPlatform: () -> Unit,
    content: @Composable () -> Unit
) {
    var showLanguages by remember { mutableStateOf(false) }
    var showFonts by remember { mutableStateOf(false) }
    var showShare by remember { mutableStateOf(false) }

    val close: () -> Unit = { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 2000.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ---------- رأس القائمة ----------
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0F2E24), Color(0xFF1B4D3E), Color(0xFF2E7D5B))
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 22.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .border(1.dp, GoldAccent.copy(alpha = 0.6f), RoundedCornerShape(15.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = tr("app_name"),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                )
                                Text(
                                    text = "قرآن · أذكار · قارئ ذكي",
                                    color = Color(0xFFD9E7DF),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    DrawerSectionTitle(tr("general"))

                    // ---------- اللغة ----------
                    DrawerRow(
                        icon = Icons.Default.Language,
                        title = tr("language"),
                        value = AppLocale.languageFor(language).nativeName,
                        onClick = { showLanguages = true }
                    )

                    // ---------- وضع الشاشة ----------
                    DrawerRow(
                        icon = if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        title = tr("screen_mode"),
                        value = if (darkMode) tr("night_mode") else tr("day_mode"),
                        trailing = {
                            Switch(
                                checked = darkMode,
                                onCheckedChange = onDarkModeChange
                            )
                        },
                        onClick = { onDarkModeChange(!darkMode) }
                    )

                    // ---------- خطوط النص ----------
                    DrawerRow(
                        icon = Icons.Default.FormatSize,
                        title = tr("fonts"),
                        value = "${AppFonts.all.getOrNull(fontFamilyIndex)?.arabicName ?: ""} · ${
                            listOf(tr("small"), tr("medium"), tr("large")).getOrElse(fontSizeIndex) { tr("medium") }
                        }",
                        onClick = { showFonts = !showFonts }
                    )
                    AnimatedVisibility(visible = showFonts) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                tr("font_size"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(tr("small"), tr("medium"), tr("large")).forEachIndexed { index, label ->
                                    SelectChip(
                                        label = label,
                                        selected = fontSizeIndex == index,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onFontSizeChange(index) }
                                    )
                                }
                            }
                            Text(
                                tr("font_family"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(AppFonts.all.size) { index ->
                                    val option = AppFonts.all[index]
                                    SelectChip(
                                        label = option.arabicName,
                                        selected = fontFamilyIndex == index,
                                        onClick = { onFontFamilyChange(index) }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        tr("tashkeel"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        if (tashkeel) tr("tashkeel_on") else tr("tashkeel_off"),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Switch(checked = tashkeel, onCheckedChange = onTashkeelChange)
                            }
                        }
                    }

                    // ---------- مشاركة التطبيق ----------
                    DrawerRow(
                        icon = Icons.Default.Share,
                        title = tr("share_app"),
                        value = tr("share_hint"),
                        onClick = { showShare = !showShare }
                    )
                    AnimatedVisibility(visible = showShare) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ShareButton(
                                icon = Icons.Default.Share,
                                label = tr("share_generic"),
                                onClick = onShareGeneric
                            )
                            ShareButton(
                                icon = Icons.Default.Chat,
                                label = tr("share_whatsapp"),
                                onClick = onShareWhatsapp
                            )
                            ShareButton(
                                icon = Icons.Default.Bluetooth,
                                label = tr("share_bluetooth"),
                                onClick = onShareBluetooth
                            )
                        }
                    }

                    DrawerSectionTitle("أدوات")

                    DrawerRow(
                        icon = Icons.Default.SmartToy,
                        title = tr("smart_reader"),
                        value = "يقرأ أي نص في أي صفحة",
                        onClick = { close(); onSmartReader() }
                    )
                    DrawerRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "ميزات حصرية",
                        value = "بصمة النور · مسبحة الحركة · الذكر الآن",
                        onClick = { close(); onExclusive() }
                    )
                    DrawerRow(
                        icon = Icons.Default.Settings,
                        title = tr("advanced_settings"),
                        value = "الصوت · الإشعارات · الخط",
                        onClick = { close(); onAdvancedSettings() }
                    )
                    DrawerRow(
                        icon = Icons.Default.Image,
                        title = "خلفية التطبيق",
                        value = "اختر صورة من معرض الهاتف",
                        onClick = { close(); onAdvancedSettings() }
                    )

                    DrawerSectionTitle(tr("about_app"))

                    DrawerRow(
                        icon = Icons.Default.Info,
                        title = tr("about_app"),
                        value = "زاد المسلم · الإصدار 1.0",
                        onClick = { close(); onAbout() }
                    )
                    DrawerRow(
                        icon = Icons.Default.Favorite,
                        title = tr("support_developer"),
                        value = "محمد الحزمي · محفظة جيب",
                        onClick = { close(); onSupportDeveloper() }
                    )
                    DrawerRow(
                        icon = Icons.Default.Verified,
                        title = tr("telegram_channel"),
                        value = "t.me/moh_alymani1",
                        onClick = { close(); onTelegram() }
                    )
                    DrawerRow(
                        icon = Icons.Default.Article,
                        title = tr("developer_blog"),
                        value = "mohammedalhzmi.blogspot.com",
                        onClick = { close(); onBlog() }
                    )
                    DrawerRow(
                        icon = Icons.Default.MenuBook,
                        title = tr("developer_platform"),
                        value = "منصة الذكاء الاصطناعي",
                        onClick = { close(); onPlatform() }
                    )

                    Divider(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                    Text(
                        text = "برمجة وتطوير المطور محمد الحزمي",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 22.dp)
                    )
                }
            }
        },
        content = content
    )

    // ---------- حوار اللغة ----------
    if (showLanguages) {
        AlertDialog(
            onDismissRequest = { showLanguages = false },
            title = { Text(tr("language")) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(AppLocale.languages) { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLanguageChange(lang.code)
                                    showLanguages = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lang.flag, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    lang.nativeName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    lang.englishName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                            if (language == lang.code) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguages = false }) { Text(tr("close")) }
            }
        )
    }
}

@Composable
private fun DrawerSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun DrawerRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                value,
                fontSize = 11.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
            )
        }
        if (trailing != null) trailing() else Icon(
            Icons.Default.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun SelectChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ShareButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp)
    }
}
