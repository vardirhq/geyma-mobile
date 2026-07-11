package dev.madsens.geyma.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

val LocalTheme = staticCompositionLocalOf { resolveTheme("parchment") }

val ResolvedTheme.bodyFont: FontFamily
    get() = when (fontKey) {
        FontKey.GROTESK -> FontFamily.SansSerif
        FontKey.SERIF -> FontFamily.Serif
        FontKey.MONO -> FontFamily.Monospace
        FontKey.SYSTEM -> FontFamily.Default
    }

/** Text color that reads on top of the accent (accent-filled buttons, badges). */
val ResolvedTheme.onAccent: Color
    get() = if (isLightAccent(accent)) Color(0xFF10130F) else Color(0xFFF7F7F2)

/** Card fill for tiles/cards, honoring the flat-vs-card tile token. */
val ResolvedTheme.tileFill: Color
    get() = if (tile == TileStyle.CARD) card else Color.Transparent

@Composable
fun GeymaTheme(theme: ResolvedTheme, content: @Composable () -> Unit) {
    val scheme = if (theme.isDark) {
        darkColorScheme(
            primary = theme.accent,
            onPrimary = theme.onAccent,
            background = theme.bg,
            onBackground = theme.ink,
            surface = theme.surface,
            onSurface = theme.ink,
            surfaceVariant = theme.card,
            onSurfaceVariant = theme.inkSoft,
            surfaceContainer = theme.card,
            surfaceContainerHigh = theme.card,
            surfaceContainerHighest = theme.card,
            surfaceContainerLow = theme.surface,
            outline = theme.inkFaint,
            outlineVariant = theme.border,
            error = DANGER,
        )
    } else {
        lightColorScheme(
            primary = theme.accent,
            onPrimary = theme.onAccent,
            background = theme.bg,
            onBackground = theme.ink,
            surface = theme.surface,
            onSurface = theme.ink,
            surfaceVariant = theme.card,
            onSurfaceVariant = theme.inkSoft,
            surfaceContainer = theme.card,
            surfaceContainerHigh = theme.card,
            surfaceContainerHighest = theme.card,
            surfaceContainerLow = theme.surface,
            outline = theme.inkFaint,
            outlineVariant = theme.border,
            error = DANGER,
        )
    }
    val font = theme.bodyFont
    val base = Typography()
    val typography = Typography(
        displayLarge = base.displayLarge.copy(fontFamily = font),
        displayMedium = base.displayMedium.copy(fontFamily = font),
        displaySmall = base.displaySmall.copy(fontFamily = font),
        headlineLarge = base.headlineLarge.copy(fontFamily = font),
        headlineMedium = base.headlineMedium.copy(fontFamily = font),
        headlineSmall = base.headlineSmall.copy(fontFamily = font),
        titleLarge = base.titleLarge.copy(fontFamily = font),
        titleMedium = base.titleMedium.copy(fontFamily = font),
        titleSmall = base.titleSmall.copy(fontFamily = font),
        bodyLarge = base.bodyLarge.copy(fontFamily = font),
        bodyMedium = base.bodyMedium.copy(fontFamily = font),
        bodySmall = base.bodySmall.copy(fontFamily = font),
        labelLarge = base.labelLarge.copy(fontFamily = font),
        labelMedium = base.labelMedium.copy(fontFamily = font),
        labelSmall = base.labelSmall.copy(fontFamily = font),
    )
    CompositionLocalProvider(LocalTheme provides theme) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

/** Background fill plus the skin's optional dot/grid backdrop pattern. */
fun Modifier.geymaBackdrop(theme: ResolvedTheme): Modifier {
    val patternColor = theme.ink.copy(alpha = if (theme.isDark) 0.05f else 0.06f)
    return this
        .fillMaxSize()
        .background(theme.bg)
        .drawBehind {
            when (theme.backdrop) {
                BackgroundPattern.NONE -> Unit
                BackgroundPattern.DOTS -> {
                    val step = 22.dp.toPx()
                    var y = step / 2
                    while (y < size.height) {
                        var x = step / 2
                        while (x < size.width) {
                            drawCircle(patternColor, radius = 1.4.dp.toPx(), center = Offset(x, y))
                            x += step
                        }
                        y += step
                    }
                }
                BackgroundPattern.GRID -> {
                    val step = 26.dp.toPx()
                    val stroke = 1.dp.toPx()
                    var x = step
                    while (x < size.width) {
                        drawLine(patternColor, Offset(x, 0f), Offset(x, size.height), stroke)
                        x += step
                    }
                    var y = step
                    while (y < size.height) {
                        drawLine(patternColor, Offset(0f, y), Offset(size.width, y), stroke)
                        y += step
                    }
                }
            }
        }
}
