package tech.mmarca.openvitals.features.heart

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BloodPressureCategoryBounds
import tech.mmarca.openvitals.domain.insights.bloodPressureCategoryBounds
import tech.mmarca.openvitals.domain.preferences.BloodPressureGuideline
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.Spacing

@StringRes
internal fun bloodPressureGuidelineLabelRes(guideline: BloodPressureGuideline): Int = when (guideline) {
    BloodPressureGuideline.ACC_AHA_2017 -> R.string.settings_bp_guideline_acc_aha
    BloodPressureGuideline.ESH_2023 -> R.string.settings_bp_guideline_esh
    BloodPressureGuideline.ESC_2024 -> R.string.settings_bp_guideline_esc
    BloodPressureGuideline.ISH_2020 -> R.string.settings_bp_guideline_ish
}

/** How the category is chosen, then the links that back it. Both follow the guideline in Settings. */
@Composable
internal fun BloodPressureGuidelineExplanation(guideline: BloodPressureGuideline) {
    var showCategories by rememberSaveable { mutableStateOf(false) }
    SectionHeader(stringResource(R.string.interpretation_bp_explanation_title))
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = stringResource(
                    R.string.interpretation_bp_explanation_summary,
                    stringResource(bloodPressureGuidelineLabelRes(guideline)),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showCategories) {
                bloodPressureCategoryBounds(guideline).forEach { bounds ->
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = bloodPressureCategoryText(bounds.category),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = bloodPressureBoundsText(bounds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = stringResource(R.string.interpretation_bp_explanation_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.md))
            OpenVitalsOutlinedButton(onClick = { showCategories = !showCategories }) {
                Text(
                    stringResource(
                        if (showCategories) {
                            R.string.interpretation_bp_hide_categories
                        } else {
                            R.string.interpretation_bp_show_categories
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun bloodPressureBoundsText(bounds: BloodPressureCategoryBounds): String {
    val diastolic = bounds.diastolicMmHg
    return when {
        bounds.isLowest && diastolic != null ->
            stringResource(R.string.interpretation_bp_bounds_lowest, bounds.systolicMmHg, diastolic)
        diastolic != null ->
            stringResource(R.string.interpretation_bp_bounds_either, bounds.systolicMmHg, diastolic)
        else -> stringResource(R.string.interpretation_bp_bounds_systolic, bounds.systolicMmHg)
    }
}
