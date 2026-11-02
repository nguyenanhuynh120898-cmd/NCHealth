package tech.mmarca.openvitals.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import tech.mmarca.openvitals.ui.theme.LocalAppColorTheme
import tech.mmarca.openvitals.ui.theme.loopingMotionAllowed
import kotlin.math.cos
import kotlin.math.sin

/**
 * The ground the glass cards float on: the theme's background with three soft glows in the
 * colour theme's three tones, drifting very slowly. Reduced motion holds them still.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val glowAlpha = if (dark) 0.20f else 0.30f
    // The three glows follow the chosen colour theme.
    val glows = LocalAppColorTheme.current.glows
    val mint = glows[0].copy(alpha = glowAlpha)
    val teal = glows[1].copy(alpha = glowAlpha * 0.8f)
    val coral = glows[2].copy(alpha = glowAlpha * 0.55f)
    val phaseState: State<Float> = if (loopingMotionAllowed()) {
        rememberInfiniteTransition(label = "Aurora").animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(AuroraCycleMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "AuroraPhase",
        )
    } else {
        StillPhase
    }
    Canvas(modifier = modifier) {
        val phase = phaseState.value
        drawRect(scheme.background)
        val w = size.width
        val h = size.height
        fun glow(color: Color, cx: Float, cy: Float, radius: Float) = drawCircle(
            brush = Brush.radialGradient(listOf(color, Color.Transparent), center = Offset(cx, cy), radius = radius),
            radius = radius,
            center = Offset(cx, cy),
        )
        glow(mint, w * (0.15f + 0.05f * sin(phase)), h * (0.08f + 0.03f * cos(phase)), w * 0.9f)
        glow(teal, w * (0.95f + 0.05f * cos(phase)), h * (0.42f + 0.04f * sin(phase)), w * 0.8f)
        glow(coral, w * (0.25f + 0.06f * cos(phase)), h * (0.9f + 0.03f * sin(phase)), w * 0.75f)
    }
}

private const val AuroraCycleMillis = 24_000
private val StillPhase: State<Float> = mutableFloatStateOf(0f)
