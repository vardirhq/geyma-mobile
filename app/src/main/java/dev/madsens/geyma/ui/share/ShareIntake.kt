package dev.madsens.geyma.ui.share

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dev.madsens.geyma.GeymaApp
import dev.madsens.geyma.files.PathUtils
import dev.madsens.geyma.files.StorageRoots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Turns files shared *into* Geyma from other apps into real, journaled files.
 * They land in a "Geyma Inbox" folder, are recorded as arrivals with their
 * source, and the caller then offers to drop them into a working set — so the
 * share sheet becomes a front door for building sets.
 */
object ShareIntake {

    suspend fun intake(context: Context, app: GeymaApp, uris: List<Uri>, fromApp: String?): List<String> =
        withContext(Dispatchers.IO) {
            if (uris.isEmpty()) return@withContext emptyList()
            val inbox = File(StorageRoots.primaryPath(), "Download/Geyma Inbox").apply { mkdirs() }
            val detail = if (fromApp.isNullOrBlank()) "shared to Geyma" else "shared from $fromApp"
            val paths = ArrayList<String>()
            for (uri in uris) {
                val name = displayName(context, uri) ?: "shared-${System.currentTimeMillis()}"
                val unique = PathUtils.uniqueChildName(inbox.list()?.toSet() ?: emptySet(), name)
                val dest = File(inbox, unique)
                val ok = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    } != null
                }.getOrDefault(false)
                if (ok && dest.exists()) {
                    app.repo.recordArrival(dest.absolutePath, detail)
                    paths.add(dest.absolutePath)
                }
            }
            paths
        }

    private fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else {
                    null
                }
            }
        }.getOrNull() ?: uri.lastPathSegment
    }
}
