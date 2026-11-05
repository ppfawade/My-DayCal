package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Obsidian Kinetic Dark ColorScheme
private val DarkColorScheme = darkColorScheme(
    primary = SolarAmberPrimary,                        // #FFC174
    onPrimary = Color(0xFF472A00),                      // #472A00
    primaryContainer = SolarAmber,                      // #F59E0B
    onPrimaryContainer = Color(0xFF2A1700),             // #2A1700
    inversePrimary = Color(0xFF855300),
    secondary = HyperSkyLight,                          // #7BD0FF
    onSecondary = Color(0xFF00354A),
    secondaryContainer = Color(0xFF00A6E0),
    onSecondaryContainer = Color(0xFF00374D),
    tertiary = MintSignalLight,                         // #56E5A9
    onTertiary = Color(0xFF003824),
    tertiaryContainer = CyberTeal,                      // #10B981
    onTertiaryContainer = Color(0xFF003824),
    background = ObsidianM3Surface,                     // #111319
    onBackground = ObsidianOnSurface,                   // #E2E2EB
    surface = ObsidianM3Surface,                        // #111319
    onSurface = ObsidianOnSurface,                      // #E2E2EB
    surfaceVariant = ObsidianSurfaceVariant,            // #33343B
    onSurfaceVariant = ObsidianOnSurfaceVariant,        // #D8C3AD
    surfaceTint = Color(0xFFFFB95F),
    inverseSurface = ObsidianOnSurface,
    inverseOnSurface = Color(0xFF2E3037),
    error = KineticError,                               // #FFB4AB
    onError = KineticOnError,
    errorContainer = KineticErrorContainer,             // #93000A
    onErrorContainer = KineticOnErrorContainer,         // #FFDAD6
    outline = ObsidianOutline,                          // #A08E7A
    outlineVariant = ObsidianOutlineVariant,            // #534434
    surfaceBright = ObsidianSurfaceBright,              // #373940
    surfaceDim = ObsidianM3Surface,                     // #111319
    surfaceContainerLowest = ObsidianCanvasLowest,      // #0C0E14
    surfaceContainerLow = ObsidianSurfaceLow,           // #191B22
    surfaceContainer = ObsidianSurfaceContainer,        // #1E1F26
    surfaceContainerHigh = ObsidianSurfaceHigh,         // #282A30
    surfaceContainerHighest = ObsidianSurfaceVariant    // #33343B
)

// Warm Ivory Daylight ColorScheme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB45309),                        // Deep Warm Amber for crisp light-mode contrast
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF59E0B),               // Solar Amber active pill/chip fill
    onPrimaryContainer = Color(0xFF2A1700),
    inversePrimary = Color(0xFFFFC174),
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF059669),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF10B981),
    onTertiaryContainer = Color(0xFF002114),
    background = Color(0xFFFAF8F5),                     // Warm Ivory paper canvas
    onBackground = Color(0xFF1C1917),                   // Deep Warm Ink
    surface = Color(0xFFFAF8F5),
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFE6E0D6),
    onSurfaceVariant = Color(0xFF57534E),               // Warm Stone secondary text
    surfaceTint = Color(0xFFD97706),
    inverseSurface = Color(0xFF292524),
    inverseOnSurface = Color(0xFFFAF8F5),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFF8C8275),                        // Warm muted label
    outlineVariant = Color(0xFFD6CFC2),                 // Hairline border
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFF0ECE4),
    surfaceContainerLowest = Color(0xFFF3EFEA),         // Recessed wells (CW box, gap bar)
    surfaceContainerLow = Color(0xFFEEEAE2),            // Unselected day chips, progress bar card, sheet bg
    surfaceContainer = Color(0xFFFFFFFF),               // Task cards, form containers
    surfaceContainerHigh = Color(0xFFE6E1D8),           // Dial boxes, unselected pills
    surfaceContainerHighest = Color(0xFFDCD5C9)         // Tracks, category badge container
)

val DaytileShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(48.dp)
)

@Composable
fun DaytileTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = DaytileShapes,
        content = content
    )
}
