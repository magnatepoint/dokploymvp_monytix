package com.example.monytix.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Spec: Primary 20–24dp, Secondary 16–20dp, Supporting 12–16dp. */
object MonytixRadius {
    val primary = 24.dp
    val secondary = 16.dp
    val compact = 12.dp
}

val MonytixShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(MonytixRadius.compact),
    large = RoundedCornerShape(MonytixRadius.secondary),
    extraLarge = RoundedCornerShape(MonytixRadius.primary)
)
