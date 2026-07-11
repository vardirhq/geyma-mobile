package dev.madsens.geyma.ui.browser

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import dev.madsens.geyma.files.FileKind
import java.io.File

private fun uriFor(context: Context, file: File) =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

fun openFile(context: Context, path: String, onOpened: ((String) -> Unit)? = null) {
    val file = File(path)
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(context, file), FileKind.mimeOf(file.name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open ${file.name}"))
        onOpened?.invoke(path)
    }.onFailure {
        Toast.makeText(context, "No app can open ${file.name}", Toast.LENGTH_SHORT).show()
    }
}

/** Share several files at once — the outward face of a working set. */
fun shareFiles(context: Context, paths: List<String>) {
    val uris = ArrayList<android.net.Uri>()
    for (p in paths) {
        val f = File(p)
        if (f.isFile) runCatching { uris.add(uriFor(context, f)) }
    }
    if (uris.isEmpty()) {
        Toast.makeText(context, "Nothing to share", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${uris.size} files"))
    }.onFailure {
        Toast.makeText(context, "Could not share files", Toast.LENGTH_SHORT).show()
    }
}

fun shareFile(context: Context, path: String) {
    val file = File(path)
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = FileKind.mimeOf(file.name)
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    }.onFailure {
        Toast.makeText(context, "Could not share ${file.name}", Toast.LENGTH_SHORT).show()
    }
}
