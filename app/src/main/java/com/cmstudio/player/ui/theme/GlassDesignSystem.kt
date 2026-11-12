package com.cmstudio.player.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cmstudio.player.ui.AppSettings

/**
 * Shared opt-in Liquid Glass / Material 3 tonal surface container.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    tint: Color? = null,
    opacity: Float = 0.92f,
    borderAlpha: Float = 0.35f,
    blurRadius: Dp = 0.dp,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isLiquidGlass by AppSettings.isLiquidGlassEnabled.collectAsState()
    val userFrostOpacity by AppSettings.glassFrostOpacity.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    val backgroundColor = when {
        tint != null -> {
            val a = if (isLiquidGlass) (opacity * (userFrostOpacity / 0.78f)).coerceIn(0.35f, 0.96f) else 0.94f
            tint.copy(alpha = a)
        }
        isLiquidGlass -> {
            val a = (opacity * (userFrostOpacity / 0.78f)).coerceIn(0.55f, 0.92f)
            colorScheme.surfaceContainerHigh.copy(alpha = a)
        }
        else -> colorScheme.surfaceContainerLow
    }

    val borderStroke = if (isLiquidGlass) {
        BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = borderAlpha.coerceIn(0.18f, 0.55f)))
    } else {
        null
    }

    val surfaceElevation = if (isLiquidGlass) elevation.coerceAtMost(8.dp) else 0.dp
    val effectiveBlur = if (isLiquidGlass && blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier

    Box(
        modifier = modifier
            .then(
                if (surfaceElevation > 0.dp) {
                    Modifier.shadow(
                        elevation = surfaceElevation,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.16f),
                        spotColor = Color.Black.copy(alpha = 0.24f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .then(effectiveBlur)
            .background(color = backgroundColor, shape = shape)
            .then(if (borderStroke != null) Modifier.border(borderStroke, shape = shape) else Modifier),
        content = content
    )
}
