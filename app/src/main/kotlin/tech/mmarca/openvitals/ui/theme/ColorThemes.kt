package tech.mmarca.openvitals.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's colour themes. Each sets the brand colour of buttons, selections and charts'
 * chrome, and the three glows of the liquid-glass ground. Metric accents stay as they are,
 * so a metric keeps its colour whichever theme is on.
 */
enum class AppColorTheme(
    val primaryLight: Color,
    val primaryDark: Color,
    val containerLight: Color,
    val containerDark: Color,
    val glows: List<Color>,
) {
    TEAL(
        Color(0xFF16806D), Color(0xFF6FD9C1), Color(0xFFBFF0E2), Color(0xFF13524A),
        listOf(Color(0xFF7FCDB3), Color(0xFF16806D), Color(0xFFF47462)),
    ),
    OCEAN(
        Color(0xFF1F6FB2), Color(0xFF8CC4F5), Color(0xFFD3E6FA), Color(0xFF154A73),
        listOf(Color(0xFF7FB8E6), Color(0xFF3D8FD9), Color(0xFF8FD3C8)),
    ),
    SUNSET(
        Color(0xFFC2552F), Color(0xFFFFB59A), Color(0xFFFFDBCF), Color(0xFF7A2E14),
        listOf(Color(0xFFF7A985), Color(0xFFF47462), Color(0xFFF5C66B)),
    ),
    LAVENDER(
        Color(0xFF6A55C2), Color(0xFFC8BCFF), Color(0xFFE6DEFF), Color(0xFF4A3A93),
        listOf(Color(0xFFB7A6F2), Color(0xFF8A73E6), Color(0xFFF2A6C9)),
    ),
    FOREST(
        Color(0xFF3E7D3A), Color(0xFF9FD68F), Color(0xFFD6F0CD), Color(0xFF24521F),
        listOf(Color(0xFFA6D99A), Color(0xFF5FA857), Color(0xFFE3D27A)),
    ),
}

/** The theme in force, for the aurora ground. */
val LocalAppColorTheme = compositionLocalOf { AppColorTheme.TEAL }

/** What the settings card needs to show and change the theme. Null outside the main activity. */
class ColorThemeController(val selected: AppColorTheme, val select: (AppColorTheme) -> Unit)

val LocalColorThemeController = staticCompositionLocalOf<ColorThemeController?> { null }

internal fun ColorScheme.withColorTheme(theme: AppColorTheme, dark: Boolean): ColorScheme =
    if (dark) {
        copy(
            primary = theme.primaryDark,
            onPrimary = Color(0xFF0E1614),
            primaryContainer = theme.containerDark,
            onPrimaryContainer = Color(0xFFE8F2EF),
            secondaryContainer = theme.containerDark,
        )
    } else {
        copy(
            primary = theme.primaryLight,
            onPrimary = Color.White,
            primaryContainer = theme.containerLight,
            onPrimaryContainer = Color(0xFF0E1614),
            secondaryContainer = theme.containerLight,
        )
    }

