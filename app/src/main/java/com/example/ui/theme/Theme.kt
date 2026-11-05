package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SolarAmberPrimary,
    onPrimary = Color(0xFF472A00),
    primaryContainer = SolarAmber,
    onPrimaryContainer = Color(0xFF613B00),
    inversePrimary = Color(0xFF855300),
    secondary = HyperSkyLight,
    onSecondary = Color(0xFF00354A),
    secondaryContainer = Color(0xFF00A6E0),
    onSecondaryContainer = Color(0xFF00374D),
    tertiary = MintSignalLight,
    onTertiary = Color(0xFF003824),
    tertiaryContainer = Color(0xFF30C88F),
    onTertiaryContainer = Color(0xFF004E34),
    background = ObsidianBackground,
    onBackground = ObsidianOnSurface,
    surface = ObsidianM3Surface,
    onSurface = ObsidianOnSurface,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianOnSurfaceVariant,
    surfaceTint = Color(0xFFFFB95F),
    inverseSurface = ObsidianOnSurface,
    inverseOnSurface = Color(0xFF2E3037),
    error = KineticError,
    onError = KineticOnError,
    errorContainer = KineticErrorContainer,
    onErrorContainer = KineticOnErrorContainer,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineVariant,
    surfaceBright = ObsidianSurfaceBright,
    surfaceDim = ObsidianM3Surface,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = ObsidianSurfaceVariant,
    surfaceContainerLow = ObsidianSurfaceLow,
    surfaceContainerLowest = ObsidianCanvasLowest
)

private val LightColorScheme = lightColorScheme(
    primary = AmberAccent,
    onPrimary = Color.White,
    primaryContainer = AmberContainer,
    onPrimaryContainer = Color(0xFF78350F),
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = TerracottaTertiary,
    background = IvoryBackground,
    onBackground = InkPrimary,
    surface = IvorySurface,
    onSurface = InkPrimary,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = Color(0xFFD6D0C4)
)

// Obsidian Kinetic Shapes (sm: 8dp, DEFAULT: 16dp, md: 24dp, lg: 32dp, full: 9999px)
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
