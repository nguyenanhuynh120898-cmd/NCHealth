package tech.mmarca.openvitals.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.ui.theme.Motion
import tech.mmarca.openvitals.ui.theme.loopingMotionAllowed

/**
 * A soft highlight sweeping across the element while its value is on its way, in place
 * of a "Loading…" label. Under reduced motion it is a still, tinted block.
 */
fun Modifier.shimmer(): Modifier = composed {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    val progress = if (loopingMotionAllowed()) {
        rememberInfiniteTransition(label = "Shimmer").animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(Motion.ambientMillis, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "ShimmerProgress",
        ).value
    } else {
        0.5f
    }
    drawWithContent {
        val width = size.width
        val start = width * progress
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(start - width * 0.5f, 0f),
                end = Offset(start + width * 0.5f, size.height),
            ),
        )
    }
}

/** One rounded placeholder line, the size of the text it stands in for. */
@Composable
fun ShimmerLine(
    width: Dp,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(CircleShape)
            .shimmer(),
    )
}
