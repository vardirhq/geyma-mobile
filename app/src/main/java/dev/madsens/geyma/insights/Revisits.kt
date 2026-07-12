package dev.madsens.geyma.insights

import java.util.concurrent.TimeUnit

/** How far out a resurfacing reminder can be scheduled, from the dossier. */
enum class RevisitWhen(val label: String, val deltaMs: Long) {
    TOMORROW("Tomorrow", TimeUnit.DAYS.toMillis(1)),
    IN_A_WEEK("In a week", TimeUnit.DAYS.toMillis(7)),
    IN_A_MONTH("In a month", TimeUnit.DAYS.toMillis(30)),
}

object Revisits {

    /**
     * Splits scheduled items into (due, upcoming) as of [nowMs]. Due items —
     * those whose moment has arrived — come back soonest-due first so the most
     * overdue nudge sits on top; upcoming items are ordered by how soon they land.
     */
    fun <T> partition(items: List<T>, nowMs: Long, dueMs: (T) -> Long): Pair<List<T>, List<T>> {
        val (due, upcoming) = items.partition { dueMs(it) <= nowMs }
        return due.sortedBy { dueMs(it) } to upcoming.sortedBy { dueMs(it) }
    }
}
