package tech.mmarca.openvitals.features.assistant

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.usecase.LoadDashboardDayUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** One scheduled or on-demand evaluation. */
data class AssistantReport(val createdAt: Instant, val slot: AssistantSlot?, val text: String)

/**
 * The assistant's memory on this phone: the recent evaluations and the open
 * conversation. Each request carries today's numbers from Health Connect.
 */
@Singleton
class AssistantRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val claude: ClaudeClient,
    private val loadDashboardDay: LoadDashboardDayUseCase,
    private val preferences: PreferencesRepository,
) {
    private val prefs = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val _reports = MutableStateFlow(readReports())
    private val _conversation = MutableStateFlow(readConversation())

    /** Newest first. */
    val reports: StateFlow<List<AssistantReport>> = _reports.asStateFlow()

    /** Follow-up questions and answers, oldest first. */
    val conversation: StateFlow<List<AssistantTurn>> = _conversation.asStateFlow()

    /** Reads today's data, asks Claude for an evaluation, and keeps it. */
    suspend fun evaluate(slot: AssistantSlot?): AssistantReport {
        val snapshot = snapshot()
        val prompt = buildString {
            appendLine(slotInstruction(slot))
            appendLine()
            appendLine("Today's data from the user's wearable (via Google Health / Health Connect):")
            append(snapshot)
        }
        val text = claude.complete(systemPrompt(), listOf(AssistantTurn(fromUser = true, text = prompt)))
        val report = AssistantReport(Instant.now(), slot, text)
        mutex.withLock {
            _reports.value = (listOf(report) + _reports.value).take(MaxReports)
            // A new evaluation starts a new conversation about it.
            _conversation.value = emptyList()
            save()
        }
        return report
    }

    /** A follow-up question, answered with the latest evaluation and today's data as context. */
    suspend fun ask(question: String): String {
        val context = buildString {
            appendLine("Today's data from the user's wearable:")
            appendLine(snapshot())
            _reports.value.firstOrNull()?.let { latest ->
                appendLine()
                appendLine("Your latest evaluation:")
                appendLine(latest.text)
            }
        }
        val history = _conversation.value
        val turns = listOf(AssistantTurn(true, context), AssistantTurn(false, ContextAcknowledgement)) +
            history + AssistantTurn(true, question)
        val answer = claude.complete(systemPrompt(), turns)
        mutex.withLock {
            _conversation.value = (history + AssistantTurn(true, question) + AssistantTurn(false, answer))
                .takeLast(MaxTurns)
            save()
        }
        return answer
    }

    suspend fun clearConversation() = mutex.withLock {
        _conversation.value = emptyList()
        save()
    }

    private suspend fun snapshot(): String {
        val data = loadDashboardDay(
            DashboardQuery(
                date = LocalDate.now(),
                sleepWindow = preferences.sleepWindowFlow.value,
                activityWeekMode = preferences.activityWeekModeFlow.value,
            ),
        )
        return data.toSnapshotText(LocalTime.now())
    }

    private fun languageName(): String {
        val tag = AppCompatDelegate.getApplicationLocales()[0]?.language ?: Locale.getDefault().language
        return if (tag == "vi") "Vietnamese" else "English"
    }

    private fun systemPrompt(): String = """
        You are the health coach inside NCHealth, a personal app that reads one person's wearable
        data (a Google Fitbit and Google Health). Write in ${languageName()}, warm and direct, like a
        good coach: the numbers first, then what to do. Judge each metric against the person's own
        baseline when one is given, not a population average. Base every claim on the data provided;
        if something is missing, say so briefly instead of guessing. You are not a doctor: do not
        diagnose, and suggest seeing a clinician only when a value is clearly out of range or keeps
        worsening. Keep it short enough to read on a phone in under a minute. Plain text with short
        headings and bullet points; no tables, no markdown links.
    """.trimIndent()

    private fun slotInstruction(slot: AssistantSlot?): String {
        val focus = when (slot) {
            AssistantSlot.MORNING ->
                "It is morning. Focus on last night's sleep and recovery, and plan today's effort."
            AssistantSlot.MIDDAY ->
                "It is early afternoon. Check the morning's activity and energy, and steer the afternoon."
            AssistantSlot.EVENING ->
                "It is late afternoon. Say whether now is a good time to train and how hard."
            AssistantSlot.NIGHT ->
                "It is night. Sum up the day and help the person wind down for good sleep."
            null -> "The person asked for an evaluation right now."
        }
        return """
            $focus
            Open with one short, warm greeting to the person that suits the time of day, then give the
            single most useful piece of health advice for right now in one or two sentences. After
            that, two scored lines: overall health right now out of 10, and readiness right now out of
            100, each with a one-line reason. End with 2 to 4 concrete recommendations for the next
            few hours.
        """.trimIndent()
    }

    private fun save() {
        val reports = buildJsonArray {
            _reports.value.forEach { report ->
                add(
                    buildJsonObject {
                        put("at", report.createdAt.toEpochMilli())
                        put("slot", report.slot?.name ?: "")
                        put("text", report.text)
                    },
                )
            }
        }
        val conversation = buildJsonArray {
            _conversation.value.forEach { turn ->
                add(buildJsonObject { put("user", turn.fromUser); put("text", turn.text) })
            }
        }
        prefs.edit()
            .putString(KeyReports, reports.toString())
            .putString(KeyConversation, conversation.toString())
            .apply()
    }

    private fun readReports(): List<AssistantReport> = parseArray(KeyReports).mapNotNull { element ->
        runCatching {
            val obj = element.jsonObject
            AssistantReport(
                createdAt = Instant.ofEpochMilli(obj.getValue("at").jsonPrimitive.long),
                slot = obj["slot"]?.jsonPrimitive?.content
                    ?.let { name -> AssistantSlot.entries.firstOrNull { it.name == name } },
                text = obj.getValue("text").jsonPrimitive.content,
            )
        }.getOrNull()
    }

    private fun readConversation(): List<AssistantTurn> = parseArray(KeyConversation).mapNotNull { element ->
        runCatching {
            val obj = element.jsonObject
            AssistantTurn(obj.getValue("user").jsonPrimitive.boolean, obj.getValue("text").jsonPrimitive.content)
        }.getOrNull()
    }

    private fun parseArray(key: String): JsonArray =
        prefs.getString(key, null)
            ?.let { runCatching { Json.parseToJsonElement(it).jsonArray }.getOrNull() }
            ?: JsonArray(emptyList())

    private companion object {
        const val PrefsName = "nchealth_assistant_history"
        const val KeyReports = "reports"
        const val KeyConversation = "conversation"
        const val MaxReports = 28
        const val MaxTurns = 20
        const val ContextAcknowledgement = "Understood. I have today's data and my latest evaluation."
    }
}
