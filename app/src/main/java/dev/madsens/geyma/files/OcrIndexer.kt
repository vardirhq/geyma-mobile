package dev.madsens.geyma.files

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dev.madsens.geyma.data.GeymaDb
import dev.madsens.geyma.data.OcrText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Reads text out of images with the on-device recognizer and banks it in the
 * `ocr_index` table so pictures become findable by their contents. Everything
 * runs locally — the bundled ML Kit model needs no network, keeping Geyma's
 * on-device promise intact.
 *
 * Indexing is incremental: an image already indexed at its current size and
 * mtime is skipped, so re-scanning is cheap and only new or changed pictures
 * are read.
 */
class OcrIndexer(private val context: Context, private val db: GeymaDb) {

    private val dao get() = db.ocr()

    data class Progress(val done: Int, val total: Int, val indexed: Int)

    /**
     * Walk [roots] (recursively), recognizing text in every eligible image, and
     * return how many were freshly indexed. [onProgress] is called on a
     * background thread so the caller can show it isn't frozen. Cooperative:
     * respects cancellation of the calling coroutine.
     */
    suspend fun indexRoots(
        roots: List<String>,
        onProgress: (Progress) -> Unit = {},
    ): Int = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val candidates = collectImages(roots)
            var done = 0
            var indexed = 0
            for (file in candidates) {
                coroutineContext.ensureActive()
                done++
                val existing = dao.byPath(file.absolutePath)
                if (existing == null || existing.modifiedMs != file.lastModified() || existing.size != file.length()) {
                    val text = runCatching { recognize(recognizer, file) }.getOrNull()
                    if (!text.isNullOrBlank()) {
                        dao.set(
                            OcrText(
                                path = file.absolutePath,
                                text = text,
                                modifiedMs = file.lastModified(),
                                size = file.length(),
                            ),
                        )
                        indexed++
                    }
                }
                if (done % 3 == 0 || done == candidates.size) {
                    onProgress(Progress(done, candidates.size, indexed))
                }
            }
            onProgress(Progress(done, candidates.size, indexed))
            indexed
        } finally {
            recognizer.close()
        }
    }

    private fun recognize(recognizer: TextRecognizer, file: File): String {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        return Tasks.await(recognizer.process(image)).text
    }

    /** Eligible images under [roots], de-duplicated and capped to bound a run. */
    private fun collectImages(roots: List<String>): List<File> {
        val out = ArrayList<File>()
        val seen = HashSet<String>()
        for (root in roots) {
            val stack = ArrayDeque<File>()
            stack.addLast(File(root))
            while (stack.isNotEmpty() && out.size < MAX_FILES) {
                val dir = stack.removeLast()
                val kids = dir.listFiles() ?: continue
                for (f in kids) {
                    if (f.name.startsWith(".")) continue // skip hidden + Geyma's own tree
                    when {
                        f.isDirectory -> stack.addLast(f)
                        Ocr.canIndex(f.name, f.length()) && seen.add(f.canonicalPath) -> out.add(f)
                    }
                }
            }
        }
        return out
    }

    private companion object {
        /** Ceiling on images inspected per run, so a huge gallery can't run away. */
        const val MAX_FILES = 3000
    }
}
