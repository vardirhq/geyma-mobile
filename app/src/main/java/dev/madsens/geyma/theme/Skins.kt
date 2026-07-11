package dev.madsens.geyma.theme

import androidx.compose.ui.graphics.Color

/**
 * Straight port of the desktop app's `src/theme/skins.ts` token system.
 * Every skin resolves to the same named tokens the desktop uses, so the two
 * apps stay visually interchangeable.
 */

enum class SkinMode { LIGHT, DARK }
enum class TileStyle { FLAT, CARD }
enum class BackgroundPattern { NONE, DOTS, GRID }
enum class FontKey { GROTESK, SERIF, MONO, SYSTEM }

data class Skin(
    val id: String,
    val name: String,
    val mode: SkinMode,
    val tag: String,
    val bg: Color,
    val surface: Color,
    val main: Color,
    val card: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val border: Color,
    val accent: Color,
    val font: FontKey,
    val radius: Int,
    val tile: TileStyle,
    val iconMono: Boolean,
    val pattern: BackgroundPattern,
)

data class SkinOverrides(
    val accent: Color? = null,
    val fontKey: FontKey? = null,
    val radius: Int? = null,
    val tile: TileStyle? = null,
    val iconMono: Boolean? = null,
    val pattern: BackgroundPattern? = null,
)

data class ResolvedTheme(
    val id: String,
    val name: String,
    val tag: String,
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val main: Color,
    val card: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val border: Color,
    val accent: Color,
    val radius: Int,
    val tile: TileStyle,
    val iconMono: Boolean,
    val backdrop: BackgroundPattern,
    val fontKey: FontKey,
)

private fun hex(v: Long) = Color(0xFF000000 or v)
private fun rgba(r: Int, g: Int, b: Int, a: Float) = Color(r / 255f, g / 255f, b / 255f, a)

val SKINS: Map<String, Skin> = listOf(
    Skin(
        id = "parchment", name = "Parchment", mode = SkinMode.LIGHT, tag = "Warm reading light",
        bg = hex(0xEFE9DE), surface = hex(0xE7E0D2), main = hex(0xF3EEE4), card = hex(0xFBF8F2),
        ink = hex(0x211D17), inkSoft = hex(0x5F5849), inkFaint = hex(0x7E7359),
        border = rgba(0x21, 0x1D, 0x17, 0.10f), accent = hex(0x2C6E49),
        font = FontKey.GROTESK, radius = 14, tile = TileStyle.FLAT, iconMono = false, pattern = BackgroundPattern.NONE,
    ),
    Skin(
        id = "obsidian", name = "Obsidian", mode = SkinMode.DARK, tag = "Cold and precise",
        bg = hex(0x0B0D10), surface = hex(0x101318), main = hex(0x0E1114), card = hex(0x171B21),
        ink = hex(0xE8ECF1), inkSoft = hex(0x9AA4B0), inkFaint = hex(0x5C6672),
        border = rgba(0xFF, 0xFF, 0xFF, 0.09f), accent = hex(0x35D0C0),
        font = FontKey.GROTESK, radius = 14, tile = TileStyle.CARD, iconMono = false, pattern = BackgroundPattern.NONE,
    ),
    Skin(
        id = "phosphor", name = "Phosphor", mode = SkinMode.DARK, tag = "Terminal green",
        bg = hex(0x050A06), surface = hex(0x08110A), main = hex(0x060D08), card = hex(0x0C160E),
        ink = hex(0x8CF5A6), inkSoft = hex(0x4A9E63), inkFaint = hex(0x2E6440),
        border = rgba(0x38, 0xE5, 0x6A, 0.16f), accent = hex(0x38E56A),
        font = FontKey.MONO, radius = 2, tile = TileStyle.FLAT, iconMono = true, pattern = BackgroundPattern.GRID,
    ),
    Skin(
        id = "nord", name = "Nord", mode = SkinMode.DARK, tag = "Arctic calm",
        bg = hex(0x2E3440), surface = hex(0x2B303B), main = hex(0x333A47), card = hex(0x3B4252),
        ink = hex(0xECEFF4), inkSoft = hex(0xAEB6C6), inkFaint = hex(0x6D7488),
        border = rgba(0xEC, 0xEF, 0xF4, 0.10f), accent = hex(0x88C0D0),
        font = FontKey.GROTESK, radius = 10, tile = TileStyle.CARD, iconMono = false, pattern = BackgroundPattern.NONE,
    ),
    Skin(
        id = "amber", name = "Amber", mode = SkinMode.DARK, tag = "Retro phosphor",
        bg = hex(0x140F08), surface = hex(0x1A130A), main = hex(0x120D06), card = hex(0x20180F),
        ink = hex(0xF5C877), inkSoft = hex(0xB0812F), inkFaint = hex(0x9A7B36),
        border = rgba(0xFF, 0xB2, 0x3E, 0.16f), accent = hex(0xFFB23E),
        font = FontKey.MONO, radius = 4, tile = TileStyle.FLAT, iconMono = true, pattern = BackgroundPattern.GRID,
    ),
    Skin(
        id = "plasma", name = "Plasma", mode = SkinMode.LIGHT, tag = "Clean workspace",
        bg = hex(0xEEF1F6), surface = hex(0xE5EAF3), main = hex(0xF5F7FB), card = hex(0xFFFFFF),
        ink = hex(0x1D2733), inkSoft = hex(0x566072), inkFaint = hex(0x98A2B3),
        border = rgba(0x1D, 0x27, 0x33, 0.11f), accent = hex(0x2C7DD6),
        font = FontKey.SYSTEM, radius = 8, tile = TileStyle.CARD, iconMono = false, pattern = BackgroundPattern.NONE,
    ),
    Skin(
        id = "synthwave", name = "Synthwave", mode = SkinMode.DARK, tag = "Neon nights",
        bg = hex(0x17091F), surface = hex(0x1F0C2B), main = hex(0x1A0A23), card = hex(0x2A1139),
        ink = hex(0xF6E7FF), inkSoft = hex(0xBC93D8), inkFaint = hex(0x7C5C97),
        border = rgba(0xFF, 0x4F, 0xA3, 0.18f), accent = hex(0xFF4FA3),
        font = FontKey.GROTESK, radius = 12, tile = TileStyle.CARD, iconMono = false, pattern = BackgroundPattern.DOTS,
    ),
    Skin(
        id = "paper", name = "Paper", mode = SkinMode.LIGHT, tag = "Nothing but ink",
        bg = hex(0xFFFFFF), surface = hex(0xFAFAF9), main = hex(0xFFFFFF), card = hex(0xFFFFFF),
        ink = hex(0x141414), inkSoft = hex(0x5F5F5F), inkFaint = hex(0xADADAD),
        border = rgba(0, 0, 0, 0.11f), accent = hex(0x141414),
        font = FontKey.SERIF, radius = 5, tile = TileStyle.FLAT, iconMono = true, pattern = BackgroundPattern.NONE,
    ),
).associateBy { it.id }

