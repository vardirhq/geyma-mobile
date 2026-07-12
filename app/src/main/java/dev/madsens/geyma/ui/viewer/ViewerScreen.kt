package dev.madsens.geyma.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.files.InAppViewer
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.files.ViewerKind
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.ui.browser.openFile
import dev.madsens.geyma.ui.browser.shareFile
import java.io.File

/**
 * Full-screen in-app viewer. Dispatches on the file's [ViewerKind] so images,
 * video, audio, PDFs and text/code open inside Geyma instead of bouncing out to
 * a chooser. The current path is state so the image gallery can swipe between
 * siblings and keep the title bar (and journal) in step. An "open with another
 * app" affordance is always present so nothing is ever a dead end.
 */
@Composable
fun ViewerScreen(
    app: GeymaApp,
    initialPath: String,
    onBack: () -> Unit,
) {
    val t = LocalTheme.current
    val context = LocalContext.current
    var path by remember(initialPath) { mutableStateOf(initialPath) }
    val name = PathUtils.nameOf(path)
    val kind = remember(path) { InAppViewer.kindFor(name, File(path).length()) }

    // Viewing a file counts as opening it — record it just like an external open.
    LaunchedEffect(path) { app.repo.recordOpen(path) }

    Column(Modifier.fillMaxSize().background(t.bg)) {
        ViewerBar(
            name = name,
            onBack = onBack,
            onShare = { shareFile(context, path) },
            onExternal = { openFile(context, path) },
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (kind) {
                ViewerKind.IMAGE -> ImageViewer(app, path) { path = it }
                ViewerKind.VIDEO -> VideoViewer(path)
                ViewerKind.AUDIO -> AudioViewer(path, name)
                ViewerKind.PDF -> PdfViewer(path)
                ViewerKind.TEXT -> TextViewer(path)
                ViewerKind.NONE -> ViewerMessage(
                    "Geyma can't preview this one.",
                    action = "Open with another app",
                    onAction = { openFile(context, path) },
                )
            }
        }
    }
}

@Composable
private fun ViewerBar(
    name: String,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onExternal: () -> Unit,
) {
    val t = LocalTheme.current
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().background(t.surface).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.ink)
        }
        Text(
            name,
            color = t.ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onShare) {
            Icon(Icons.Filled.Share, "Share", tint = t.inkSoft)
        }
        IconButton(onClick = onExternal) {
            Icon(Icons.Filled.OpenInNew, "Open with another app", tint = t.inkSoft)
        }
    }
}

/** Shared centered message for empty/error/unsupported states, with one action. */
@Composable
internal fun ViewerMessage(
    text: String,
    icon: ImageVector? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val t = LocalTheme.current
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = t.inkFaint, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
        }
        Text(text, color = t.inkSoft, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.TextButton(onClick = onAction) {
                Text(action, color = t.accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
