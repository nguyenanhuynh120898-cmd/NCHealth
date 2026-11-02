package tech.mmarca.openvitals.features.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.ShimmerLine
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var question by rememberSaveable { mutableStateOf("") }
    val configured = state.config.enabled && state.config.hasApiKey

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = LayoutMetrics.screenGutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                AssistantHeader(
                    configured = configured,
                    isEvaluating = state.isEvaluating,
                    onEvaluate = viewModel::evaluateNow,
                    onToggleSettings = viewModel::toggleSettings,
                )
            }
            item(key = "settings") {
                AnimatedVisibility(
                    visible = state.showSettings || !configured,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    AssistantSettingsCard(state.config, viewModel)
                }
            }
            state.failure?.let { failure ->
                item(key = "failure") { FailureCard(failure) }
            }
            if (state.isEvaluating) {
                item(key = "evaluating") { LoadingCard() }
            }
            items(state.reports.take(1), key = { "latest-${it.createdAt}" }) { report ->
                ReportCard(report, emphasized = true, modifier = Modifier.animateItem())
            }
            items(state.conversation, key = { "turn-${state.conversation.indexOf(it)}-${it.fromUser}" }) { turn ->
                ChatBubble(turn, modifier = Modifier.animateItem())
            }
            if (state.isAnswering) {
                item(key = "answering") { LoadingCard() }
            }
            if (state.reports.size > 1) {
                item(key = "history-title") {
                    Text(
                        text = stringResource(R.string.assistant_history_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(state.reports.drop(1), key = { "report-${it.createdAt}" }) { report ->
                    ReportCard(report, emphasized = false, modifier = Modifier.animateItem())
                }
            }
        }
        if (configured && state.reports.isNotEmpty()) {
            AskBar(
                question = question,
                onQuestionChange = { question = it },
                enabled = !state.isAnswering,
                onSend = {
                    viewModel.ask(question)
                    question = ""
                },
            )
        }
    }
}

@Composable
private fun AssistantHeader(
    configured: Boolean,
    isEvaluating: Boolean,
    onEvaluate: () -> Unit,
    onToggleSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_assistant_spark),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.assistant_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleSettings) {
                Icon(Icons.Outlined.Tune, contentDescription = stringResource(R.string.assistant_settings_title))
            }
        }
        if (configured) {
            Button(onClick = onEvaluate, enabled = !isEvaluating, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.assistant_evaluate_now))
            }
        }
    }
}

@Composable
private fun AssistantSettingsCard(config: AssistantConfig, viewModel: AssistantViewModel) {
    var key by rememberSaveable { mutableStateOf("") }
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.assistant_settings_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.assistant_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = {
                    Text(
                        stringResource(
                            if (config.hasApiKey) R.string.assistant_api_key_replace else R.string.assistant_api_key,
                        ),
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        viewModel.saveApiKey(key)
                        key = ""
                    },
                    enabled = key.isNotBlank(),
                ) { Text(stringResource(R.string.assistant_api_key_save)) }
                if (config.hasApiKey) {
                    TextButton(onClick = { viewModel.saveApiKey("") }) {
                        Text(stringResource(R.string.assistant_api_key_remove))
                    }
                }
            }
            Text(stringResource(R.string.assistant_model_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AssistantModel.entries.forEachIndexed { index, model ->
                    SegmentedButton(
                        // The fill colour marks the choice; no check mark.
                        icon = {},
                        selected = config.model == model,
                        onClick = { viewModel.setModel(model) },
                        shape = SegmentedButtonDefaults.itemShape(index, AssistantModel.entries.size),
                    ) {
                        Text(stringResource(if (model == AssistantModel.SONNET) R.string.assistant_model_sonnet else R.string.assistant_model_haiku))
                    }
                }
            }
            SwitchRow(stringResource(R.string.assistant_schedule_toggle), config.enabled, config.hasApiKey, viewModel::setEnabled)
            SwitchRow(stringResource(R.string.assistant_notify_toggle), config.notify, config.enabled, viewModel::setNotify)
            Text(
                text = stringResource(R.string.assistant_schedule_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun ReportCard(report: AssistantReport, emphasized: Boolean, modifier: Modifier = Modifier) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT) }
    val time = remember(report.createdAt) { report.createdAt.atZone(ZoneId.systemDefault()).format(formatter) }
    OpenVitalsCard(modifier = modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = listOfNotNull(report.slot?.let { stringResource(it.titleRes()) }, time).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = report.text,
                style = if (emphasized) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                maxLines = if (emphasized) Int.MAX_VALUE else 6,
            )
        }
    }
}

@Composable
private fun ChatBubble(turn: AssistantTurn, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = if (turn.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            text = turn.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (turn.fromUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    color = if (turn.fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun LoadingCard() {
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShimmerLine(width = 140.dp)
            ShimmerLine(width = 260.dp)
            ShimmerLine(width = 220.dp)
            ShimmerLine(width = 180.dp)
        }
    }
}

@Composable
private fun FailureCard(failure: AssistantFailure) {
    val message = when (failure) {
        AssistantFailure.NO_KEY -> R.string.assistant_error_no_key
        AssistantFailure.BAD_KEY -> R.string.assistant_error_bad_key
        AssistantFailure.RATE_LIMITED -> R.string.assistant_error_rate_limited
        AssistantFailure.OFFLINE -> R.string.assistant_error_offline
        AssistantFailure.DECLINED -> R.string.assistant_error_declined
        AssistantFailure.SERVER -> R.string.assistant_error_server
    }
    OpenVitalsCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.errorContainer) {
        Text(
            text = stringResource(message),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun AskBar(question: String, onQuestionChange: (String) -> Unit, enabled: Boolean, onSend: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LayoutMetrics.screenGutter, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = question,
            onValueChange = onQuestionChange,
            placeholder = { Text(stringResource(R.string.assistant_ask_hint)) },
            shape = MaterialTheme.shapes.large,
            maxLines = 4,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onSend, enabled = enabled && question.isNotBlank()) {
            if (enabled) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = stringResource(R.string.assistant_ask_send))
            } else {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    }
}
