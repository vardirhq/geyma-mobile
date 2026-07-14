package dev.madsens.geyma

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import dev.madsens.geyma.continuity.Continuity
import dev.madsens.geyma.data.GeymaDb
import dev.madsens.geyma.data.Prefs
import dev.madsens.geyma.files.FsRepository
import dev.madsens.geyma.files.OcrIndexer
import dev.madsens.geyma.files.StorageWatcher
import dev.madsens.geyma.insights.DuplicateGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GeymaApp : Application(), ImageLoaderFactory {
    val db: GeymaDb by lazy { GeymaDb.get(this) }
    val repo: FsRepository by lazy { FsRepository(db) }
    val prefs: Prefs by lazy { Prefs(this) }

    /** Lives as long as the process — for storage watching that outlasts screens. */
    val appScope: CoroutineScope by lazy { CoroutineScope(SupervisorJob()) }

    val watcher: StorageWatcher by lazy { StorageWatcher(repo, prefs, appScope) }
    val continuity: Continuity by lazy { Continuity(this, db) }
    val ocrIndexer: OcrIndexer by lazy { OcrIndexer(this, db) }

    /**
     * The last Echoes scan, kept for the life of the process so stepping into a
     * file's dossier and back doesn't trigger a full rescan. Invalidated when
     * echoes are cleared or the user asks to rescan.
     */
    @Volatile
    var echoesCache: List<DuplicateGroup>? = null

    override fun onCreate() {
        super.onCreate()
        // Keep the journal from growing without bound; the retention window is
        // deliberately long so Geyma still "remembers."
        appScope.launch(Dispatchers.IO) { repo.pruneJournal() }
    }

    /**
     * Coil's singleton loader, taught to decode animated GIFs and vector SVGs so
     * they render (and animate) both in thumbnails and the in-app image viewer.
     * ImageDecoderDecoder gives hardware-accelerated animation on API 28+; the
     * older GifDecoder covers 26–27.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .build()
}
