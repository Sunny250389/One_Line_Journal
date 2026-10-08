package com.onelinejournal.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/** Streak length at which the flame reaches full strength. */
private const val FULL_FLAME_STREAK = 30f

/**
 * A small fire that grows, brightens and gains side flames as the streak lengthens.
 * The flicker is deliberately slow and shallow so it stays calm next to the journal.
 */
@Composable
fun StreakFlame(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val level by animateFloatAsState(
        targetValue = (streak / FULL_FLAME_STREAK).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "flameLevel"
    )
    val transition = rememberInfiniteTransition(label = "flame")
    val flicker by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )
    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )
    val ember = streak <= 0

    Canvas(modifier = modifier.size(width = 68.dp, height = 76.dp)) {
        val centerX = size.width / 2f
        val bottom = size.height - 4.dp.toPx()
        val alpha = if (ember) 0.4f else 1f

        // Glow behind the fire gets wider and warmer with the streak.
        val glowRadius = lerp(22.dp.toPx(), 62.dp.toPx(), level)
        val glowAlpha = lerp(0.08f, 0.55f, level) * (0.85f + 0.15f * flicker) * alpha
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFB300).copy(alpha = glowAlpha), Color.Transparent),
                center = Offset(centerX, bottom - size.height * 0.28f),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(centerX, bottom - size.height * 0.28f)
        )

        val mainWidth = lerp(26.dp.toPx(), 44.dp.toPx(), level)
        val mainHeight = lerp(34.dp.toPx(), 66.dp.toPx(), level)

        if (level > 0.45f) {
            val sideScale = lerp(0.35f, 0.58f, (level - 0.45f) / 0.55f)
            drawFlame(centerX - mainWidth * 0.62f, bottom, mainWidth * sideScale, mainHeight * sideScale, level, flicker, -sway, alpha)
            drawFlame(centerX + mainWidth * 0.62f, bottom, mainWidth * sideScale, mainHeight * sideScale, level, 1f - flicker, sway, alpha)
        }
        drawFlame(centerX, bottom, mainWidth, mainHeight, level, flicker, sway, alpha)
    }
}

private fun DrawScope.drawFlame(
    centerX: Float,
    bottom: Float,
    width: Float,
    height: Float,
    level: Float,
    flicker: Float,
    sway: Float,
    alpha: Float
) {
    val animatedHeight = height * (0.94f + 0.08f * flicker)
    rotate(degrees = sway * 3f, pivot = Offset(centerX, bottom)) {
        // Outer, middle and inner tongues — all colours warm up as the streak grows.
        drawPath(
            path = flamePath(centerX, bottom, width, animatedHeight),
            brush = Brush.verticalGradient(
                colors = listOf(
                    lerp(Color(0xFFFF8A50), Color(0xFFFFB300), level),
                    lerp(Color(0xFFE64A19), Color(0xFFFF6D00), level)
                ),
                startY = bottom - animatedHeight,
                endY = bottom
            ),
            alpha = alpha
        )
        drawPath(
            path = flamePath(centerX, bottom, width * 0.66f, animatedHeight * 0.68f),
            color = lerp(Color(0xFFFFB74D), Color(0xFFFFEB3B), level),
            alpha = alpha
        )
        drawPath(
            path = flamePath(centerX, bottom, width * 0.34f, animatedHeight * 0.38f),
            color = lerp(Color(0xFFFFE0B2), Color(0xFFFFFFFF), level),
            alpha = alpha
        )
    }
}

/** A teardrop with its tip at the top, sitting on [bottom] and centred on [centerX]. */
private fun flamePath(centerX: Float, bottom: Float, width: Float, height: Float): Path {
    val left = centerX - width / 2f
    val top = bottom - height
    fun x(f: Float) = left + f * width
    fun y(f: Float) = top + f * height
    return Path().apply {
        moveTo(x(0.5f), y(0f))
        cubicTo(x(0.58f), y(0.26f), x(0.96f), y(0.44f), x(0.96f), y(0.68f))
        cubicTo(x(0.96f), y(0.88f), x(0.76f), y(1f), x(0.5f), y(1f))
        cubicTo(x(0.24f), y(1f), x(0.04f), y(0.88f), x(0.04f), y(0.68f))
        cubicTo(x(0.04f), y(0.5f), x(0.34f), y(0.38f), x(0.5f), y(0f))
        close()
    }
}
