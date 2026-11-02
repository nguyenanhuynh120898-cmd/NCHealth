package tech.mmarca.openvitals.features.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssistantUiState(
    val config: AssistantConfig = AssistantConfig(),
    val reports: List<AssistantReport> = emptyList(),
    val conversation: List<AssistantTurn> = emptyList(),
    val isEvaluating: Boolean = false,
    val isAnswering: Boolean = false,
    val failure: AssistantFailure? = null,
    val showSettings: Boolean = false,
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val settings: AssistantSettings,
    private val repository: AssistantRepository,
    private val scheduler: AssistantScheduler,
) : ViewModel() {

    private val local = MutableStateFlow(
        AssistantUiState(showSettings = !settings.config.value.let { it.enabled && it.hasApiKey }),
    )

    val uiState: StateFlow<AssistantUiState> = combine(
        local,
        settings.config,
        repository.reports,
        repository.conversation,
    ) { state, config, reports, conversation ->
        state.copy(config = config, reports = reports, conversation = conversation)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    fun toggleSettings() = local.update { it.copy(showSettings = !it.showSettings) }

    fun saveApiKey(key: String) {
        settings.setApiKey(key)
        if (key.isNotBlank()) settings.setEnabled(true)
        scheduler.reschedule()
        local.update { it.copy(failure = null) }
    }

    fun setEnabled(enabled: Boolean) {
        settings.setEnabled(enabled)
        scheduler.reschedule()
    }

    fun setModel(model: AssistantModel) = settings.setModel(model)

    fun setNotify(notify: Boolean) = settings.setNotify(notify)

    fun evaluateNow() {
        if (local.value.isEvaluating) return
        local.update { it.copy(isEvaluating = true, failure = null) }
        viewModelScope.launch {
            val failure = runCatching { repository.evaluate(slot = null) }.exceptionOrNull()?.toFailure()
            local.update { it.copy(isEvaluating = false, failure = failure, showSettings = it.showSettings && failure != null) }
        }
    }

    fun ask(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty() || local.value.isAnswering) return
        local.update { it.copy(isAnswering = true, failure = null) }
        viewModelScope.launch {
            val failure = runCatching { repository.ask(trimmed) }.exceptionOrNull()?.toFailure()
            local.update { it.copy(isAnswering = false, failure = failure) }
        }
    }

    fun clearConversation() {
        viewModelScope.launch { repository.clearConversation() }
    }

    private fun Throwable.toFailure(): AssistantFailure =
        (this as? AssistantException)?.failure ?: AssistantFailure.SERVER
}
