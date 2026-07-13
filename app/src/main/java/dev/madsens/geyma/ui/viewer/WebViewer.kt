package dev.madsens.geyma.ui.viewer

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import dev.madsens.geyma.files.InAppViewer
import dev.madsens.geyma.files.Markdown
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.theme.ResolvedTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val markdownExt = setOf("md", "markdown")

/**
 * Renders HTML and Markdown as a formatted page inside Geyma using the platform
 * [WebView], with scripting, file and network access all disabled — it only ever
 * shows the local bytes we hand it. Markdown is converted to HTML first via the
 * pure [Markdown] converter and wrapped in a skin-themed stylesheet; a raw HTML
 * file is shown as its author styled it (just re-backgrounded to the skin).
 */
@Composable
fun WebViewer(path: String) {
    val t = LocalTheme.current
    var html by remember(path) { mutableStateOf<String?>(null) }
    var failed by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path, t) {
        val result = runCatching {
            withContext(Dispatchers.IO) { readCapped(File(path)) }
        }
        val raw = result.getOrNull()
        failed = result.isFailure
        html = raw?.let { body ->
            val ext = PathUtils.nameOf(path).substringAfterLast('.', "").lowercase()
            if (ext in markdownExt) wrapThemed(Markdown.toHtml(body), t) else body
        }
    }

    when {
        failed -> ViewerMessage("Couldn't read this file.")
        html == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = t.accent)
        }
        else -> AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    @SuppressLint("SetJavaScriptEnabled")
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.blockNetworkLoads = true
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    setBackgroundColor(t.bg.toArgb())
                }
            },
            update = { web ->
                web.setBackgroundColor(t.bg.toArgb())
                web.loadDataWithBaseURL(null, html!!, "text/html; charset=utf-8", "UTF-8", null)
            },
        )
    }
}

/** Reads at most [InAppViewer.MAX_TEXT_BYTES] so an oversized file can't OOM us. */
private fun readCapped(file: File): String {
    val cap = minOf(file.length(), InAppViewer.MAX_TEXT_BYTES).toInt().coerceAtLeast(0)
    val bytes = ByteArray(cap)
    file.inputStream().use { input ->
        var read = 0
        while (read < bytes.size) {
            val n = input.read(bytes, read, bytes.size - read)
            if (n < 0) break
            read += n
        }
        return String(bytes, 0, read, Charsets.UTF_8)
    }
}

private fun hex(c: Color): String = String.format("#%06X", 0xFFFFFF and c.toArgb())

/** Wrap converted-Markdown [body] in a stylesheet that follows the active skin. */
private fun wrapThemed(body: String, t: ResolvedTheme): String {
    val bg = hex(t.bg)
    val ink = hex(t.ink)
    val soft = hex(t.inkSoft)
    val accent = hex(t.accent)
    val border = hex(t.border)
    val card = hex(t.card)
    return """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          html,body{margin:0;padding:16px;background:$bg;color:$ink;
            font-family:-apple-system,Roboto,'Segoe UI',sans-serif;font-size:16px;line-height:1.6;
            overflow-wrap:break-word;word-wrap:break-word;}
          h1,h2,h3,h4,h5,h6{color:$ink;line-height:1.25;margin:1.2em 0 .5em;}
          h1{font-size:1.7em;} h2{font-size:1.4em;} h3{font-size:1.2em;}
          p{margin:.6em 0;} a{color:$accent;text-decoration:none;}
          code{background:$card;color:$soft;padding:.1em .3em;border-radius:4px;
            font-family:ui-monospace,'SFMono-Regular',monospace;font-size:.9em;}
          pre{background:$card;padding:12px;border-radius:8px;overflow-x:auto;}
          pre code{background:transparent;padding:0;color:$ink;}
          blockquote{margin:.6em 0;padding:.2em .9em;border-left:3px solid $border;color:$soft;}
          hr{border:none;border-top:1px solid $border;margin:1.2em 0;}
          ul,ol{padding-left:1.4em;} li{margin:.2em 0;}
          img{max-width:100%;height:auto;}
          table{border-collapse:collapse;} td,th{border:1px solid $border;padding:.3em .6em;}
        </style></head><body>$body</body></html>
    """.trimIndent()
}
