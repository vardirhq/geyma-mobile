package dev.madsens.geyma

import dev.madsens.geyma.files.ArchiveRecord
import dev.madsens.geyma.files.ArchiveSupport
import dev.madsens.geyma.files.ArchiveTree
import dev.madsens.geyma.files.FileKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveTreeTest {

    private val records = listOf(
        ArchiveRecord("readme.txt", 10),
        ArchiveRecord("docs/", 0),          // explicit dir
        ArchiveRecord("docs/guide.md", 20),
        ArchiveRecord("docs/img/logo.png", 30), // "img" is implicit — no dir entry
        ArchiveRecord("src/Main.kt", 40),
    )

    @Test
    fun rootListsTopLevelFoldersBeforeFiles() {
        val root = ArchiveTree.childrenOf(records, "")
        assertEquals(listOf("docs", "src", "readme.txt"), root.map { it.name })
        assertTrue(root[0].isDir)
        assertFalse(root.last().isDir)
        assertEquals(FileKind.FOLDER, root[0].kind)
    }

    @Test
    fun synthesizesImplicitDirectoriesFromFilePaths() {
        val docs = ArchiveTree.childrenOf(records, "docs/")
        assertEquals(listOf("img", "guide.md"), docs.map { it.name })
        val img = docs.first { it.name == "img" }
        assertTrue(img.isDir)
        assertEquals("docs/img/", img.path)

        val inImg = ArchiveTree.childrenOf(records, "docs/img/")
        assertEquals(listOf("logo.png"), inImg.map { it.name })
        assertEquals(FileKind.IMAGE, inImg[0].kind)
        assertEquals(30L, inImg[0].size)
    }

    @Test
    fun onlyZipLikeContainersAreBrowsable() {
        assertTrue(ArchiveSupport.canBrowse("photos.zip"))
        assertTrue(ArchiveSupport.canBrowse("lib.jar"))
        assertFalse(ArchiveSupport.canBrowse("bundle.tar.gz"))
        assertFalse(ArchiveSupport.canBrowse("data.7z"))
        assertFalse(ArchiveSupport.canBrowse("app.apk"))
    }
}
