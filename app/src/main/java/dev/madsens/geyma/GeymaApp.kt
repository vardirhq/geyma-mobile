package dev.madsens.geyma

import android.app.Application
import dev.madsens.geyma.continuity.Continuity
import dev.madsens.geyma.data.GeymaDb
import dev.madsens.geyma.data.Prefs
import dev.madsens.geyma.files.FsRepository
import dev.madsens.geyma.files.StorageWatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GeymaApp : Application() {
    val db: GeymaDb by lazy { GeymaDb.get(this) }
    val repo: FsRepository by lazy { FsRepository(db) }
    val prefs: Prefs by lazy { Prefs(this) }

    /** Lives as long as the process — for storage watching that outlasts screens. */
    val appScope: CoroutineScope by lazy { CoroutineScope(SupervisorJob()) }

    val watcher: StorageWatcher by lazy { StorageWatcher(repo, prefs, appScope) }
    val continuity: Continuity by lazy { Continuity(this, db) }

    override fun onCreate() {
        super.onCreate()
        // Keep the journal from growing without bound; the retention window is
        // deliberately long so Geyma still "remembers."
        appScope.launch(Dispatchers.IO) { repo.pruneJournal() }
    }
}
