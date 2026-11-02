package tech.mmarca.openvitals.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import tech.mmarca.openvitals.R
import java.time.LocalTime

/** "Good morning" and its siblings, rechecked each minute so an open app keeps up. */
@Composable
internal fun timeOfDayGreeting(): String {
    var hour by remember { mutableIntStateOf(LocalTime.now().hour) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(GreetingRefreshMillis)
            hour = LocalTime.now().hour
        }
    }
    return stringResource(
        when (hour) {
            in 5..10 -> R.string.greeting_morning
            in 11..12 -> R.string.greeting_noon
            in 13..17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        },
    )
}

private const val GreetingRefreshMillis = 60_000L