val SKIN_ORDER = listOf("parchment", "obsidian", "phosphor", "nord", "amber", "plasma", "synthwave", "paper")

val ACCENTS = listOf(
    hex(0x2C6E49), hex(0x2C7DD6), hex(0xB4562E), hex(0x7A4B8C),
    hex(0xC6427A), hex(0xD89B2B), hex(0x35D0C0), hex(0xE4572E),
)

/** The one red-ish tone for destructive/error affordances — fits every skin. */
val DANGER = hex(0xC6427A)

val KIND_COLORS: Map<String, Color> = mapOf(
    "folder" to hex(0x7A7264),
    "document" to hex(0xE4572E),
    "text" to hex(0x8A8172),
    "code" to hex(0x17A398),
    "image" to hex(0x9B6DE0),
    "video" to hex(0x3B82C4),
    "audio" to hex(0xD6559A),
    "archive" to hex(0xD19A3A),
    "app" to hex(0x5CA95A),
)

fun mix(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f,
)

fun lighten(c: Color, t: Float) = mix(c, Color.White, t)
fun shade(c: Color, t: Float) = mix(c, Color.Black, t)

fun isLightAccent(c: Color): Boolean =
    (0.299f * c.red + 0.587f * c.green + 0.114f * c.blue) * 255f > 150f

fun resolveTheme(skinId: String, ov: SkinOverrides = SkinOverrides()): ResolvedTheme {
    val base = SKINS[skinId] ?: SKINS.getValue("parchment")
    return ResolvedTheme(
        id = base.id,
        name = base.name,
        tag = base.tag,
        isDark = base.mode == SkinMode.DARK,
        bg = base.bg,
        surface = base.surface,
        main = base.main,
        card = base.card,
        ink = base.ink,
        inkSoft = base.inkSoft,
        inkFaint = base.inkFaint,
        border = base.border,
        accent = ov.accent ?: base.accent,
        radius = ov.radius ?: base.radius,
        tile = ov.tile ?: base.tile,
        iconMono = ov.iconMono ?: base.iconMono,
        backdrop = ov.pattern ?: base.pattern,
        fontKey = ov.fontKey ?: base.font,
    )
}

data class ItemColors(val tint: Color, val bg: Color)

/** Icon tint + chip background for an entry kind, honoring the mono-icon toggle. */
fun itemColors(kind: String, t: ResolvedTheme): ItemColors {
    val isFolder = kind == "folder"
    if (t.iconMono) {
        return ItemColors(
            tint = if (isFolder) t.ink else t.inkSoft,
            bg = t.ink.copy(alpha = if (t.isDark) 0.12f else 0.07f),
        )
    }
    if (isFolder) {
        return ItemColors(
            tint = t.accent,
            bg = t.accent.copy(alpha = if (t.isDark) 0.2f else 0.14f),
        )
    }
    val c = KIND_COLORS[kind] ?: Color(0xFF8A8172)
    return ItemColors(
        tint = if (t.isDark) lighten(c, 0.28f) else c,
        bg = c.copy(alpha = if (t.isDark) 0.2f else 0.13f),
    )
}
