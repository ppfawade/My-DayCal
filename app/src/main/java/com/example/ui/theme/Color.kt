package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme - Warm Architectural Ivory & Deep Ink
val IvoryBackground = Color(0xFFFAF8F5)
val IvorySurface = Color(0xFFFFFFFF)
val IvorySurfaceVariant = Color(0xFFF2ECE4)
val InkPrimary = Color(0xFF141824)
val InkSecondary = Color(0xFF525866)
val AmberAccent = Color(0xFFD97706)
val AmberContainer = Color(0xFFFEF3C7)
val TealSecondary = Color(0xFF0D9488)
val TealContainer = Color(0xFFCCFBF1)
val TerracottaTertiary = Color(0xFFE11D48)

// Dark Theme - Deep Obsidian & Warm Solar Amber / Emerald Accents
val ObsidianBackground = Color(0xFF070A11)
val ObsidianSurface = Color(0xFF0F141F)
val ObsidianSurfaceElevated = Color(0xFF151C2B)
val ObsidianSurfaceVariant = Color(0xFF1A2234)
val ObsidianBorder = Color(0xFF232E42)
val SlateTextPrimary = Color(0xFFF8FAFC)
val SlateTextSecondary = Color(0xFF94A3B8)
val SolarAmber = Color(0xFFF59E0B)
val SolarAmberContainer = Color(0xFF281B08)
val SolarAmberBorder = Color(0xFF78450A)
val CyberTeal = Color(0xFF10B981)
val CyberTealContainer = Color(0xFF09261E)
val CyberTealBorder = Color(0xFF10B981)
val FocusBluePillBg = Color(0xFF101C2E)
val FocusBluePillBorder = Color(0xFF1E3A5F)
val FocusBlueText = Color(0xFF38BDF8)
val CoralRose = Color(0xFFF43F5E)

// 6 High-Contrast Tile Accent Colors (Index 0..5)
data class TilePalette(
    val accent: Color,
    val containerLight: Color,
    val containerDark: Color,
    val borderDark: Color,
    val onContainerLight: Color,
    val onContainerDark: Color
)

val DaytilePalettes = listOf(
    // 0: Focus (Amber Gold)
    TilePalette(
        accent = Color(0xFFF59E0B),
        containerLight = Color(0xFFFEF3C7),
        containerDark = Color(0xFF1E1910),
        borderDark = Color(0xFF5C4012),
        onContainerLight = Color(0xFF78350F),
        onContainerDark = Color(0xFFFDE68A)
    ),
    // 1: Work (Cobalt Cyan)
    TilePalette(
        accent = Color(0xFF38BDF8),
        containerLight = Color(0xFFDBEAFE),
        containerDark = Color(0xFF101D2E),
        borderDark = Color(0xFF1E3F66),
        onContainerLight = Color(0xFF1E3A8A),
        onContainerDark = Color(0xFFBAE6FD)
    ),
    // 2: Personal (Emerald)
    TilePalette(
        accent = Color(0xFF10B981),
        containerLight = Color(0xFFD1FAE5),
        containerDark = Color(0xFF0D211C),
        borderDark = Color(0xFF14533E),
        onContainerLight = Color(0xFF065F46),
        onContainerDark = Color(0xFFA7F3D0)
    ),
    // 3: Health (Coral Rose)
    TilePalette(
        accent = Color(0xFFF43F5E),
        containerLight = Color(0xFFFFE4E6),
        containerDark = Color(0xFF221118),
        borderDark = Color(0xFF5E1B2B),
        onContainerLight = Color(0xFF881337),
        onContainerDark = Color(0xFFFECDD3)
    ),
    // 4: Break (Violet Indigo)
    TilePalette(
        accent = Color(0xFF8B5CF6),
        containerLight = Color(0xFFEDE9FE),
        containerDark = Color(0xFF19132B),
        borderDark = Color(0xFF432A74),
        onContainerLight = Color(0xFF4C1D95),
        onContainerDark = Color(0xFFDDD6FE)
    ),
    // 5: Creative (Teal Cyan)
    TilePalette(
        accent = Color(0xFF06B6D4),
        containerLight = Color(0xFFCFFAFE),
        containerDark = Color(0xFF0C222B),
        borderDark = Color(0xFF164E63),
        onContainerLight = Color(0xFF164E63),
        onContainerDark = Color(0xFFA5F3FC)
    )
)

fun getTilePalette(index: Int): TilePalette =
    DaytilePalettes[index.mod(DaytilePalettes.size)]
