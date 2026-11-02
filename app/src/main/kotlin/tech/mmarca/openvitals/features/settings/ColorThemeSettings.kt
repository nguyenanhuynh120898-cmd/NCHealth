package tech.mmarca.openvitals.features.settings

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.AppColorTheme
import tech.mmarca.openvitals.ui.theme.LocalColorThemeController
import javax.inject.Inject
import javax.inject.Singleton

/** The chosen colour theme, kept on this phone. */
@Singleton
class ColorThemeSettings @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("nchealth_color_theme", Context.MODE_PRIVATE)
    private val _theme = MutableStateFlow(
        prefs.getString(KeyTheme, null)
            ?.let { name -> AppColorTheme.entries.firstOrNull { it.name == name } }
            ?: AppColorTheme.TEAL,
    )
    val theme: StateFlow<AppColorTheme> = _theme.asStateFlow()

    fun select(theme: AppColorTheme) {
        prefs.edit().putString(KeyTheme, theme.name).apply()
        _theme.value = theme
    }

    private companion object {
        const val KeyTheme = "theme"
    }
}

/** A row of swatches, one per theme; the chosen one wears a ring and grows a little. */
@Composable
fun ColorThemeCard(modifier: Modifier = Modifier) {
    val controller = LocalColorThemeController.current ?: return
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(stringResource(R.string.settings_color_theme_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.settings_color_theme_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.md),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppColorTheme.entries.forEach { theme ->
                    ThemeSwatch(theme, theme == controller.selected) { controller.select(theme) }
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(theme: AppColorTheme, selected: Boolean, onClick: () -> Unit) {
    val name = stringResource(theme.labelRes())
    val size by animateDpAsState(if (selected) 52.dp else 44.dp, label = "SwatchSize")
    val ring by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
        label = "SwatchRing",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.size(width = 60.dp, height = 84.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
            Box(
                modifier = Modifier
                    .size(size)
                    .border(2.dp, ring, CircleShape)
                    .padding(Spacing.xs)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(theme.glows[0], theme.primaryLight, theme.glows[2])))
                    .semantics {
                        contentDescription = name
                        this.selected = selected
                    }
                    .clickable(role = Role.RadioButton, onClick = onClick),
            )
        }
        AutoResizeText(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun AppColorTheme.labelRes(): Int = when (this) {
    AppColorTheme.TEAL -> R.string.color_theme_teal
    AppColorTheme.OCEAN -> R.string.color_theme_ocean
    AppColorTheme.SUNSET -> R.string.color_theme_sunset
    AppColorTheme.LAVENDER -> R.string.color_theme_lavender
    AppColorTheme.FOREST -> R.string.color_theme_forest
}
