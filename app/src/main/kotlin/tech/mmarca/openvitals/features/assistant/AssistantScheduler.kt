package tech.mmarca.openvitals.features.assistant

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import tech.mmarca.openvitals.MainActivity
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.navigation.EXTRA_OPENVITALS_ROUTE
import tech.mmarca.openvitals.navigation.Screen
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** The four daily check-ins. */
enum class AssistantSlot(val time: LocalTime) {
    MORNING(LocalTime.of(8, 0)),
    MIDDAY(LocalTime.of(14, 0)),
    EVENING(LocalTime.of(17, 30)),
    NIGHT(LocalTime.of(22, 0)),
    ;

    companion object {
        /** The next slot strictly after [now], and when it falls. */
        fun next(now: LocalDateTime): Pair<AssistantSlot, LocalDateTime> {
            entries.forEach { slot ->
                val at = now.toLocalDate().atTime(slot.time)
                if (at.isAfter(now)) return slot to at
            }
            return MORNING to now.toLocalDate().plusDays(1).atTime(MORNING.time)
        }
    }
}

/**
 * Keeps exactly one evaluation queued for the next slot. WorkManager may start it a few
 * minutes late to save battery; it survives restarts and reboots.
 */
@Singleton
class AssistantScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: AssistantSettings,
) {
    /** Queues the next slot unless one is already queued. Cancels when the assistant is off. */
    fun ensureScheduled() = schedule(ExistingWorkPolicy.KEEP)

    /** Re-queues from now, for a change of settings or after a run. */
    fun reschedule() = schedule(ExistingWorkPolicy.REPLACE)

    private fun schedule(policy: ExistingWorkPolicy) {
        val workManager = WorkManager.getInstance(context)
        val config = settings.config.value
        if (!config.enabled || !config.hasApiKey) {
            workManager.cancelUniqueWork(WorkName)
            return
        }
        val now = LocalDateTime.now()
        val (slot, at) = AssistantSlot.next(now)
        val request = OneTimeWorkRequestBuilder<AssistantReportWorker>()
            .setInitialDelay(Duration.between(now, at).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(KeySlot, slot.name).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork(WorkName, policy, request)
    }

    internal companion object {
        const val WorkName = "nchealth_assistant_report"
        const val KeySlot = "slot"
    }
}

/** Runs one check-in, posts it, and queues the next. */
class AssistantReportWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, AssistantWorkerEntryPoint::class.java)
        val settings = entryPoint.assistantSettings()
        val slot = inputData.getString(AssistantScheduler.KeySlot)
            ?.let { name -> AssistantSlot.entries.firstOrNull { it.name == name } }
        val config = settings.config.value
        if (!config.enabled || !config.hasApiKey) return Result.success()

        val outcome = runCatching { entryPoint.assistantRepository().evaluate(slot) }
        val failure = (outcome.exceptionOrNull() as? AssistantException)?.failure
        if (outcome.isFailure && failure in Retryable && runAttemptCount < MaxAttempts) return Result.retry()

        outcome.getOrNull()?.let { report ->
            if (config.notify) AssistantNotifications.post(applicationContext, report)
        }
        entryPoint.assistantScheduler().reschedule()
        return Result.success()
    }

    private companion object {
        const val MaxAttempts = 3
        val Retryable = setOf(AssistantFailure.OFFLINE, AssistantFailure.RATE_LIMITED, AssistantFailure.SERVER, null)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AssistantWorkerEntryPoint {
    fun assistantSettings(): AssistantSettings
    fun assistantRepository(): AssistantRepository
    fun assistantScheduler(): AssistantScheduler
}

internal object AssistantNotifications {
    private const val ChannelId = "nchealth_assistant"
    private const val NotificationId = 4108

    fun post(context: Context, report: AssistantReport) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                ChannelId,
                context.getString(R.string.assistant_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPENVITALS_ROUTE, Screen.Assistant.route)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = report.slot?.let { context.getString(it.titleRes()) }
            ?: context.getString(R.string.assistant_title)
        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_mindfulness_reminder)
            .setContentTitle(title)
            .setContentText(report.text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(report.text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NotificationId, notification)
    }
}

internal fun AssistantSlot.titleRes(): Int = when (this) {
    AssistantSlot.MORNING -> R.string.assistant_slot_morning
    AssistantSlot.MIDDAY -> R.string.assistant_slot_midday
    AssistantSlot.EVENING -> R.string.assistant_slot_evening
    AssistantSlot.NIGHT -> R.string.assistant_slot_night
}
