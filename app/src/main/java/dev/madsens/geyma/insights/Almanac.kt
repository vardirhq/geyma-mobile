package dev.madsens.geyma.insights

import dev.madsens.geyma.data.EventActions
import dev.madsens.geyma.data.FileEvent
import dev.madsens.geyma.files.PathUtils
import java.util.concurrent.TimeUnit

/** One 24-hour bucket in the activity sparkline; [daysAgo] 0 is the last day. */
data class DayBucket(val daysAgo: Int, val count: Int)

/** A ranked count — a folder, a file, or an action and how often it shows up. */
data class Tally(val key: String, val label: String, val count: Int)

/**
 * A read-only portrait of the journal: what Geyma has remembered, when it was
 * busiest, which folders and files see the most life, and how much arrived and
 * was never touched. Pure so JVM tests cover the arithmetic.
 */
data class AlmanacStats(
    val totalEvents: Int,
    val firstEventMs: Long?,
    val arrivals: Int,
    val opens: Int,
    val moves: Int,
    val neglected: Int,
    val perDay: List<DayBucket>,
    val topFolders: List<Tally>,
    val busiestFiles: List<Tally>,
    val byAction: List<Tally>,
) {
    val hasHistory: Boolean get() = totalEvents > 0
    val busiestDay: DayBucket? get() = perDay.maxByOrNull { it.count }?.takeIf { it.count > 0 }
    val peakDayCount: Int get() = perDay.maxOfOrNull { it.count } ?: 0
}

object Almanac {

    /** The path a file lived at when the event happened (gone events name the origin). */
    private fun homePath(e: FileEvent): String = when (e.action) {
        EventActions.TRASHED, EventActions.DELETED, EventActions.MOVED, EventActions.RENAMED ->
            e.prevPath ?: e.path
        else -> e.path
    }

    /**
     * Aggregate [events] (any order) into a portrait as of [nowMs], with a
     * [days]-wide activity sparkline. [neglected] is the count of arrived-but-
     * never-opened files, which lives in the seen-ledger rather than the journal.
     */
    fun compute(
        events: List<FileEvent>,
        neglected: Int,
        nowMs: Long,
        days: Int = 14,
    ): AlmanacStats {
        val dayMs = TimeUnit.DAYS.toMillis(1)
        val perDayCounts = IntArray(days)
        var arrivals = 0
        var opens = 0
        var moves = 0
        val folderCounts = LinkedHashMap<String, Int>()
        val fileCounts = LinkedHashMap<String, Int>()
        val actionCounts = LinkedHashMap<String, Int>()
        var firstEventMs: Long? = null

        for (e in events) {
            if (firstEventMs == null || e.whenMs < firstEventMs!!) firstEventMs = e.whenMs
            when (e.action) {
                EventActions.ARRIVED -> arrivals++
                EventActions.OPENED -> opens++
                EventActions.MOVED, EventActions.RENAMED -> moves++
            }
            actionCounts.merge(e.action, 1, Int::plus)

            val home = homePath(e)
            fileCounts.merge(home, 1, Int::plus)
            PathUtils.parentOf(home)?.let { folderCounts.merge(it, 1, Int::plus) }

            val daysAgo = ((nowMs - e.whenMs) / dayMs).toInt()
            if (daysAgo in 0 until days) perDayCounts[daysAgo] += 1
        }

        // Oldest bucket first so a UI can read left-to-right up to "today".
        val perDay = (days - 1 downTo 0).map { DayBucket(it, perDayCounts[it]) }

        val topFolders = folderCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { Tally(it.key, PathUtils.nameOf(it.key), it.value) }

        val busiestFiles = fileCounts.entries
            .filter { it.value > 1 }
            .sortedByDescending { it.value }
            .take(5)
            .map { Tally(it.key, PathUtils.nameOf(it.key), it.value) }

        val byAction = actionCounts.entries
            .sortedByDescending { it.value }
            .map { Tally(it.key, it.key, it.value) }

        return AlmanacStats(
            totalEvents = events.size,
            firstEventMs = firstEventMs,
            arrivals = arrivals,
            opens = opens,
            moves = moves,
            neglected = neglected,
            perDay = perDay,
            topFolders = topFolders,
            busiestFiles = busiestFiles,
            byAction = byAction,
        )
    }
}
