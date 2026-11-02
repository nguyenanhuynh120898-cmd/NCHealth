package tech.mmarca.openvitals.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.ui.theme.LocalReducedMotion
import tech.mmarca.openvitals.ui.theme.Motion

/**
 * The element rises a little and fades in when it first appears, [index] steps after the
 * first one, so a screen fills in as a soft cascade instead of all at once. Runs once per
 * composition; reduced motion shows it at rest straight away.
 */
fun Modifier.staggeredEntrance(index: Int): Modifier = composed {
    val reduced = LocalReducedMotion.current
    // Saved with the list item: a fast scroll back up finds it in place, not fading in again.
    var entered by rememberSaveable { mutableStateOf(reduced) }
    val progress = remember { Animatable(if (entered) 1f else 0f) }
    val rise = with(androidx.compose.ui.platform.LocalDensity.current) { EntranceRise.toPx() }
    LaunchedEffect(Unit) {
        if (entered) return@LaunchedEffect
        entered = true
        delay(index.coerceIn(0, MaxSteps) * StepMillis)
        launch {
            progress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow))
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * rise
        val scale = 0.97f + 0.03f * p
        scaleX = scale
        scaleY = scale
    }
}

/** A plain fade, for things that should not move (a list that is already on screen). */
fun Modifier.fadeInOnce(): Modifier = composed {
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alphaAnim.animateTo(1f, tween(Motion.standardMillis)) }
    graphicsLayer { alpha = alphaAnim.value }
}

private val EntranceRise = 24.dp
private const val StepMillis = 70L
private const val MaxSteps = 8
