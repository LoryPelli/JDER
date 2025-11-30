package com.jder.ui.components
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
@Composable
fun ThemeToggleButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = isDarkTheme, label = "theme")
    val rotation by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "rotation"
    ) { dark ->
        if (dark) 180f else 0f
    }
    val iconColor by animateColorAsState(
        targetValue = if (isDarkTheme) {
            Color(0xFFFFC107)
        } else {
            Color(0xFFFF9800)
        },
        animationSpec = tween(durationMillis = 300),
        label = "color"
    )
    val raysAlpha by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 200)
        },
        label = "raysAlpha"
    ) { dark ->
        if (dark) 0f else 1f
    }
    IconButton(
        onClick = onToggle,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                if (isDarkTheme) {
                    val moonRadius = size.minDimension / 2 * 0.42f
                    drawCircle(
                        color = iconColor.copy(alpha = 0.3f),
                        radius = moonRadius * 1.4f,
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = iconColor.copy(alpha = 0.5f),
                        radius = moonRadius * 1.15f,
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = iconColor,
                        radius = moonRadius,
                        center = Offset(centerX, centerY)
                    )
                } else {
                    val sunRadius = size.minDimension / 2 * 0.35f
                    drawCircle(
                        color = iconColor,
                        radius = sunRadius,
                        center = Offset(centerX, centerY)
                    )
                    if (raysAlpha > 0.01f) {
                        rotate(degrees = rotation, pivot = Offset(centerX, centerY)) {
                            val rayLength = size.minDimension / 2 * 0.35f
                            val rayDistance = size.minDimension / 2 * 0.5f
                            val rayThickness = 2.5f
                            for (i in 0 until 8) {
                                val angle = (i * 45f) * (PI / 180f).toFloat()
                                val startX = centerX + cos(angle) * rayDistance
                                val startY = centerY + sin(angle) * rayDistance
                                val endX = centerX + cos(angle) * (rayDistance + rayLength)
                                val endY = centerY + sin(angle) * (rayDistance + rayLength)
                                drawLine(
                                    color = iconColor.copy(alpha = raysAlpha),
                                    start = Offset(startX, startY),
                                    end = Offset(endX, endY),
                                    strokeWidth = rayThickness
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
