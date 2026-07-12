package dev.madsens.geyma.ui.viewer

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.madsens.geyma.theme.LocalTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders a PDF page-by-page with the framework's [PdfRenderer] — no third-party
 * dependency. Pages render lazily to bitmaps sized to the viewport width, and a
 * lock serializes access because a renderer may only hold one page open at a time.
 */
@Composable
fun PdfViewer(path: String) {
    val t = LocalTheme.current
    val renderLock = remember(path) { Any() }
    var renderer by remember(path) { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember(path) { mutableIntStateOf(0) }
    var failed by remember(path) { mutableStateOf(false) }

    DisposableEffect(path) {
        var descriptor: ParcelFileDescriptor? = null
        runCatching {
            descriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
            val r = PdfRenderer(descriptor!!)
            renderer = r
            pageCount = r.pageCount
        }.onFailure { failed = true }
        onDispose {
            synchronized(renderLock) {
                runCatching { renderer?.close() }
            }
            runCatching { descriptor?.close() }
        }
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val targetWidthPx = with(density) { (configuration.screenWidthDp.dp - 16.dp).toPx() }.toInt()

    when {
        failed -> ViewerMessage("Couldn't open this PDF.", icon = Icons.Filled.PictureAsPdf)
        renderer == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = t.accent)
        }
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(pageCount) { index ->
                PdfPage(renderer, renderLock, index, targetWidthPx)
            }
        }
    }
}

@Composable
private fun PdfPage(renderer: PdfRenderer?, lock: Any, index: Int, targetWidthPx: Int) {
    val t = LocalTheme.current
    var bitmap by remember(renderer, index, targetWidthPx) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(renderer, index, targetWidthPx) {
        if (renderer == null || targetWidthPx <= 0) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                synchronized(lock) {
                    renderer.openPage(index).use { page ->
                        val scale = targetWidthPx.toFloat() / page.width
                        val heightPx = (page.height * scale).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(targetWidthPx, heightPx, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(AndroidColor.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }.getOrNull()
        }
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Page ${index + 1}",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Box(Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = t.accent)
        }
    }
}
