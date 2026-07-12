package dev.madsens.geyma

import dev.madsens.geyma.insights.Revisits
import org.junit.Assert.assertEquals
import org.junit.Test

class RevisitsTest {

    private val now = 1_000L

    @Test
    fun partitionSplitsDueFromUpcoming() {
        val items = listOf(500L, 900L, 1_000L, 1_500L, 2_000L)
        val (due, upcoming) = Revisits.partition(items, now) { it }
        // dueMs <= now counts as due, including exactly-now.
        assertEquals(listOf(500L, 900L, 1_000L), due)
        assertEquals(listOf(1_500L, 2_000L), upcoming)
    }

    @Test
    fun dueItemsComeBackSoonestDueFirst() {
        val items = listOf(900L, 100L, 500L)
        val (due, _) = Revisits.partition(items, now) { it }
        assertEquals(listOf(100L, 500L, 900L), due)
    }

    @Test
    fun upcomingItemsAreOrderedBySoonest() {
        val items = listOf(3_000L, 1_100L, 2_000L)
        val (_, upcoming) = Revisits.partition(items, now) { it }
        assertEquals(listOf(1_100L, 2_000L, 3_000L), upcoming)
    }

    @Test
    fun everythingUpcomingWhenNothingIsDue() {
        val items = listOf(2_000L, 3_000L)
        val (due, upcoming) = Revisits.partition(items, now) { it }
        assertEquals(emptyList<Long>(), due)
        assertEquals(items, upcoming)
    }
}
