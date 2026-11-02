package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.LayoutMetrics

/** Calories and the drinking goal. Food and caffeine intake are not tracked: no wearable measures them. */
@Composable
fun NutritionSettingsScreen(viewModel: NutritionSettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsSectionList {
        item { SectionHeader(stringResource(SettingsSection.NUTRITION.titleRes)) }
        item {
            CalorieDataSourceCard(
                enabled = state.showOpenVitalsCalculatedCalories,
                onEnabledChange = viewModel::setShowOpenVitalsCalculatedCalories,
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            )
        }
        item { SettingsCardSpacer() }
        item {
            HydrationGoalCard(
                goalLiters = state.hydrationDailyGoalLiters,
                unitSystem = state.hydrationUnitSystem,
                onGoalChange = viewModel::setHydrationDailyGoalLiters,
                modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            )
        }
    }
}
