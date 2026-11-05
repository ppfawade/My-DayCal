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

// Dark Theme - Obsidian Slate & Warm Solar Accents
val ObsidianBackground = Color(0xFF0B0F17)
val ObsidianSurface = Color(0xFF131B2E)
val ObsidianSurfaceVariant = Color(0xFF1E293B)
val SlateTextPrimary = Color(0xFFF8FAFC)
val SlateTextSecondary = Color(0xFF94A3B8)
val SolarAmber = Color(0xFFF59E0B)
val SolarAmberContainer = Color(0xFF3B2506)
val CyberTeal = Color(0xFF14B8A6)
val CyberTealContainer = Color(0xFF0A322E)
val CoralRose = Color(0xFFF43F5E)

// 6 High-Contrast Tile Accent Colors (Index 0..5)
data class TilePalette(
    val accent: Color,
    val containerLight: Color,
    val containerDark: Color,
    val onContainerLight: Color,
    val onContainerDark: Color
)

val DaytilePalettes = listOf(
    // 0: Amber (Deep Focus)
    TilePalette(
        accent = Color(0xFFF59E0B),
        containerLight = Color(0xFFFEF3C7),
        containerDark = Color(0xFF332409),
        onContainerLight = Color(0xFF78350F),
        onContainerDark = Color(0xFFFDE68A)
    ),
    // 1: Cobalt (Work)
    TilePalette(
        accent = Color(0xFF3B82F6),
        containerLight = Color(0xFFDBEAFE),
        containerDark = Color(0xFF12264A),
        onContainerLight = Color(0xFF1E3A8A),
        onContainerDark = Color(0xFFBFDBFE)
    ),
    // 2: Emerald (Personal)
    TilePalette(
        accent = Color(0xFF10B981),
        containerLight = Color(0xFFD1FAE5),
        containerDark = Color(0xFF0B3126),
        onContainerLight = Color(0xFF065F46),
        onContainerDark = Color(0xFFA7F3D0)
    ),
    // 3: Coral (Health & Move)
    TilePalette(
        accent = Color(0xFFF43F5E),
        containerLight = Color(0xFFFFE4E6),
        containerDark = Color(0xFF3B121D),
        onContainerLight = Color(0xFF881337),
        onContainerDark = Color(0xFFFECDD3)
    ),
    // 4: Violet (Daily Ritual)
    TilePalette(
        accent = Color(0xFF8B5CF6),
        containerLight = Color(0xFFEDE9FE),
        containerDark = Color(0xFF26174A),
        onContainerLight = Color(0xFF4C1D95),
        onContainerDark = Color(0xFFDDD6FE)
    ),
    // 5: Teal Cyan (Creative)
    TilePalette(
        accent = Color(0xFF06B6D4),
        containerLight = Color(0xFFCFFAFE),
        containerDark = Color(0xFF0C2E38),
        onContainerLight = Color(0xFF164E63),
        onContainerDark = Color(0xFFA5F3FC)
    )
)

fun getTilePalette(index: Int): TilePalette =
    DaytilePalettes[index.mod(DaytilePalettes.size)]
