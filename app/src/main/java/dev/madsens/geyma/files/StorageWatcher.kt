package dev.madsens.geyma.files

import android.os.FileObserver
import dev.madsens.geyma.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/**
 * Watches the folders where files land on a phone and turns fresh files into
 * "arrived" journal entries — the thing that makes Geyma's memory about *your
 * storage*, not just the operations you did inside the app.
 *
 * Two mechanisms cover the two cases:
 *  - a reconcile scan on launch/resume catches everything that arrived while
 *    Geyma was closed (the common case), and
 *  - lightweight [FileObserver]s catch arrivals live while Geyma is open.
 */
class StorageWatcher(
    private val repo: FsRepository,
    private val prefs: Prefs,
    private val scope: CoroutineScope,
) {
    private val observers = mutableListOf<FileObserver>()
    private var onArrival: (() -> Unit)? = null

    fun setOnArrival(cb: () -> Unit) {
        onArrival = cb
    }

    /** Reconcile the ledger with what's on disk. Safe to call repeatedly. */
    fun sync() {
        scope.launch(Dispatchers.IO) {
            val seeded = prefs.arrivalsSeeded.first()
            val arrivals = repo.reconcileArrivals(StorageRoots.watchedFolders(), seeded)
            if (!seeded) prefs.setArrivalsSeeded(true)
            if (arrivals > 0) onArrival?.invoke()
        }
    }

    /** Begin live watching. Idempotent; pair with [stop] on lifecycle changes. */
    @Suppress("DEPRECATION") // String-path FileObserver is the API that works on 26+.
    fun start() {
        if (observers.isNotEmpty()) return
        for (dirPath in StorageRoots.watchedFolders()) {
            if (!File(dirPath).isDirectory) continue
            val observer = object : FileObserver(dirPath, CREATE or MOVED_TO or CLOSE_WRITE) {
                override fun onEvent(event: Int, path: String?) {
                    if (path == null) return
                    // A brief settle before recording avoids catching a file
                    // mid-write; the reconcile scan is the source of truth.
                    sync()
                }
            }
            runCatching { observer.startWatching() }.onSuccess { observers.add(observer) }
        }
    }

    fun stop() {
        observers.forEach { runCatching { it.stopWatching() } }
        observers.clear()
    }
}
