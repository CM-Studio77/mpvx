package com.cmstudio.player.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 8-level Material 3 Expressive shape system for MPVX cards, sheets, dialogs, and buttons.
 */
object AppShapeScale {
    val extraSmall = RoundedCornerShape(4.dp)
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val largeIncreased = RoundedCornerShape(20.dp)
    val extraLarge = RoundedCornerShape(28.dp)
    val extraLargeIncreased = RoundedCornerShape(32.dp)
    val full = RoundedCornerShape(50)
}

val AppShapes = Shapes(
    extraSmall = AppShapeScale.extraSmall,
    small = AppShapeScale.small,
    medium = AppShapeScale.medium,
    large = AppShapeScale.large,
    extraLarge = AppShapeScale.extraLarge
)
