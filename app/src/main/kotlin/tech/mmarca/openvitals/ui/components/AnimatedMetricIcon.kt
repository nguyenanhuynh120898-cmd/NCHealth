package tech.mmarca.openvitals.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.ui.theme.Motion
import tech.mmarca.openvitals.ui.theme.loopingMotionAllowed

/**
 * A metric's icon in its tinted circle. While the value loads the glyph breathes; when the
 * value lands it pops in with a small spring and tilt, once. Reduced motion keeps it still.
 */
@Composable
fun AnimatedMetricIcon(
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    settled: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    glyphSize: Dp = 16.dp,
) {
    val motionAllowed = loopingMotionAllowed()
    val scale = remember { Animatable(1f) }
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(settled, motionAllowed) {
        if (settled && motionAllowed) {
            scale.snapTo(0.6f)
            tilt.snapTo(-14f)
            launch {
                scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
            }
            tilt.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
        }
    }
    val breath = if (!settled && motionAllowed) {
        rememberInfiniteTransition(label = "MetricIconBreath").animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(Motion.skeletonPulseMillis / 2),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "MetricIconBreathAlpha",
        ).value
    } else {
        1f
    }
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = tilt.value
            }
            .background(color = containerColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
                .size(glyphSize)
                .graphicsLayer { alpha = breath },
        )
    }
}
