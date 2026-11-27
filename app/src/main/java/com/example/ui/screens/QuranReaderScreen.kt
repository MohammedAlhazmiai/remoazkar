package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Ayah
import com.example.data.PreferencesManager
import com.example.data.QuranReaderPrefs
import com.example.data.QuranRepository
import com.example.data.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahNumber: Int,
    pageNumber: Int,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    fontSizeIndex: Int,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { PreferencesManager(context) }
    val readerPrefs by prefs.quranReaderPrefsFlow.collectAsState(initial = QuranReaderPrefs())

    // إبقاء شاشة الهاتف مضاءة أثناء تلاوة القرآن الكريم
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var renderer by remember(context) { mutableStateOf<BundledMushaf?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var pageCount by remember { mutableIntStateOf(0) }
    var pageIndex by rememberSaveable { mutableIntStateOf((pageNumber - 1).coerceAtLeast(0)) }
    var activeSurahNumber by rememberSaveable { mutableIntStateOf(surahNumber) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // وضع القراءة (ملء الشاشة / عادي) والتكبير والسحب
    var isFullScreen by rememberSaveable { mutableStateOf(false) }
    var showFullScreenOverlay by remember { mutableStateOf(true) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var pageInput by remember { mutableStateOf("") }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var mushafViewportSize by remember { mutableStateOf(IntSize.Zero) }

    val isTextMode = readerPrefs.textMode
    val themeIndex = readerPrefs.themeIndex
    val textFontSp = readerPrefs.textFontSp
    val isCurrentBookmarked = remember(readerPrefs.bookmarks, pageIndex) {
        val pageOneBased = pageIndex + 1
        readerPrefs.bookmarks.any { it.startsWith("$pageOneBased|") }
    }

    BackHandler(enabled = isFullScreen) {
        isFullScreen = false
    }

    DisposableEffect(renderer) {
        val openedRenderer = renderer
        onDispose { runCatching { openedRenderer?.close() } }
    }

    LaunchedEffect(context) {
        QuranRepository.ensureLoaded(context)
        val result = withContext(Dispatchers.IO) { runCatching { BundledMushaf(context) } }
        renderer = result.getOrNull()
        error = result.exceptionOrNull()?.let {
            "تعذّر فتح ملف المصحف المرفق على هذا الجهاز. تحقّق من سلامة ملف PDF ثم أعد المحاولة."
        }
        isLoading = false
    }

    LaunchedEffect(renderer) {
        renderer?.let { opened ->
            pageCount = opened.pageCount
            if (activeSurahNumber > 0 && pageNumber <= 0) {
                val standardStart = runCatching {
                    QuranRepository.surah(activeSurahNumber)?.pageStart ?: 1
                }.getOrDefault(1)
                pageIndex = (((standardStart - 1).toDouble() * (pageCount - 1).coerceAtLeast(0)) / 603.0)
                    .toInt().coerceIn(0, (pageCount - 1).coerceAtLeast(0))
            } else {
                pageIndex = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
            }
        }
    }

    // حفظ آخر موضع قراءة وإعادة ضبط موضع الإزاحة عند تغيير الصفحة
    LaunchedEffect(pageIndex, pageCount, activeSurahNumber) {
        if (pageCount > 0) {
            prefs.setLastQuranPosition(activeSurahNumber, pageIndex + 1)
        }
        panOffset = Offset.Zero
    }

    val targetRenderWidth = remember(zoomScale) {
        if (zoomScale >= 1.55f) 2400 else 1950
    }

    LaunchedEffect(renderer, pageIndex, pageCount, targetRenderWidth) {
        val activeRenderer = renderer
        if (activeRenderer != null && pageCount > 0) {
            error = null
            val result = withContext(Dispatchers.IO) {
                runCatching { activeRenderer.render(pageIndex, targetRenderWidth) }
            }
            bitmap = result.getOrNull()
            error = result.exceptionOrNull()?.let {
                "تعذّر عرض الصفحة ${pageIndex + 1} من ملف المصحف: ${it.localizedMessage ?: "ملف PDF غير صالح"}"
            }
            pageInput = (pageIndex + 1).toString()
        }
    }

    val surahInfo = remember(activeSurahNumber, QuranRepository.isLoaded) {
        if (activeSurahNumber > 0) QuranRepository.surah(activeSurahNumber) else null
    }
    val approxStandardPage = remember(pageIndex, pageCount) {
        if (pageCount <= 1) 1
        else (((pageIndex.toDouble() * 603.0) / (pageCount - 1).coerceAtLeast(1)).roundToInt() + 1)
            .coerceIn(1, 604)
    }

    val clampPan: (Offset, Float) -> Offset = { raw, scale ->
        if (scale <= 1.02f || mushafViewportSize == IntSize.Zero) {
            Offset.Zero
        } else {
            val maxX = ((mushafViewportSize.width * (scale - 1f)) / 2f).coerceAtLeast(0f)
            val maxY = ((mushafViewportSize.height * (scale - 1f)) / 2f).coerceAtLeast(0f)
            Offset(raw.x.coerceIn(-maxX, maxX), raw.y.coerceIn(-maxY, maxY))
        }
    }

    val zoomInAction: () -> Unit = {
        if (isTextMode) {
            val nextSp = (textFontSp + 3f).coerceAtMost(48f)
            scope.launch { prefs.setQuranTextFontSp(nextSp) }
        } else {
            val nextScale = (zoomScale + 0.35f).coerceAtMost(3.8f)
            zoomScale = nextScale
            panOffset = clampPan(panOffset, nextScale)
        }
    }

    val zoomOutAction: () -> Unit = {
        if (isTextMode) {
            val nextSp = (textFontSp - 3f).coerceAtLeast(18f)
            scope.launch { prefs.setQuranTextFontSp(nextSp) }
        } else {
            val nextScale = (zoomScale - 0.35f).coerceAtLeast(1f)
            zoomScale = nextScale
            panOffset = clampPan(panOffset, nextScale)
        }
    }

    val resetZoomAction: () -> Unit = {
        if (isTextMode) {
            scope.launch { prefs.setQuranTextFontSp(26f) }
        } else {
            zoomScale = 1f
            panOffset = Offset.Zero
        }
    }

    val goPrevPage: () -> Unit = {
        if (isTextMode && activeSurahNumber > 1) {
            activeSurahNumber -= 1
        } else if (pageIndex > 0) {
            pageIndex -= 1
            zoomScale = 1f
            panOffset = Offset.Zero
        }
    }

    val goNextPage: () -> Unit = {
        if (isTextMode && activeSurahNumber in 1..113) {
            activeSurahNumber += 1
        } else if (pageIndex < pageCount - 1) {
            pageIndex += 1
            zoomScale = 1f
            panOffset = Offset.Zero
        }
    }

    val readerBgColor = when (themeIndex) {
        1 -> Color(0xFFFBF3DF)
        2 -> Color(0xFF101513)
        else -> Color(0xFFFFFFFF)
    }
    val readerTextColor = when (themeIndex) {
        1 -> Color(0xFF2B2111)
        2 -> Color(0xFFECE5D3)
        else -> Color(0xFF151C18)
    }
    val readerAccentColor = when (themeIndex) {
        1 -> Color(0xFF8C5B10)
        2 -> Color(0xFFFFD36A)
        else -> MaterialTheme.colorScheme.primary
    }

    val pdfColorFilter = remember(themeIndex) {
        when (themeIndex) {
            1 -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.98f, 0f, 0f, 0f, 4f,
                        0f, 0.94f, 0f, 0f, 2f,
                        0f, 0f, 0.82f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            2 -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -0.91f, 0f, 0f, 0f, 240f,
                        0f, -0.92f, 0f, 0f, 236f,
                        0f, 0f, -0.94f, 0f, 224f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            else -> null
        }
    }

    Scaffold(
        containerColor = if (isFullScreen) readerBgColor else MaterialTheme.colorScheme.background,
        topBar = {
            if (!isFullScreen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                surahInfo?.let { "سورة ${it.name}" } ?: "المصحف الشريف",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                if (isTextMode && surahInfo != null)
                                    "${surahInfo.revelationType} • ${surahInfo.ayahCount} آية"
                                else
                                    "صفحة ${pageIndex + 1} من ${pageCount.coerceAtLeast(0)}",
                                fontSize = 11.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val label = surahInfo?.let { "سورة ${it.name} · صفحة ${pageIndex + 1}" }
                                    ?: "صفحة ${pageIndex + 1}"
                                scope.launch {
                                    prefs.toggleQuranBookmark(pageIndex + 1, label)
                                    Toast.makeText(
                                        context,
                                        if (isCurrentBookmarked) "تمت إزالة العلامة المرجعية" else "تم حفظ علامة مرجعية للصفحة ${pageIndex + 1}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.testTag("quran_bookmark_button")
                        ) {
                            Icon(
                                if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isCurrentBookmarked) "إزالة العلامة" else "حفظ علامة مرجعية",
                                tint = if (isCurrentBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(
                            onClick = {
                                isFullScreen = true
                                showFullScreenOverlay = true
                            },
                            modifier = Modifier.testTag("quran_fullscreen_button")
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "تكبير ملء الشاشة")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // شريط التحكم المدمج (الوضع، المظهر، أزرار التكبير والتصغير) عند عدم التواجد في ملء الشاشة
                if (!isFullScreen) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = !isTextMode,
                                onClick = { scope.launch { prefs.setQuranTextMode(false) } },
                                label = { Text("المصحف المصوّر", fontSize = 12.sp) },
                                modifier = Modifier.testTag("quran_mode_pdf_chip")
                            )
                            FilterChip(
                                selected = isTextMode,
                                onClick = { scope.launch { prefs.setQuranTextMode(true) } },
                                label = { Text("النص المكبّر", fontSize = 12.sp) },
                                modifier = Modifier.testTag("quran_mode_text_chip")
                            )

                            FilledTonalIconButton(
                                onClick = zoomOutAction,
                                enabled = if (isTextMode) textFontSp > 18f else zoomScale > 1.01f,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("quran_zoom_out")
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "تصغير الصفحة")
                            }

                            AssistChip(
                                onClick = resetZoomAction,
                                label = {
                                    Text(
                                        if (isTextMode) "${textFontSp.toInt()}sp" else "${(zoomScale * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )

                            FilledTonalIconButton(
                                onClick = zoomInAction,
                                enabled = if (isTextMode) textFontSp < 48f else zoomScale < 3.8f,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("quran_zoom_in")
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "تكبير الصفحة")
                            }

                            AssistChip(
                                onClick = {
                                    scope.launch { prefs.setQuranReaderTheme((themeIndex + 1) % 3) }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = {
                                    Text(
                                        when (themeIndex) {
                                            1 -> "ورقي"
                                            2 -> "ليلي"
                                            else -> "عادي"
                                        },
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.testTag("quran_theme_button")
                            )

                            AssistChip(
                                onClick = {
                                    pageInput = (pageIndex + 1).toString()
                                    showJumpDialog = true
                                },
                                label = { Text("ص ${pageIndex + 1}", fontSize = 12.sp) },
                                modifier = Modifier.testTag("quran_jump_page_chip")
                            )
                        }
                    }
                }

                // مساحة القراءة الرئيسية (أكبر مساحة ممكنة)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(readerBgColor)
                        .clipToBounds()
                ) {
                    if (isTextMode) {
                        ScalableUthmaniTextView(
                            surahNumber = activeSurahNumber,
                            standardPage = approxStandardPage,
                            textFontSp = textFontSp,
                            bgColor = readerBgColor,
                            textColor = readerTextColor,
                            accentColor = readerAccentColor,
                            favorites = favorites,
                            onToggleFavorite = onToggleFavorite,
                            onPinchFontChange = { nextSp ->
                                scope.launch { prefs.setQuranTextFontSp(nextSp) }
                            },
                            onToggleOverlay = {
                                if (isFullScreen) showFullScreenOverlay = !showFullScreenOverlay
                            },
                            onCopyAyah = { ayah ->
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("آية كريمة", "${ayah.text} ﴿${ayah.number}﴾"))
                                Toast.makeText(context, "تم نسخ الآية الكريمة", Toast.LENGTH_SHORT).show()
                            },
                            onShareAyah = { ayah ->
                                val name = QuranRepository.surah(ayah.surah)?.name ?: ""
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "﴿${ayah.text}﴾\n[سورة $name: ${ayah.number}]")
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الآية الكريمة"))
                                }
                            }
                        )
                    } else {
                        ZoomableMushafPdfCanvas(
                            bitmap = bitmap,
                            isLoading = isLoading,
                            error = error,
                            pageIndex = pageIndex,
                            pageCount = pageCount,
                            zoomScale = zoomScale,
                            panOffset = panOffset,
                            colorFilter = pdfColorFilter,
                            onViewportChanged = { mushafViewportSize = it },
                            onTransform = { nextScale, deltaPan ->
                                val clampedScale = nextScale.coerceIn(1f, 3.8f)
                                zoomScale = clampedScale
                                panOffset = clampPan(panOffset + deltaPan, clampedScale)
                            },
                            onDoubleTapToggle = {
                                val targetScale = if (zoomScale > 1.2f) 1f else 1.9f
                                zoomScale = targetScale
                                if (targetScale <= 1.02f) panOffset = Offset.Zero
                            },
                            onSingleTap = {
                                if (isFullScreen) showFullScreenOverlay = !showFullScreenOverlay
                            },
                            onSwipeNextPage = {
                                if (pageIndex < pageCount - 1) {
                                    pageIndex += 1
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                }
                            },
                            onSwipePrevPage = {
                                if (pageIndex > 0) {
                                    pageIndex -= 1
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                }
                            }
                        )
                    }
                }

                // شريط التنقل السفلي في الوضع العادي
                if (!isFullScreen) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilledTonalButton(
                                onClick = goPrevPage,
                                enabled = if (isTextMode && activeSurahNumber > 0) activeSurahNumber > 1 else pageIndex > 0,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.NavigateBefore, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (isTextMode && activeSurahNumber > 0) "السورة السابقة" else "السابقة", fontSize = 12.sp)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isTextMode && surahInfo != null) {
                                        "${surahInfo.number}/114"
                                    } else {
                                        "${pageIndex + 1} / ${pageCount.coerceAtLeast(0)}"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        pageInput = (pageIndex + 1).toString()
                                        showJumpDialog = true
                                    }
                                )
                            }

                            FilledTonalButton(
                                onClick = goNextPage,
                                enabled = if (isTextMode && activeSurahNumber > 0) activeSurahNumber < 114 else pageIndex < pageCount - 1,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(if (isTextMode && activeSurahNumber > 0) "السورة التالية" else "التالية", fontSize = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.NavigateNext, contentDescription = null)
                            }
                        }
                    }
                }
            }

            // شريط التحكم العائم في وضع ملء الشاشة (Full-Screen Floating Controls)
            AnimatedVisibility(
                visible = isFullScreen && showFullScreenOverlay,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp, start = 12.dp, end = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xD9122E26),
                    contentColor = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = goPrevPage,
                            enabled = if (isTextMode && activeSurahNumber > 0) activeSurahNumber > 1 else pageIndex > 0
                        ) {
                            Icon(Icons.Default.NavigateBefore, contentDescription = "السابقة")
                        }
                        IconButton(
                            onClick = zoomOutAction,
                            enabled = if (isTextMode) textFontSp > 18f else zoomScale > 1.01f
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "تصغير")
                        }
                        Text(
                            text = if (isTextMode) "${textFontSp.toInt()}sp" else "${(zoomScale * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { resetZoomAction() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                        IconButton(
                            onClick = zoomInAction,
                            enabled = if (isTextMode) textFontSp < 48f else zoomScale < 3.8f
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "تكبير")
                        }
                        IconButton(
                            onClick = goNextPage,
                            enabled = if (isTextMode && activeSurahNumber > 0) activeSurahNumber < 114 else pageIndex < pageCount - 1
                        ) {
                            Icon(Icons.Default.NavigateNext, contentDescription = "التالية")
                        }
                        IconButton(
                            onClick = { isFullScreen = false },
                            modifier = Modifier.testTag("quran_exit_fullscreen")
                        ) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = "إنهاء ملء الشاشة", tint = Color(0xFFFFD36A))
                        }
                    }
                }
            }
        }
    }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("الانتقال إلى صفحة في المصحف", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "أدخل رقم الصفحة من 1 إلى ${pageCount.coerceAtLeast(1)}:",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter(Char::isDigit).take(4) },
                        label = { Text("رقم الصفحة") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val entered = pageInput.toIntOrNull()
                        if (entered != null && entered in 1..pageCount) {
                            pageIndex = entered - 1
                            zoomScale = 1f
                            panOffset = Offset.Zero
                            if (isTextMode) activeSurahNumber = 0
                        }
                        showJumpDialog = false
                    }
                ) { Text("انتقال") }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun ZoomableMushafPdfCanvas(
    bitmap: Bitmap?,
    isLoading: Boolean,
    error: String?,
    pageIndex: Int,
    pageCount: Int,
    zoomScale: Float,
    panOffset: Offset,
    colorFilter: ColorFilter?,
    onViewportChanged: (IntSize) -> Unit,
    onTransform: (Float, Offset) -> Unit,
    onDoubleTapToggle: () -> Unit,
    onSingleTap: () -> Unit,
    onSwipeNextPage: () -> Unit,
    onSwipePrevPage: () -> Unit
) {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val currentScale by rememberUpdatedState(zoomScale)
    val currentPageIndex by rememberUpdatedState(pageIndex)
    val currentPageCount by rememberUpdatedState(pageCount)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged(onViewportChanged)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTapToggle() },
                    onTap = { onSingleTap() }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var didPinch = false
                    var dragX = 0f
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2) {
                            didPinch = true
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                onTransform(currentScale * zoomChange, panChange)
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (pressedCount == 1 && !didPinch) {
                            val pan = event.calculatePan()
                            if (currentScale > 1.04f) {
                                if (pan != Offset.Zero) {
                                    onTransform(currentScale, pan)
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            } else {
                                dragX += pan.x
                                if (abs(dragX) > touchSlop) {
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (!didPinch && currentScale <= 1.04f) {
                        if (dragX < -55f && currentPageIndex < currentPageCount - 1) {
                            onSwipeNextPage()
                        } else if (dragX > 55f && currentPageIndex > 0) {
                            onSwipePrevPage()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            error != null -> Text(
                error,
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            bitmap != null -> Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "صفحة كاملة من المصحف رقم ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    },
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter
            )
            isLoading -> CircularProgressIndicator()
            else -> CircularProgressIndicator()
        }
    }
}

@Composable
private fun ScalableUthmaniTextView(
    surahNumber: Int,
    standardPage: Int,
    textFontSp: Float,
    bgColor: Color,
    textColor: Color,
    accentColor: Color,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onPinchFontChange: (Float) -> Unit,
    onToggleOverlay: () -> Unit,
    onCopyAyah: (Ayah) -> Unit,
    onShareAyah: (Ayah) -> Unit
) {
    val ayahs = remember(surahNumber, standardPage, QuranRepository.isLoaded) {
        if (surahNumber > 0) {
            QuranRepository.ayahsOf(surahNumber)
        } else {
            QuranRepository.byPage(standardPage)
        }
    }
    val surahInfo = remember(surahNumber) {
        if (surahNumber > 0) QuranRepository.surah(surahNumber) else null
    }
    var selectedAyahKey by remember { mutableStateOf<String?>(null) }
    val currentFontSp by rememberUpdatedState(textFontSp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2) {
                            val zoomChange = event.calculateZoom()
                            if (zoomChange != 1f) {
                                onPinchFontChange((currentFontSp * zoomChange).coerceIn(18f, 48f))
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onToggleOverlay() })
            }
    ) {
        if (ayahs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                surahInfo?.let { "سورة ${it.name}" } ?: "صفحة $standardPage من المصحف",
                                fontSize = (textFontSp * 0.8f).coerceIn(18f, 30f).sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                textAlign = TextAlign.Center
                            )
                            if (surahNumber != 9 && surahNumber != 1) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    fontSize = (textFontSp * 0.88f).coerceIn(19f, 36f).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(ayahs, key = { it.key }) { ayah ->
                    val isSelected = selectedAyahKey == ayah.key
                    val favKey = "quran:${ayah.key}"
                    val isFav = favorites.contains(favKey)
                    val isPlaying = TtsManager.isSpeaking

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAyahKey = if (isSelected) null else ayah.key
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) accentColor.copy(alpha = 0.10f) else Color.Transparent
                        )
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                text = "${ayah.text} ﴿${ayah.number}﴾",
                                fontSize = textFontSp.sp,
                                lineHeight = (textFontSp * 1.65f).sp,
                                color = textColor,
                                textAlign = TextAlign.Start
                            )
                            AnimatedVisibility(visible = isSelected) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { TtsManager.toggle(ayah.text) }) {
                                        Icon(
                                            if (isPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                            contentDescription = "استمع للآية",
                                            tint = accentColor
                                        )
                                    }
                                    IconButton(onClick = { onCopyAyah(ayah) }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الآية", tint = accentColor)
                                    }
                                    IconButton(onClick = { onShareAyah(ayah) }) {
                                        Icon(Icons.Default.Share, contentDescription = "مشاركة الآية", tint = accentColor)
                                    }
                                    IconButton(onClick = { onToggleFavorite(favKey) }) {
                                        Icon(
                                            if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "حفظ في المفضلة",
                                            tint = if (isFav) Color(0xFFE0A800) else accentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private class BundledMushaf(context: Context) : Closeable {
    private val file = File(context.cacheDir, "remoazkar-mushaf.pdf")
    private val descriptor: ParcelFileDescriptor
    private val renderer: PdfRenderer

    init {
        if (!file.exists() || file.length() == 0L) {
            context.assets.open("mushaf.pdf").use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
        }
        descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(descriptor)
    }

    val pageCount: Int get() = renderer.pageCount

    @Synchronized
    fun render(index: Int, maxWidth: Int): Bitmap {
        val page = renderer.openPage(index.coerceIn(0, renderer.pageCount - 1))
        try {
            val width = maxWidth.coerceIn(480, 2400)
            val height = (width.toFloat() * page.height / page.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } finally {
            page.close()
        }
    }

    override fun close() {
        renderer.close()
        descriptor.close()
    }
}
