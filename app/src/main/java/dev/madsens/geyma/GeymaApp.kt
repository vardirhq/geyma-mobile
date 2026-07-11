package dev.madsens.geyma

import android.app.Application
import dev.madsens.geyma.data.GeymaDb
import dev.madsens.geyma.data.Prefs
import dev.madsens.geyma.files.FsRepository

class GeymaApp : Application() {
    val db: GeymaDb by lazy { GeymaDb.get(this) }
    val repo: FsRepository by lazy { FsRepository(db) }
    val prefs: Prefs by lazy { Prefs(this) }
}
