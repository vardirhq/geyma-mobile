package dev.madsens.geyma

import dev.madsens.geyma.files.FileKind
import dev.madsens.geyma.insights.Echoes
import dev.madsens.geyma.insights.FileFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EchoesTest {

    private fun fp(path: String, size: Long, hash: String, modifiedMs: Long) =
        FileFingerprint(path, path.substringAfterLast('/'), size, hash, FileKind.ofName(path, false), modifiedMs)

    @Test
    fun identicalSizeAndHashGroupTogether() {
        val files = listOf(
            fp("/a/one.jpg", 100, "aaa", modifiedMs = 30),
            fp("/b/copy.jpg", 100, "aaa", modifiedMs = 10),
            fp("/c/other.jpg", 100, "bbb", modifiedMs = 5), // same size, different bytes
        )
        val groups = Echoes.group(files)
        assertEquals(1, groups.size)
        assertEquals(2, groups.first().files.size)
    }

    @Test
    fun oldestCopyIsTheKeptOriginal() {
        val files = listOf(
            fp("/a/one.jpg", 100, "aaa", modifiedMs = 30),
            fp("/b/copy.jpg", 100, "aaa", modifiedMs = 10),
        )
        val group = Echoes.group(files).first()
        assertEquals("/b/copy.jpg", group.original.path) // modifiedMs 10 is older
        assertEquals(1, group.echoes.size)
        assertEquals("/a/one.jpg", group.echoes.first().path)
    }

    @Test
    fun reclaimableCountsEveryEchoButTheOriginal() {
        val files = listOf(
            fp("/a/x", 200, "h", modifiedMs = 1),
            fp("/b/x", 200, "h", modifiedMs = 2),
            fp("/c/x", 200, "h", modifiedMs = 3),
        )
        val group = Echoes.group(files).first()
        assertEquals(400, group.reclaimable) // 2 echoes * 200 bytes
        assertEquals(400, Echoes.totalReclaimable(listOf(group)))
    }

    @Test
    fun groupsRankByReclaimableSpace() {
        val files = listOf(
            fp("/a/small", 10, "s", modifiedMs = 1),
            fp("/b/small", 10, "s", modifiedMs = 2),
            fp("/a/big", 1000, "g", modifiedMs = 1),
            fp("/b/big", 1000, "g", modifiedMs = 2),
        )
        val groups = Echoes.group(files)
        assertEquals(2, groups.size)
        assertTrue(groups[0].reclaimable > groups[1].reclaimable)
        assertEquals(1000, groups[0].size)
    }

    @Test
    fun zeroByteFilesAreNeverDuplicates() {
        val files = listOf(
            fp("/a/empty", 0, "e", modifiedMs = 1),
            fp("/b/empty", 0, "e", modifiedMs = 2),
        )
        assertTrue(Echoes.group(files).isEmpty())
    }

    @Test
    fun uniqueFilesProduceNoGroups() {
        val files = listOf(
            fp("/a/one", 100, "a", modifiedMs = 1),
            fp("/b/two", 200, "b", modifiedMs = 2),
        )
        assertTrue(Echoes.group(files).isEmpty())
    }
}
