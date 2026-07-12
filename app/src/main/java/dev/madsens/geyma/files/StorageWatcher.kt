package dev.madsens.geyma.files

import android.os.FileObserver
import dev.madsens.geyma.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    // Only one reconcile runs at a time: concurrent scans both snapshot the
    // seen-ledger and would each log the same file as ARRIVED (the journal
    // insert isn't dedup'd, unlike the SeenFile insert).
    private val reconcileLock = Mutex()

    // A burst of downloads fires many FileObserver events in a row; coalesce
    // them into a single scan instead of one scan storm per event.
    private var debounceJob: Job? = null

    fun setOnArrival(cb: () -> Unit) {
        onArrival = cb
    }

    /** Reconcile the ledger with what's on disk. Safe to call repeatedly. */
    fun sync() {
        scope.launch(Dispatchers.IO) {
            reconcileLock.withLock {
                val seeded = prefs.arrivalsSeeded.first()
                val arrivals = repo.reconcileArrivals(StorageRoots.watchedFolders(), seeded)
                if (!seeded) prefs.setArrivalsSeeded(true)
                if (arrivals > 0) onArrival?.invoke()
            }
        }
    }

    /** Coalesce a burst of live events into one settled scan. */
    @Synchronized
    private fun scheduleSync() {
        debounceJob?.cancel()
        debounceJob = scope.launch(Dispatchers.IO) {
            // A brief settle before recording avoids catching a file mid-write
            // and lets a download burst quiet down; the reconcile is the source
            // of truth, so a coalesced scan loses nothing.
            delay(SETTLE_MS)
            sync()
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
                    scheduleSync()
                }
            }
            runCatching { observer.startWatching() }.onSuccess { observers.add(observer) }
        }
    }

    fun stop() {
        observers.forEach { runCatching { it.stopWatching() } }
        observers.clear()
    }

    private companion object {
        const val SETTLE_MS = 750L
    }
}
