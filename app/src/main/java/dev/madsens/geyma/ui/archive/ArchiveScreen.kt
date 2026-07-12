package dev.madsens.geyma.ui.archive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.files.ArchiveNode
import dev.madsens.geyma.files.ArchiveRecord
import dev.madsens.geyma.files.ArchiveTree
import dev.madsens.geyma.files.InAppViewer
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.files.ZipIo
import dev.madsens.geyma.theme.DANGER
import dev.madsens.geyma.theme.LocalTheme
import dev.madsens.geyma.theme.itemColors
import dev.madsens.geyma.ui.browser.openFile
import dev.madsens.geyma.ui.components.EmptyState
import dev.madsens.geyma.ui.components.geymaShape
import dev.madsens.geyma.ui.components.kindIcon
import dev.madsens.geyma.ui.viewer.ViewerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Browse a zip-format archive as if it were a folder: navigate its internal
 * directories, tap a file to preview it in the in-app viewer (extracted to a
 * cache scratch file), or extract the whole thing next to the archive. Reading
 * is read-only; the only mutation — "Extract all" — goes through
 * FsRepository.extractArchive so it lands in the journal.
 */
@Composable
fun ArchiveScreen(
    app: GeymaApp,
    archivePath: String,
    onBack: () -> Unit,
    onExtracted: (String) -> Unit,
) {
    val t = LocalTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var records by remember(archivePath) { mutableStateOf<List<ArchiveRecord>?>(null) }
    var loadError by remember(archivePath) { mutableStateOf(false) }
    var currentDir by remember(archivePath) { mutableStateOf("") }
    var previewPath by remember(archivePath) { mutableStateOf<String?>(null) }
    var busyMessage by remember(archivePath) { mutableStateOf<String?>(null) }
    var error by remember(archivePath) { mutableStateOf<String?>(null) }

    LaunchedEffect(archivePath) {
        val result = runCatching { withContext(Dispatchers.IO) { ZipIo.readRecords(archivePath) } }
        records = result.getOrNull()
        loadError = result.isFailure
    }

    // Preview overlay sits above the listing; back dismisses just the preview.
    previewPath?.let { path ->
        BackHandler { previewPath = null }
        ViewerScreen(
            app = app,
            initialPath = path,
            onBack = { previewPath = null },
            recordOpen = false,
        )
        return
    }

    BackHandler {
        if (currentDir.isEmpty()) onBack() else currentDir = parentInternalDir(currentDir)
    }

    fun openNode(node: ArchiveNode) {
        if (node.isDir) {
            currentDir = node.path
            return
        }
        busyMessage = "Opening ${node.name}…"
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val scratch = File(context.cacheDir, "archive-preview")
                    ZipIo.extractEntry(archivePath, node.path, File(scratch, node.name))
                }
            }
            busyMessage = null
            val file = result.getOrNull()
            if (file == null) {
                error = "Couldn't read ${node.name} from the archive."
            } else if (InAppViewer.canView(file.name, file.length())) {
                previewPath = file.absolutePath
            } else {
                openFile(context, file.absolutePath)
            }
        }
    }

    fun extractAll() {
        val parent = PathUtils.parentOf(archivePath) ?: return
        busyMessage = "Extracting…"
        scope.launch {
            val result = app.repo.extractArchive(archivePath, parent)
            busyMessage = null
            result.fold(
                onSuccess = { onExtracted(it) },
                onFailure = { error = it.message ?: "Extraction failed." },
            )
        }
    }

    Column(Modifier.fillMaxSize().background(t.bg)) {
        ArchiveBar(
            title = PathUtils.nameOf(archivePath),
            onBack = onBack,
            onExtractAll = ::extractAll,
        )
        ArchiveBreadcrumbs(
            archiveName = PathUtils.nameOf(archivePath),
            currentDir = currentDir,
            onNavigate = { currentDir = it },
        )

        error?.let { message ->
            Text(
                message,
                color = DANGER,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(geymaShape())
                    .background(DANGER.copy(alpha = 0.15f))
                    .clickable { error = null }
                    .padding(10.dp),
            )
        }

        Box(Modifier.fillMaxSize()) {
            when {
                loadError -> EmptyState("Couldn't open this archive.")
                records == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = t.accent)
                }
                else -> {
                    val children = remember(records, currentDir) {
                        ArchiveTree.childrenOf(records!!, currentDir)
                    }
                    if (children.isEmpty()) {
                        EmptyState("This folder is empty.")
                    } else {
                        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                            items(children, key = { it.path }) { node ->
                                ArchiveRow(node, onClick = { openNode(node) })
                            }
                        }
                    }
                }
            }

            busyMessage?.let { message ->
                Box(
                    Modifier.fillMaxSize().background(t.bg.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = t.accent)
                        Spacer(Modifier.size(12.dp))
                        Text(message, color = t.inkSoft, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveBar(title: String, onBack: () -> Unit, onExtractAll: () -> Unit) {
    val t = LocalTheme.current
    Row(
        Modifier.fillMaxWidth().background(t.surface).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = t.ink)
        }
        Text(
            title,
            color = t.ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onExtractAll) {
            Icon(Icons.Filled.Unarchive, "Extract all", tint = t.accent)
        }
    }
}

@Composable
private fun ArchiveBreadcrumbs(archiveName: String, currentDir: String, onNavigate: (String) -> Unit) {
    val t = LocalTheme.current
    // Build (label, internalDir) crumbs from "a/b/" → root, a/, a/b/.
    val crumbs = remember(archiveName, currentDir) {
        val list = mutableListOf(archiveName to "")
        if (currentDir.isNotEmpty()) {
            val segments = currentDir.trimEnd('/').split('/')
            val builder = StringBuilder()
            for (segment in segments) {
                builder.append(segment).append('/')
                list.add(segment to builder.toString())
            }
        }
        list
    }
    LazyRow(
        Modifier.fillMaxWidth().background(t.surface).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(crumbs.size) { i ->
            val (label, dir) = crumbs[i]
            val last = i == crumbs.lastIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    color = if (last) t.ink else t.inkSoft,
                    fontWeight = if (last) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onNavigate(dir) }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
                if (!last) {
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateNext,
                        null,
                        tint = t.inkFaint,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveRow(node: ArchiveNode, onClick: () -> Unit) {
    val t = LocalTheme.current
    val colors = itemColors(node.kind, t)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(geymaShape())
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(geymaShape(0.6f)).background(colors.bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(kindIcon(node.kind), null, tint = colors.tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                node.name,
                color = t.ink,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (node.isDir) "Folder" else PathUtils.humanSize(node.size),
                color = t.inkFaint,
                fontSize = 12.sp,
            )
        }
        if (node.isDir) {
            Icon(
                Icons.AutoMirrored.Filled.NavigateNext,
                null,
                tint = t.inkFaint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Parent of an internal archive dir: "a/b/" → "a/", "a/" → "". */
private fun parentInternalDir(dir: String): String {
    val trimmed = dir.trimEnd('/')
    val slash = trimmed.lastIndexOf('/')
    return if (slash < 0) "" else trimmed.substring(0, slash + 1)
}
