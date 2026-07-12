package dev.madsens.geyma.files

import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * Read-only zip helpers for browsing and previewing archive contents. These
 * touch the app's own cache (for previews), not the user's storage, so unlike
 * real mutations they don't go through the journal. Extraction *into* storage
 * lives in [FsRepository.extractArchive] so it stays logged.
 */
object ZipIo {
    fun readRecords(archivePath: String): List<ArchiveRecord> {
        val out = ArrayList<ArchiveRecord>()
        ZipFile(File(archivePath)).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                out.add(ArchiveRecord(entry.name, if (entry.isDirectory) 0L else entry.size.coerceAtLeast(0L)))
            }
        }
        return out
    }

    /** Extract one entry to [destFile] (creating parents), returning it. */
    fun extractEntry(archivePath: String, entryName: String, destFile: File): File {
        ZipFile(File(archivePath)).use { zip ->
            val entry = zip.getEntry(entryName) ?: throw IOException("Entry not found: $entryName")
            destFile.parentFile?.mkdirs()
            zip.getInputStream(entry).use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return destFile
    }
}
