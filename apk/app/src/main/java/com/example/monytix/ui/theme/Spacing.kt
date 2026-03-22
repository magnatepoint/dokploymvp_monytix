package com.example.monytix.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design tokens for Home and Future screens (spec: primary/secondary/supporting hierarchy).
 * Use between cards and inside cards for consistent premium fintech layout.
 */
object MonytixSpacing {
    /** Padding inside primary card (e.g. Risk Hero). */
    val cardPaddingPrimary = 24.dp
    /** Padding inside secondary cards (Health, Forecast teaser). */
    val cardPaddingSecondary = 20.dp
    /** Padding inside supporting cards (Goal, CTA, Insight, rows). */
    val cardPaddingCompact = 16.dp

    /** Vertical spacing between major sections. */
    val betweenSections = 20.dp
    /** Vertical spacing between cards. */
    val betweenCards = 16.dp
    /** Tighter spacing within a group (e.g. insight cards). */
    val betweenCardsTight = 12.dp
}
