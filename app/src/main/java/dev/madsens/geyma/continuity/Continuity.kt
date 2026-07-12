package dev.madsens.geyma.continuity

import android.content.Context
import android.os.Build
import dev.madsens.geyma.data.FileEvent
import dev.madsens.geyma.data.GeymaDb
import dev.madsens.geyma.data.SetItem
import dev.madsens.geyma.data.Star
import dev.madsens.geyma.data.WorkingSet
import dev.madsens.geyma.files.StorageRoots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Portable handoff of Geyma's memory. The desktop app has a journal too, so the
 * one thing no other file manager can offer is continuity: carry your stars,
 * working sets and the timeline between devices as a plain-JSON `.geyma` bundle,
 * shared over whatever channel you like. No account, no server, no telemetry —
 * true to the on-device promise, but no longer trapped on one device.
 */
class Continuity(private val context: Context, private val db: GeymaDb) {

    data class Summary(val events: Int, val stars: Int, val sets: Int, val items: Int)

    /** Writes a bundle into `.geyma/exports` and returns the file. */
    suspend fun export(): File = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("format", "geyma-bundle")
        root.put("version", 1)
        root.put("exportedAtMs", System.currentTimeMillis())
        root.put("device", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
        root.put("storageRoot", StorageRoots.primaryPath())

        root.put(
            "events",
            JSONArray().apply {
                for (e in db.events().allChronological()) {
                    put(
                        JSONObject()
                            .put("path", e.path)
                            .put("action", e.action)
                            .putOpt("detail", e.detail)
                            .putOpt("prevPath", e.prevPath)
                            .put("isDir", e.isDir)
                            .put("whenMs", e.whenMs),
                    )
                }
            },
        )
        root.put(
            "stars",
            JSONArray().apply {
                for (s in db.stars().snapshot()) {
                    put(JSONObject().put("path", s.path).put("whenMs", s.whenMs))
                }
            },
        )
        root.put(
            "sets",
            JSONArray().apply {
                for (set in db.sets().snapshotSets()) {
                    put(
                        JSONObject()
                            .put("id", set.id)
                            .put("name", set.name)
                            .putOpt("note", set.note)
                            .put("createdMs", set.createdMs),
                    )
                }
            },
        )
        root.put(
            "setItems",
            JSONArray().apply {
                for (i in db.sets().snapshotItems()) {
                    put(
                        JSONObject()
                            .put("setId", i.setId)
                            .put("path", i.path)
                            .put("addedMs", i.addedMs),
                    )
                }
            },
        )

        val dir = File(StorageRoots.primaryPath(), ".geyma/exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val file = File(dir, "geyma-$stamp.geyma")
        file.writeText(root.toString())
        file
    }

    /**
     * Merge another device's bundle into this one. Additive: stars and set
     * membership union, sets match by id, and journal events are appended so the
     * combined timeline reflects both devices. Paths are rebased from the
     * bundle's storage root onto this device's so they resolve locally.
     */
    suspend fun import(input: InputStream): Summary = withContext(Dispatchers.IO) {
        // A bundle is plain JSON that we parse fully in memory, so cap how much a
        // (possibly malformed or hostile) file can pull in before we ever build
        // the tree. Real exports are a few MB even with long histories.
        val text = input.readBoundedText(MAX_BUNDLE_BYTES)
        val root = JSONObject(text)
        require(root.optString("format") == "geyma-bundle") { "Not a Geyma bundle" }

        val fromRoot = root.optString("storageRoot", StorageRoots.primaryPath())
        val here = StorageRoots.primaryPath()
        fun rebase(p: String?): String? {
            if (p == null) return null
            if (fromRoot == here || !p.startsWith(fromRoot)) return p
            return here + p.substring(fromRoot.length)
        }

        var events = 0
        var stars = 0
        var sets = 0
        var items = 0

        root.optJSONArray("stars")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                rebase(o.getString("path"))?.let {
                    db.stars().add(Star(path = it, whenMs = o.optLong("whenMs", System.currentTimeMillis())))
                    stars++
                }
            }
        }
        root.optJSONArray("sets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.sets().addSet(
                    WorkingSet(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        note = o.optString("note").ifBlank { null },
                        createdMs = o.optLong("createdMs", System.currentTimeMillis()),
                    ),
                )
                sets++
            }
        }
        root.optJSONArray("setItems")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                rebase(o.getString("path"))?.let {
                    db.sets().addItem(
                        SetItem(
                            setId = o.getString("setId"),
                            path = it,
                            addedMs = o.optLong("addedMs", System.currentTimeMillis()),
                        ),
                    )
                    items++
                }
            }
        }
        root.optJSONArray("events")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val path = rebase(o.getString("path")) ?: continue
                db.events().insert(
                    FileEvent(
                        path = path,
                        action = o.getString("action"),
                        detail = o.optString("detail").ifBlank { null },
                        prevPath = rebase(o.optString("prevPath").ifBlank { null }),
                        isDir = o.optBoolean("isDir", false),
                        whenMs = o.optLong("whenMs", System.currentTimeMillis()),
                    ),
                )
                events++
            }
        }

        Summary(events = events, stars = stars, sets = sets, items = items)
    }

    private companion object {
        /** Reject bundles larger than this before parsing — see [import]. */
        const val MAX_BUNDLE_BYTES = 64L * 1024 * 1024
    }
}

/**
 * Read a UTF-8 stream into a string, failing fast once more than [maxBytes]
 * have been consumed so a huge or malformed file can't exhaust memory.
 */
private fun InputStream.readBoundedText(maxBytes: Long): String = buffered().use { source ->
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(64 * 1024)
    var total = 0L
    while (true) {
        val read = source.read(buf)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "Bundle is too large to import" }
        out.write(buf, 0, read)
    }
    out.toString(Charsets.UTF_8.name())
}
