package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream

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
    var renderer by remember(context) { mutableStateOf<BundledMushaf?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var pageCount by remember { mutableIntStateOf(0) }
    var pageIndex by remember { mutableIntStateOf((pageNumber - 1).coerceAtLeast(0)) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pageInput by remember { mutableStateOf("") }

    DisposableEffect(renderer) {
        val openedRenderer = renderer
        onDispose { runCatching { openedRenderer?.close() } }
    }

    LaunchedEffect(context) {
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
            if (surahNumber > 0) {
                // pageStart مأخوذ من بيانات المصحف القياسي ذي 604 صفحات؛ الملف المرفق 569 صفحة، لذلك هذا انتقال تقريبي فقط.
                val standardStart = runCatching {
                    com.example.data.QuranRepository.surah(surahNumber)?.pageStart ?: 1
                }.getOrDefault(1)
                pageIndex = (((standardStart - 1).toDouble() * (pageCount - 1).coerceAtLeast(0)) / 603.0)
                    .toInt().coerceIn(0, (pageCount - 1).coerceAtLeast(0))
            }
        }
    }

    LaunchedEffect(renderer, pageIndex, pageCount) {
        val activeRenderer = renderer
        if (activeRenderer != null && pageCount > 0) {
            error = null
            val result = withContext(Dispatchers.IO) {
                runCatching { activeRenderer.render(pageIndex, 1500) }
            }
            bitmap = result.getOrNull()
            error = result.exceptionOrNull()?.let {
                "تعذّر عرض الصفحة ${pageIndex + 1} من ملف المصحف: ${it.localizedMessage ?: "ملف PDF غير صالح"}"
            }
            pageInput = (pageIndex + 1).toString()
        }
    }

    val surahName = remember(surahNumber) {
        if (surahNumber > 0) com.example.data.QuranRepository.surah(surahNumber)?.name else null
    }
    var dragDistance by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(surahName?.let { "سورة $it" } ?: "المصحف الشريف", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            if (surahNumber > 0) "موضع تقريبي؛ تنقّل يدويًا للتأكد من بداية السورة"
                            else "صفحة ${pageIndex + 1} من ${pageCount.coerceAtLeast(0)}",
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
                    IconButton(onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) }, enabled = pageIndex > 0) {
                        Icon(Icons.Default.NavigateBefore, contentDescription = "الصفحة السابقة")
                    }
                    IconButton(onClick = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) }, enabled = pageIndex < pageCount - 1) {
                        Icon(Icons.Default.NavigateNext, contentDescription = "الصفحة التالية")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter(Char::isDigit).take(4) },
                    modifier = Modifier.width(108.dp),
                    label = { Text("رقم الصفحة") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(onClick = {
                    val entered = pageInput.toIntOrNull()
                    if (entered != null && entered in 1..pageCount) pageIndex = entered - 1
                }, enabled = pageCount > 0) { Text("اذهب") }
                Text("${pageIndex + 1} / ${pageCount.coerceAtLeast(0)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth().pointerInput(pageIndex, pageCount) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragDistance < -40f) pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1)
                            if (dragDistance > 40f) pageIndex = (pageIndex - 1).coerceAtLeast(0)
                            dragDistance = 0f
                        },
                        onHorizontalDrag = { change, amount ->
                            dragDistance += amount
                            change.consume()
                        }
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when {
                        error != null -> Text(error!!, modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
                        bitmap != null -> Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "صفحة كاملة من المصحف رقم ${pageIndex + 1}",
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            alignment = Alignment.Center,
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                        isLoading -> CircularProgressIndicator()
                        else -> CircularProgressIndicator()
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("اسحب أفقيًا أو استخدم الأسهم لتقليب الصفحات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
            val width = maxWidth.coerceIn(320, 1800)
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
