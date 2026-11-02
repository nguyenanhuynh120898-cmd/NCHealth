package tech.mmarca.openvitals.features.assistant

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.navigation.timeOfDayGreeting
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.ShimmerLine
import tech.mmarca.openvitals.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The top of the summary: Claude's latest check-in, which opens with its own greeting, or
 * Claude's invitation to set up or run the first one.
 */
@Composable
fun AssistantBriefCard(
    onOpenAssistant: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AssistantViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val configured = state.config.enabled && state.config.hasApiKey
    val latest = state.reports.firstOrNull()
    var expanded by rememberSaveable { mutableStateOf(false) }

    OpenVitalsCard(modifier = modifier.fillMaxWidth().animateContentSize(), onClick = onOpenAssistant) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            BriefHeader(latest)
            when {
                state.isEvaluating -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    ShimmerLine(width = 220.dp)
                    ShimmerLine(width = 260.dp)
                    ShimmerLine(width = 180.dp)
                }
                latest != null -> {
                    Text(
                        text = latest.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (expanded) Int.MAX_VALUE else CollapsedLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(stringResource(if (expanded) R.string.assistant_brief_less else R.string.assistant_brief_more))
                        }
                        Spacer(Modifier.weight(1f))
                        FilledTonalButton(onClick = onOpenAssistant) { Text(stringResource(R.string.assistant_brief_ask)) }
                    }
                }
                configured -> {
                    Text(
                        text = "${timeOfDayGreeting()}! " + stringResource(R.string.assistant_brief_first),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FilledTonalButton(onClick = viewModel::evaluateNow) {
                        Text(stringResource(R.string.assistant_evaluate_now))
                    }
                }
                else -> {
                    Text(
                        text = "${timeOfDayGreeting()}! " + stringResource(R.string.assistant_brief_setup_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FilledTonalButton(onClick = onOpenAssistant) {
                        Text(stringResource(R.string.assistant_brief_setup_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefHeader(latest: AssistantReport?) {
    val formatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_assistant_spark),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = "Claude",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        latest?.let { report ->
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = listOfNotNull(
                    report.slot?.let { stringResource(it.titleRes()) },
                    report.createdAt.atZone(ZoneId.systemDefault()).format(formatter),
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val CollapsedLines = 7
