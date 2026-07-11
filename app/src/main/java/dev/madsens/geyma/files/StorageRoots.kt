package dev.madsens.geyma.files

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File

data class StorageRoot(
    val label: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val removable: Boolean,
)

object StorageRoots {

    fun primaryPath(): String = Environment.getExternalStorageDirectory().absolutePath

    fun list(context: Context): List<StorageRoot> {
        val roots = mutableListOf<StorageRoot>()
        val primary = Environment.getExternalStorageDirectory()
        roots.add(rootOf(primary, "Internal storage", removable = false))

        // Removable volumes: walk up from each app-specific dir to the volume root.
        for (dir in context.getExternalFilesDirs(null).filterNotNull()) {
            val volume = dir.parentFile?.parentFile?.parentFile?.parentFile ?: continue
            if (volume.absolutePath == primary.absolutePath) continue
            if (!volume.canRead()) continue
            roots.add(rootOf(volume, volume.name.ifEmpty { "SD card" }, removable = true))
        }
        return roots
    }

    private fun rootOf(dir: File, label: String, removable: Boolean): StorageRoot {
        val stat = runCatching { StatFs(dir.absolutePath) }.getOrNull()
        return StorageRoot(
            label = label,
            path = dir.absolutePath,
            totalBytes = stat?.totalBytes ?: 0,
            freeBytes = stat?.availableBytes ?: 0,
            removable = removable,
        )
    }

    fun hasFullAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            android.content.pm.PackageManager.PERMISSION_GRANTED ==
                context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

    /** Well-known folders surfaced as quick access chips on Home. */
    fun quickAccess(): List<Pair<String, String>> {
        val base = primaryPath()
        return listOf(
            "Downloads" to "$base/Download",
            "Documents" to "$base/Documents",
            "Pictures" to "$base/Pictures",
            "Camera" to "$base/DCIM",
            "Music" to "$base/Music",
            "Movies" to "$base/Movies",
        )
    }

    /**
     * Folders Geyma watches for arrivals — where files land on a phone without
     * anyone filing them: downloads, screenshots, camera, and messaging media.
     * Only the ones that actually exist are returned.
     */
    fun watchedFolders(): List<String> {
        val base = primaryPath()
        return listOf(
            "$base/Download",
            "$base/Downloads",
            "$base/Documents",
            "$base/Pictures",
            "$base/Pictures/Screenshots",
            "$base/DCIM/Screenshots",
            "$base/DCIM/Camera",
            "$base/Movies",
            "$base/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents",
            "$base/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images",
            "$base/bluetooth",
            "$base/Telegram",
        ).filter { File(it).isDirectory }
    }
}
