package tech.mmarca.openvitals.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/*
 * How screens change. Forward: the new screen slides in from a short way to the right and
 * fades up while the old one shrinks back a touch and dims. Back is the mirror. Short travel
 * and long, soft easing keep it smooth rather than showy.
 */

private const val EnterMillis = 420
private const val ExitMillis = 300

internal fun screenEnterTransition(): EnterTransition =
    slideInHorizontally(tween(EnterMillis, easing = FastOutSlowInEasing)) { width -> width / 6 } +
        fadeIn(tween(EnterMillis, easing = LinearOutSlowInEasing))

internal fun screenExitTransition(): ExitTransition =
    scaleOut(tween(ExitMillis, easing = FastOutSlowInEasing), targetScale = 0.96f) +
        fadeOut(tween(ExitMillis))

internal fun screenPopEnterTransition(): EnterTransition =
    scaleIn(tween(EnterMillis, easing = FastOutSlowInEasing), initialScale = 0.96f) +
        fadeIn(tween(EnterMillis, easing = LinearOutSlowInEasing))

internal fun screenPopExitTransition(): ExitTransition =
    slideOutHorizontally(tween(ExitMillis, easing = FastOutSlowInEasing)) { width -> width / 6 } +
        fadeOut(tween(ExitMillis))
