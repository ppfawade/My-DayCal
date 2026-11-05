package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Obsidian Kinetic Typography — Geist with compulsory Tabular Numerals ('tnum')
val GeistFontFamily = FontFamily(
    Font(R.font.geist, FontWeight.Normal),
    Font(R.font.geist, FontWeight.Medium),
    Font(R.font.geist, FontWeight.SemiBold),
    Font(R.font.geist, FontWeight.Bold)
)

val TabularTimeStyle = TextStyle(
    fontFamily = GeistFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.26.sp,
    fontFeatureSettings = "tnum"
)

val Typography = Typography(
    // headline-xl: 40px, weight 600, lineHeight 48px, letterSpacing -0.03em
    displayLarge = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp,
        fontFeatureSettings = "tnum"
    ),
    // headline-xl-mobile: 30px, weight 600, lineHeight 36px, letterSpacing -0.025em
    displayMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.75).sp,
        fontFeatureSettings = "tnum"
    ),
    // headline-lg: 28px, weight 600, lineHeight 34px, letterSpacing -0.02em
    headlineLarge = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.56).sp,
        fontFeatureSettings = "tnum"
    ),
    // headline-md: 20px, weight 500, lineHeight 26px, letterSpacing -0.015em
    headlineMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = "tnum"
    ),
    // headline-sm: 16px, weight 500, lineHeight 22px, letterSpacing -0.01em
    headlineSmall = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.16).sp,
        fontFeatureSettings = "tnum"
    ),
    // headline-sm / card title: 16px, weight 500, lineHeight 22px
    titleLarge = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.16).sp,
        fontFeatureSettings = "tnum"
    ),
    // label-lg: 14px, weight 500, lineHeight 18px, letterSpacing 0.01em
    titleMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.14.sp,
        fontFeatureSettings = "tnum"
    ),
    // body-lg: 16px, weight 400, lineHeight 24px, letterSpacing -0.005em
    bodyLarge = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.08).sp,
        fontFeatureSettings = "tnum"
    ),
    // body-md: 14px, weight 400, lineHeight 20px, letterSpacing 0em
    bodyMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    ),
    // body-sm: 12px, weight 400, lineHeight 18px, letterSpacing 0.01em
    bodySmall = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.12.sp,
        fontFeatureSettings = "tnum"
    ),
    // tabular-time: 13px, weight 500, lineHeight 16px, letterSpacing 0.02em
    labelLarge = TabularTimeStyle,
    // label-md: 12px, weight 500, lineHeight 16px, letterSpacing 0.02em
    labelMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.24.sp,
        fontFeatureSettings = "tnum"
    ),
    // label-sm: 10px, weight 600, lineHeight 14px, letterSpacing 0.05em
    labelSmall = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = "tnum"
    )
)
