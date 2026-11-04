package com.list_it.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.list_it.app.model.SurfaceChoice

/** iOS system colours used by the original app. */
object IosColor {
    val orange = Color(0xFFFF9500)
    val blue = Color(0xFF007AFF)
    val red = Color(0xFFFF3B30)
    val green = Color(0xFF34C759)
    val yellow = Color(0xFFFFCC00)
    val purple = Color(0xFFAF52DE)
    val indigo = Color(0xFF5856D6)
    val cyan = Color(0xFF32ADE6)
    val pink = Color(0xFFFF2D55)
    val brown = Color(0xFFA2845E)
    val gray = Color(0xFF8E8E93)
}

/** Colours from the web app's Capacitor (Android) build. */
object WebColor {
    val indigo = Color(0xFF6366F1)
    val orange400 = Color(0xFFFB923C)
    val orange500 = Color(0xFFF97316)
    val sky500 = Color(0xFF0EA5E9)
    val blue500 = Color(0xFF3B82F6)
    val red600 = Color(0xFFDC2626)
    val gray300 = Color(0xFFD1D5DB)
    val gray400 = Color(0xFF9CA3AF)
    val gray500 = Color(0xFF6B7280)
    val gray900 = Color(0xFF111827)
}

/** One background option: the page and the surfaces that sit on it (web `surfaceTheme.ts`). */
@Immutable
data class SurfaceRamp(val field: Color, val deep: Color, val raised: Color, val card: Color, val selected: Color, val border: Color)

private fun c(hex: Long) = Color(hex)

val SURFACE_RAMPS: Map<Boolean, Map<SurfaceChoice, SurfaceRamp>> = mapOf(
    true to mapOf(
        SurfaceChoice.MONO to SurfaceRamp(c(0xFF000000), c(0xFF000000), c(0xFF1C1C1E), c(0xFF121212), c(0xFF26262A), Color.White.copy(alpha = 0.10f)),
        SurfaceChoice.SOFT to SurfaceRamp(c(0xFF202024), c(0xFF16161A), c(0xFF35353B), c(0xFF2A2A2F), c(0xFF3C3C43), Color.White.copy(alpha = 0.09f)),
        SurfaceChoice.COOL to SurfaceRamp(c(0xFF0B1222), c(0xFF05080F), c(0xFF121829), c(0xFF131A2B), c(0xFF1B2440), Color.White.copy(alpha = 0.08f)),
        SurfaceChoice.WARM to SurfaceRamp(c(0xFF240B36), c(0xFF190726), c(0xFF381852), c(0xFF2E1244), c(0xFF421E60), Color.White.copy(alpha = 0.08f)),
    ),
    false to mapOf(
        SurfaceChoice.MONO to SurfaceRamp(c(0xFFFFFFFF), c(0xFFEBEBED), c(0xFFEBEBED), c(0xFFF4F4F5), c(0xFFEEEEF0), Color(15, 23, 42).copy(alpha = 0.12f)),
        // Light partner of Midnight: a soft grey page with pure white cards, so boxes stand out.
        SurfaceChoice.SOFT to SurfaceRamp(c(0xFFE8E8E8), c(0xFFDCDCDC), c(0xFFF4F4F5), c(0xFFFFFFFF), c(0xFFF0F0F2), Color(15, 23, 42).copy(alpha = 0.10f)),
        SurfaceChoice.COOL to SurfaceRamp(c(0xFFEDF4FF), c(0xFFDAE6FA), c(0xFFE2ECFD), c(0xFFFFFFFF), c(0xFFD7E5FC), Color(15, 23, 42).copy(alpha = 0.10f)),
        SurfaceChoice.WARM to SurfaceRamp(c(0xFFE9E7E1), c(0xFFEDEBE6), c(0xFFEDEBE6), c(0xFFFFFFFF), c(0xFFE8E5DE), Color(28, 25, 23).copy(alpha = 0.10f)),
    ),
)

/** Theme colours: iOS text colours plus the surfaces of the chosen background. */
@Immutable
data class Palette(
    val isDark: Boolean,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val separator: Color,
    val fill: Color,
    val material: Color,
    val ramp: SurfaceRamp,
) {
    /** The page. */
    val groupedBackground get() = ramp.field
    val systemBackground get() = ramp.field
    /** Cards on the page. */
    val secondaryGroupedBackground get() = ramp.card
    /** Raised surfaces that are not cards (pills, grouped rows). */
    val secondaryBackground get() = ramp.raised
    val gray6 get() = ramp.raised
    /** Settings/Progress accent: orange in dark, sky in light, as in the web app. */
    val accent get() = if (isDark) WebColor.orange400 else WebColor.sky500
}

private fun palette(dark: Boolean, ramp: SurfaceRamp) = if (dark) Palette(
    isDark = true, primary = Color.White, secondary = Color(0x99EBEBF5), tertiary = Color(0x4DEBEBF5),
    separator = Color(0x99545458), fill = Color(0x5C787880), material = ramp.card.copy(alpha = 0.85f), ramp = ramp,
) else Palette(
    isDark = false, primary = Color.Black, secondary = Color(0x993C3C43), tertiary = Color(0x4D3C3C43),
    separator = Color(0x4A3C3C43), fill = Color(0x33787880), material = ramp.card.copy(alpha = 0.85f), ramp = ramp,
)

val LocalPalette = staticCompositionLocalOf { palette(false, SURFACE_RAMPS.getValue(false).getValue(SurfaceChoice.SOFT)) }

/** Background used by every screen: the chosen background's page colour. */
val appBackground: Color
    @Composable get() = LocalPalette.current.ramp.field

/** Same parsing rules as the iOS `Color(hex:)` extension. */
fun colorFromHex(hex: String): Color {
    val clean = hex.filter { it.isLetterOrDigit() }
    val v = clean.toULongOrNull(16)?.toLong() ?: 0L
    return when (clean.length) {
        3 -> Color(((v shr 8) * 17).toInt(), ((v shr 4 and 0xF) * 17).toInt(), ((v and 0xF) * 17).toInt())
        6 -> Color((v shr 16 and 0xFF).toInt(), (v shr 8 and 0xFF).toInt(), (v and 0xFF).toInt())
        8 -> Color((v shr 16 and 0xFF).toInt(), (v shr 8 and 0xFF).toInt(), (v and 0xFF).toInt(), (v shr 24 and 0xFF).toInt())
        else -> Color(1, 1, 0, 1)
    }
}

@Composable
fun ListItTheme(darkMode: Boolean?, surface: (Boolean) -> SurfaceChoice, content: @Composable () -> Unit) {
    val dark = darkMode ?: isSystemInDarkTheme()
    val palette = palette(dark, SURFACE_RAMPS.getValue(dark).getValue(surface(dark)))
    val r = palette.ramp
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF0A84FF), onPrimary = Color.White, background = r.field,
            surface = r.card, onSurface = palette.primary, surfaceContainer = r.raised,
            surfaceContainerLow = r.card, surfaceContainerHigh = r.raised, surfaceContainerHighest = r.selected,
            onSurfaceVariant = palette.secondary, outlineVariant = palette.separator,
        )
    } else {
        lightColorScheme(
            primary = IosColor.blue, onPrimary = Color.White, background = r.field,
            surface = r.card, onSurface = palette.primary, surfaceContainer = r.raised,
            surfaceContainerLow = r.card, surfaceContainerHigh = r.card, surfaceContainerHighest = r.selected,
            onSurfaceVariant = palette.secondary, outlineVariant = palette.separator,
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = IosTypography, content = content)
    }
}
