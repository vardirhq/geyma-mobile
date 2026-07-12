package dev.madsens.geyma.ui.viewer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.files.FileKind
import dev.madsens.geyma.files.InAppViewer
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.theme.LocalTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads a text or code file (capped at [InAppViewer.MAX_TEXT_BYTES]) and shows it
 * in a selectable, scrollable pane — monospaced for code/markup, proportional for
 * prose. The read happens off the main thread.
 */
@Composable
fun TextViewer(path: String) {
    val t = LocalTheme.current
    var text by remember(path) { mutableStateOf<String?>(null) }
    var failed by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path) {
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val file = File(path)
                val cap = minOf(file.length(), InAppViewer.MAX_TEXT_BYTES).toInt().coerceAtLeast(0)
                val buffer = ByteArray(cap)
                file.inputStream().use { input ->
                    var read = 0
                    while (read < buffer.size) {
                        val n = input.read(buffer, read, buffer.size - read)
                        if (n < 0) break
                        read += n
                    }
                    String(buffer, 0, read, Charsets.UTF_8)
                }
            }
        }
        text = result.getOrNull()
        failed = result.isFailure
    }

    when {
        failed -> ViewerMessage("Couldn't read this file as text.")
        text == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = t.accent)
        }
        text!!.isEmpty() -> ViewerMessage("This file is empty.")
        else -> {
            val name = PathUtils.nameOf(path)
            val mono = FileKind.ofName(name, isDir = false) == FileKind.CODE ||
                name.endsWith(".svg", ignoreCase = true)
            SelectionContainer {
                Text(
                    text = text!!,
                    color = t.ink,
                    fontSize = 13.sp,
                    fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(14.dp),
                )
            }
        }
    }
}
