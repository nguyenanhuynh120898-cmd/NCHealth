package tech.mmarca.openvitals.features.assistant

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** One turn of a conversation with the assistant. */
data class AssistantTurn(val fromUser: Boolean, val text: String)

/** Why a request to Claude did not produce an answer, in terms the screen can explain. */
enum class AssistantFailure { NO_KEY, BAD_KEY, RATE_LIMITED, OFFLINE, DECLINED, SERVER }

class AssistantException(val failure: AssistantFailure, cause: Throwable? = null) : Exception(failure.name, cause)

/**
 * Talks to the Claude API with the user's own key. Thinking stays adaptive and effort
 * medium: a health summary needs care, not a long deliberation.
 */
@Singleton
class ClaudeClient @Inject constructor(
    private val settings: AssistantSettings,
) {
    @Volatile private var cached: Pair<String, AnthropicClient>? = null

    suspend fun complete(system: String, turns: List<AssistantTurn>): String = withContext(Dispatchers.IO) {
        val apiKey = settings.apiKey() ?: throw AssistantException(AssistantFailure.NO_KEY)
        val model = settings.config.value.model
        val params = MessageCreateParams.builder()
            .model(model.id)
            .maxTokens(MaxTokens)
            .system(system)
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.MEDIUM).build())
            .apply {
                turns.forEach { turn ->
                    if (turn.fromUser) addUserMessage(turn.text) else addAssistantMessage(turn.text)
                }
                if (model == AssistantModel.SONNET) {
                    // If a safety classifier declines, the API retries on a fallback model by itself.
                    putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                    putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                }
            }
            .build()
        val response = try {
            client(apiKey).messages().create(params)
        } catch (e: UnauthorizedException) {
            throw AssistantException(AssistantFailure.BAD_KEY, e)
        } catch (e: PermissionDeniedException) {
            throw AssistantException(AssistantFailure.BAD_KEY, e)
        } catch (e: RateLimitException) {
            throw AssistantException(AssistantFailure.RATE_LIMITED, e)
        } catch (e: AnthropicServiceException) {
            throw AssistantException(AssistantFailure.SERVER, e)
        } catch (e: AnthropicIoException) {
            throw AssistantException(AssistantFailure.OFFLINE, e)
        }
        if (response.stopReason().orElse(null) == StopReason.REFUSAL) {
            throw AssistantException(AssistantFailure.DECLINED)
        }
        response.content()
            .flatMap { block -> block.text().map { listOf(it.text()) }.orElse(emptyList()) }
            .joinToString("\n")
            .trim()
            .ifEmpty { throw AssistantException(AssistantFailure.SERVER) }
    }

    private fun client(apiKey: String): AnthropicClient {
        cached?.takeIf { it.first == apiKey }?.let { return it.second }
        val created = AnthropicOkHttpClient.builder().apiKey(apiKey).build()
        cached = apiKey to created
        return created
    }

    private companion object {
        const val MaxTokens = 8000L
    }
}
