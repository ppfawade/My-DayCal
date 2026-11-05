package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Obsidian Kinetic Design System — Color Tiers & Roles
val ObsidianCanvasLowest = Color(0xFF0C0E14)       // Tier 0: Base Canvas (#0c0e14)
val ObsidianBackground = Color(0xFF0C0E14)         // Foundational obsidian canvas
val ObsidianM3Surface = Color(0xFF111319)          // M3 surface / surface-dim (#111319)
val ObsidianSurface = Color(0xFF121620)            // Tier 1: Structural Sections & Resting Cards (#121620)
val ObsidianSurfaceLow = Color(0xFF191B22)         // surface-container-low (#191b22)
val ObsidianSurfaceContainer = Color(0xFF1E1F26)   // surface-container (#1e1f26)
val ObsidianSurfaceElevated = Color(0xFF1A202C)    // Tier 2: Interactive Modules / Hover / Popovers (#1a202c)
val ObsidianSurfaceHigh = Color(0xFF282A30)        // surface-container-high (#282a30)
val ObsidianSurfaceVariant = Color(0xFF33343B)     // surface-container-highest / surface-variant (#33343b)
val ObsidianSurfaceBright = Color(0xFF373940)      // surface-bright (#373940)

// Borders & Hairlines
val ObsidianBorder = Color(0x12FFFFFF)             // border-hairline: rgba(255, 255, 255, 0.07)
val ObsidianSectionBorder = Color(0x0DFFFFFF)      // Tier 1 border: rgba(255, 255, 255, 0.05)
val ObsidianCardBorder = Color(0x0FFFFFFF)         // Card border: rgba(255, 255, 255, 0.06)
val ObsidianInteractiveBorder = Color(0x14FFFFFF)  // Tier 2 border: rgba(255, 255, 255, 0.08)
val ObsidianGhostBorder = Color(0x1FFFFFFF)        // Secondary ghost border: rgba(255, 255, 255, 0.12)
val ObsidianCheckboxBorder = Color(0x33FFFFFF)     // Checkbox border: rgba(255, 255, 255, 0.20)
val ObsidianChipFill = Color(0x0AFFFFFF)           // Chip fill: rgba(255, 255, 255, 0.04)
val AmberRimBorder = Color(0x40F59E0B)             // Tier 3 modal rim: rgba(245, 158, 11, 0.25)

// Typography Contrast Tiers
val SlateTextPrimary = Color(0xFFF8FAFC)           // text-primary (#f8fafc)
val SlateTextSecondary = Color(0xFF94A3B8)         // text-secondary (#94a3b8)
val SlateTextMuted = Color(0xFF475569)             // text-muted (#475569)
val ObsidianOnSurface = Color(0xFFE2E2EB)          // on-surface (#e2e2eb)
val ObsidianOnSurfaceVariant = Color(0xFFD8C3AD)   // on-surface-variant (#d8c3ad)
val ObsidianOutline = Color(0xFFA08E7A)            // outline (#a08e7a)
val ObsidianOutlineVariant = Color(0xFF534434)     // outline-variant (#534434)

// Primary Accent — Radiant Amber (#f59e0b / #ffc174)
val SolarAmber = Color(0xFFF59E0B)                 // primary-container / Radiant Amber (#f59e0b)
val SolarAmberPrimary = Color(0xFFFFC174)          // primary (#ffc174)
val SolarAmberContainer = Color(0x1FF59E0B)        // selected chip tint: rgba(245, 158, 11, 0.12)
val SolarAmberBorder = Color(0x4DF59E0B)           // rgba(245, 158, 11, 0.30)

// Secondary Accent — Hyper Sky (#38bdf8 / #7bd0ff)
val HyperSky = Color(0xFF38BDF8)                   // Secondary Accent (#38bdf8)
val HyperSkyLight = Color(0xFF7BD0FF)              // secondary (#7bd0ff)
val FocusBluePillBg = Color(0x1A38BDF8)            // rgba(56, 189, 248, 0.10)
val FocusBluePillBorder = Color(0x4038BDF8)        // rgba(56, 189, 248, 0.25)
val FocusBlueText = Color(0xFF38BDF8)              // Hyper Sky (#38bdf8)

