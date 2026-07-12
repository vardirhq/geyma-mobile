package dev.madsens.geyma.insights

import dev.madsens.geyma.data.EventActions
import dev.madsens.geyma.data.FileEvent
import dev.madsens.geyma.data.SeenFile
import dev.madsens.geyma.files.PathUtils

/**
 * One file's whole life as Geyma remembers it, distilled from its journal
 * entries plus the star, set and seen ledgers. Pure so the derivation is tested.
 */
data class DossierSummary(
    val path: String,
    val name: String,
    val exists: Boolean,
    val trashed: Boolean,
    /** "created" or "arrived" — how the file first entered Geyma's memory. */
    val originAction: String?,
    /** Free-text provenance from that first event ("in Downloads", "from foo"). */
    val originDetail: String?,
    val bornMs: Long?,
    val firstSeenMs: Long?,
    val lastOpenedMs: Long?,
    val timesOpened: Int,
    val timesMoved: Int,
    val timesCopied: Int,
    val starred: Boolean,
    val setNames: List<String>,
    val eventCount: Int,
) {
    val hasProvenance: Boolean get() = originAction != null || firstSeenMs != null
}

object Dossier {

    /**
     * Summarize the file at [path] from its [events] (as returned by the
     * per-path history query — newest first, matching path or prevPath) and the
     * side ledgers. [exists] is supplied by the caller since a pure function
     * can't touch the filesystem.
     */
    fun summarize(
        path: String,
        events: List<FileEvent>,
        starred: Boolean,
        setNames: List<String>,
        seen: SeenFile?,
        exists: Boolean,
    ): DossierSummary {
        val origin = events.lastOrNull {
            it.action == EventActions.CREATED || it.action == EventActions.ARRIVED
        }
        val latest = events.maxByOrNull { it.whenMs }
        val trashed = latest?.action == EventActions.TRASHED || latest?.action == EventActions.DELETED

        return DossierSummary(
            path = path,
            name = PathUtils.nameOf(path),
            exists = exists && !trashed,
            trashed = trashed,
            originAction = origin?.action,
            originDetail = origin?.detail,
            bornMs = events.minOfOrNull { it.whenMs },
            firstSeenMs = seen?.firstSeenMs,
            lastOpenedMs = seen?.lastOpenedMs,
            timesOpened = events.count { it.action == EventActions.OPENED },
            timesMoved = events.count { it.action == EventActions.MOVED || it.action == EventActions.RENAMED },
            timesCopied = events.count { it.action == EventActions.COPIED },
            starred = starred,
            setNames = setNames,
            eventCount = events.size,
        )
    }
}
