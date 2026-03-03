package com.example.monytix.ui.theme

import androidx.compose.ui.graphics.Color

// Compatibility aliases – app screens reference these; they map to the AI Cyan palette.
// Canonical palette is in Color.kt (Background, SurfacePrimary, SurfaceSecondary, TextPrimary,
// TextSecondary, CyanPrimary, CyanSecondary, CyanGlow, BorderSubtle, Success, ErrorRed).

val AccentPrimary = CyanPrimary
val AccentSecondary = CyanSecondary
val AccentMuted = CyanGlow
val GlassCard = SurfaceSecondary
val Error = ErrorRed
val SurfaceDark = Background
val SurfaceElevated = SurfacePrimary
val BackgroundGradientTop = Background
val BackgroundGradientBottom = Background
val TextMuted = TextSecondary
val Warning = Color(0xFFFB923C)
val Info = CyanSecondary

// Charts – reuse cyan/semantic palette
val ChartBlue = CyanPrimary
val ChartRed = ErrorRed
val ChartGreen = Success
val ChartOrange = Warning
val ChartPurple = CyanSecondary

// Legacy names used by HomeScreen, GoalTrackerScreen, SpendSenseScreen
val HeroCardGlow = CyanGlow
val BannerPurple = CyanGlow
