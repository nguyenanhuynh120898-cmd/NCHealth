package tech.mmarca.openvitals.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.domain.preferences.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6FD9C1),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF13524A),
    onPrimaryContainer = Color(0xFFB8F3E4),
    secondary = Color(0xFFB2CCC5),
    onSecondary = Color(0xFF1D3530),
    secondaryContainer = Color(0xFF2E4A44),
    onSecondaryContainer = Color(0xFFD3EDE6),
    tertiary = Color(0xFFFFB3A6),
    onTertiary = Color(0xFF5C1710),
    tertiaryContainer = Color(0xFF7E2B22),
    onTertiaryContainer = Color(0xFFFFDAD4),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    // background == surface, as M3 prescribes; a darker value made every card float.
    background = SurfaceDark,
    onBackground = Color(0xFFE0E3E6),
    surface = SurfaceDark,
    onSurface = Color(0xFFE0E3E6),
    surfaceVariant = Color(0xFF40484D),
    onSurfaceVariant = Color(0xFFC0C8CE),
    outline = Color(0xFF8A9298),
    outlineVariant = Color(0xFF40484D),
    surfaceContainerLowest = Color(0xFF0B0F11),
    surfaceContainerLow = Color(0xFF15191B),
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = Color(0xFF303437),
    surfaceContainerHighest = Color(0xFF3B3F42),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF16806D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFF0E2),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4A635D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E8E1),
    onSecondaryContainer = Color(0xFF0C1F1B),
    tertiary = Color(0xFFD9473C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD4),
    onTertiaryContainer = Color(0xFF410001),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2F7F5),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF2F7F5),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDCE3E9),
    onSurfaceVariant = Color(0xFF40484D),
    outline = Color(0xFF70787E),
    outlineVariant = Color(0xFFC0C8CE),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7FAF9),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEAF2EF),
    surfaceContainerHighest = Color(0xFFE2ECE8),
)

/** The shape scale, one name per [Radii] step. `medium` is the 12dp card corner. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radii.xs),
    small = RoundedCornerShape(Radii.sm),
    medium = RoundedCornerShape(Radii.md),
    large = RoundedCornerShape(Radii.lg),
    extraLarge = RoundedCornerShape(Radii.xl),
)

@Composable
fun OpenVitalsTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    colorTheme: AppColorTheme = AppColorTheme.TEAL,
    content: @Composable () -> Unit,
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDarkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK,
        AppThemeMode.AMOLED -> true
    }
    val colorScheme = when {
        themeMode == AppThemeMode.AMOLED -> {
            val baseColorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(LocalContext.current)
            } else {
                DarkColorScheme.withColorTheme(colorTheme, dark = true)
            }
            baseColorScheme.toAmoledColorScheme()
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme.withColorTheme(colorTheme, dark = true)
        else -> LightColorScheme.withColorTheme(colorTheme, dark = false)
    }

    // Resolved once, so every screen reads the same answer.
    val reducedMotion = !animatorsEnabled()

    CompositionLocalProvider(
        LocalReducedMotion provides reducedMotion,
        LocalAppColorTheme provides colorTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

private fun ColorScheme.toAmoledColorScheme(): ColorScheme =
    copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF030303),
        surfaceContainer = Color(0xFF080808),
        surfaceContainerHigh = Color(0xFF101010),
        surfaceContainerHighest = Color(0xFF181818),
        surfaceVariant = Color(0xFF242424),
        outlineVariant = Color(0xFF3A3A3A),
    )

/** Whether the running scheme is the AMOLED one, told from its defining property. */
@Composable
fun isAmoledColorScheme(): Boolean {
    val colorScheme = MaterialTheme.colorScheme
    return colorScheme.background == Color.Black && colorScheme.surface == Color.Black
}

@Composable
fun accentSurfaceContainerColor(
    accentColor: Color,
    amoledAlpha: Float = 0.10f,
    fallback: Color? = null,
): Color =
    if (isAmoledColorScheme()) {
        accentColor.copy(alpha = amoledAlpha).compositeOver(Color.Black)
    } else {
        fallback ?: MaterialTheme.colorScheme.surfaceContainer
    }