// Tertiary Accent — Mint Signal (#10b981 / #56e5a9)
val CyberTeal = Color(0xFF10B981)                  // Mint Signal (#10b981)
val MintSignalLight = Color(0xFF56E5A9)            // tertiary (#56e5a9)
val CyberTealContainer = Color(0x1F10B981)         // rgba(16, 185, 129, 0.12)
val CyberTealBorder = Color(0x4D10B981)            // rgba(16, 185, 129, 0.30)

// Error & Coral Accents
val CoralRose = Color(0xFFF43F5E)
val KineticError = Color(0xFFFFB4AB)
val KineticOnError = Color(0xFF690005)
val KineticErrorContainer = Color(0xFF93000A)
val KineticOnErrorContainer = Color(0xFFFFDAD6)

// Light Theme Fallbacks
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

// 6 High-Contrast Tile Accent Colors (Index 0..5) for Obsidian Kinetic Cards
// Cards rest on #121620 with 1px rgba(255,255,255,0.06) border and a 3dp left-edge category strip
data class TilePalette(
    val accent: Color,
    val containerLight: Color,
    val containerDark: Color,
    val borderDark: Color,
    val onContainerLight: Color,
    val onContainerDark: Color
)

val DaytilePalettes = listOf(
    // 0: Focus (Radiant Amber #f59e0b)
    TilePalette(
        accent = Color(0xFFF59E0B),
        containerLight = Color(0xFFFEF3C7),
        containerDark = Color(0xFF121620),
        borderDark = Color(0x26F59E0B),
        onContainerLight = Color(0xFF78350F),
        onContainerDark = Color(0xFFFFDDB8)
    ),
    // 1: Work (Hyper Sky #38bdf8)
    TilePalette(
        accent = Color(0xFF38BDF8),
        containerLight = Color(0xFFDBEAFE),
        containerDark = Color(0xFF121620),
        borderDark = Color(0x2638BDF8),
        onContainerLight = Color(0xFF1E3A8A),
        onContainerDark = Color(0xFFC4E7FF)
    ),
    // 2: Personal (Mint Signal #10b981)
    TilePalette(
        accent = Color(0xFF10B981),
        containerLight = Color(0xFFD1FAE5),
        containerDark = Color(0xFF121620),
        borderDark = Color(0x2610B981),
        onContainerLight = Color(0xFF065F46),
        onContainerDark = Color(0xFF6FFBBE)
    ),
    // 3: Health (Coral Rose #f43f5e)
    TilePalette(
        accent = Color(0xFFF43F5E),
        containerLight = Color(0xFFFFE4E6),
        containerDark = Color(0xFF121620),
        borderDark = Color(0x26F43F5E),
        onContainerLight = Color(0xFF881337),
        onContainerDark = Color(0xFFFECDD3)
    ),
    // 4: Break (Kinetic Violet #8b5cf6)
    TilePalette(
        accent = Color(0xFF8B5CF6),
        containerLight = Color(0xFFEDE9FE),
        containerDark = Color(0xFF121620),
        borderDark = Color(0x268B5CF6),
        onContainerLight = Color(0xFF4C1D95),
        onContainerDark = Color(0xFFDDD6FE)
    ),
    // 5: Creative (Cyan Pulse #06b6d4)
    TilePalette(
        accent = Color(0xFF06B6D4),
        containerLight = Color(0xFFCFFAFE),
        containerDark = Color(0xFF121620),
        borderDark = Color(0x2606B6D4),
        onContainerLight = Color(0xFF164E63),
        onContainerDark = Color(0xFFA5F3FC)
    )
)

fun getTilePalette(index: Int): TilePalette =
    DaytilePalettes[index.mod(DaytilePalettes.size)]
