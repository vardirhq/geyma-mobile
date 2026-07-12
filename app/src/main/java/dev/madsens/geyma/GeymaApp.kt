package dev.madsens.geyma

import android.app.Application
import dev.madsens.geyma.continuity.Continuity
import dev.madsens.geyma.data.GeymaDb
import dev.madsens.geyma.data.Prefs
import dev.madsens.geyma.files.FsRepository
import dev.madsens.geyma.files.StorageWatcher
import dev.madsens.geyma.insights.DuplicateGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class GeymaApp : Application() {
    val db: GeymaDb by lazy { GeymaDb.get(this) }
    val repo: FsRepository by lazy { FsRepository(db) }
    val prefs: Prefs by lazy { Prefs(this) }

    /** Lives as long as the process — for storage watching that outlasts screens. */
    val appScope: CoroutineScope by lazy { CoroutineScope(SupervisorJob()) }

    val watcher: StorageWatcher by lazy { StorageWatcher(repo, prefs, appScope) }
    val continuity: Continuity by lazy { Continuity(this, db) }

    /**
     * The last Echoes scan, kept for the life of the process so stepping into a
     * file's dossier and back doesn't trigger a full rescan. Invalidated when
     * echoes are cleared or the user asks to rescan.
     */
    @Volatile
    var echoesCache: List<DuplicateGroup>? = null
}
