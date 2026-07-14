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

/**
 * A one-line answer to "why is this here?" — the origin story a plain file
 * manager can never tell. [headline] is the plain-language clause; [whenMs] is
 * the moment it happened (for a relative-time suffix), null when unknown.
 */
data class Provenance(val headline: String, val whenMs: Long?)

object Dossier {

    /**
     * Distil a file's origin into a single "why is this here?" line, purely from
     * what the journal remembers — a download that landed in a watched folder, a
     * share from another app, an extraction, an in-app creation, or (failing all
     * that) that it simply predates Geyma's watch. Pure, so it is unit-tested.
     */
    fun whyHere(s: DossierSummary): Provenance = when {
        s.originAction == EventActions.ARRIVED -> {
            val where = s.originDetail?.takeIf { it.isNotBlank() }?.let { " $it" } ?: " from outside Geyma"
            Provenance("Arrived$where", s.bornMs ?: s.firstSeenMs)
        }
        s.originAction == EventActions.CREATED && s.originDetail?.startsWith("extracted") == true ->
            Provenance("Created here · ${s.originDetail}", s.bornMs)
        s.originAction == EventActions.CREATED ->
            Provenance("Created inside Geyma", s.bornMs)
        s.firstSeenMs != null ->
            Provenance("Geyma first noticed it here", s.firstSeenMs)
        else ->
            Provenance("Here before Geyma started remembering", null)
    }

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
