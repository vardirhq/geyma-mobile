package dev.madsens.geyma

import androidx.compose.ui.graphics.Color
import dev.madsens.geyma.theme.BackgroundPattern
import dev.madsens.geyma.theme.FontKey
import dev.madsens.geyma.theme.SKINS
import dev.madsens.geyma.theme.SKIN_ORDER
import dev.madsens.geyma.theme.SkinOverrides
import dev.madsens.geyma.theme.TileStyle
import dev.madsens.geyma.theme.isLightAccent
import dev.madsens.geyma.theme.itemColors
import dev.madsens.geyma.theme.resolveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinsTest {

    @Test
    fun allEightDesktopSkinsArePresent() {
        assertEquals(8, SKIN_ORDER.size)
        assertTrue(SKIN_ORDER.all { it in SKINS })
    }

    @Test
    fun resolveUsesBaseTokensWithoutOverrides() {
        val t = resolveTheme("obsidian")
        assertEquals("obsidian", t.id)
        assertTrue(t.isDark)
        assertEquals(14, t.radius)
        assertEquals(TileStyle.CARD, t.tile)
        assertEquals(FontKey.GROTESK, t.fontKey)
        assertEquals(SKINS.getValue("obsidian").accent, t.accent)
    }

    @Test
    fun overridesWinOverBaseTokens() {
        val accent = Color(0xFFE4572E)
        val t = resolveTheme(
            "paper",
            SkinOverrides(
                accent = accent,
                fontKey = FontKey.MONO,
                radius = 0,
                tile = TileStyle.CARD,
                iconMono = false,
                pattern = BackgroundPattern.GRID,
            ),
        )
        assertEquals(accent, t.accent)
        assertEquals(FontKey.MONO, t.fontKey)
        assertEquals(0, t.radius)
        assertEquals(TileStyle.CARD, t.tile)
        assertFalse(t.iconMono)
        assertEquals(BackgroundPattern.GRID, t.backdrop)
    }

    @Test
    fun unknownSkinFallsBackToParchment() {
        assertEquals("parchment", resolveTheme("does-not-exist").id)
    }

    @Test
    fun monoSkinsTintIconsWithInk() {
        val phosphor = resolveTheme("phosphor")
        val colors = itemColors("image", phosphor)
        assertEquals(phosphor.inkSoft, colors.tint)
        val folder = itemColors("folder", phosphor)
        assertEquals(phosphor.ink, folder.tint)
    }

    @Test
    fun colorfulSkinsTintFoldersWithAccent() {
        val obsidian = resolveTheme("obsidian")
        assertEquals(obsidian.accent, itemColors("folder", obsidian).tint)
    }

    @Test
    fun accentLuminanceSplitsLightFromDark() {
        assertTrue(isLightAccent(Color(0xFFFFB23E)))
        assertFalse(isLightAccent(Color(0xFF2C6E49)))
    }
}
