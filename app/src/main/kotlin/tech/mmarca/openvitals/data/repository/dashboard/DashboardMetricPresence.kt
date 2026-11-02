package tech.mmarca.openvitals.data.repository.dashboard

import android.content.Context
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * Which dashboard metrics the user's own sources actually record. NCHealth shows only
 * what the wearable writes through Google Health: a metric with no record in the last
 * [LookbackDays] days is hidden from the summary. The answer is kept between launches
 * and re-asked at most every [RefreshInterval], so the grid does not reshuffle on open.
 */
@Singleton
class DashboardMetricPresenceRepository @Inject constructor(
    @param:ApplicationContext context: Context,
    private val healthConnect: HealthConnectManager,
) {
    private val prefs = context.getSharedPreferences(PrefsName, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val _presentMetrics = MutableStateFlow(readCached())

    /** Null until the first probe has answered; then the metrics that have records. */
    val presentMetrics: StateFlow<Set<DashboardMetric>?> = _presentMetrics.asStateFlow()

    suspend fun refreshIfStale(now: Instant = Instant.now()) = mutex.withLock {
        val last = prefs.getLong(KeyCheckedAt, 0L)
        if (_presentMetrics.value != null && now.toEpochMilli() - last < RefreshInterval.toMillis()) {
            return@withLock
        }
        val start = now.minus(Duration.ofDays(LookbackDays))
        val present = healthConnect.recordPresenceReader.presentTypes(
            types = MetricRecordTypes.values.flatten().flatten().toSet(),
            start = start,
            end = now,
        )
        val metrics = metricsPresent(present)
        prefs.edit()
            .putStringSet(KeyMetrics, metrics.mapTo(mutableSetOf()) { it.name })
            .putLong(KeyCheckedAt, now.toEpochMilli())
            .apply()
        _presentMetrics.value = metrics
    }

    private fun readCached(): Set<DashboardMetric>? {
        if (!prefs.contains(KeyCheckedAt)) return null
        return prefs.getStringSet(KeyMetrics, emptySet()).orEmpty()
            .mapNotNullTo(mutableSetOf()) { name -> DashboardMetric.entries.firstOrNull { it.name == name } }
    }

    private companion object {
        const val PrefsName = "nchealth_metric_presence"
        const val KeyMetrics = "present_metrics"
        const val KeyCheckedAt = "checked_at"
        const val LookbackDays = 30L
        val RefreshInterval: Duration = Duration.ofHours(6)
    }
}

/**
 * The record types each metric needs: every group must have at least one type with
 * records (BMI needs a weight and a height). A metric missing here is never hidden.
 */
internal val MetricRecordTypes: Map<DashboardMetric, List<Set<KClass<out Record>>>> = mapOf(
    DashboardMetric.STEPS to listOf(setOf(StepsRecord::class)),
    DashboardMetric.DISTANCE to listOf(setOf(DistanceRecord::class)),
    DashboardMetric.CALORIES_OUT to
        listOf(setOf(TotalCaloriesBurnedRecord::class, ActiveCaloriesBurnedRecord::class)),
    DashboardMetric.ACTIVE_CALORIES to listOf(setOf(ActiveCaloriesBurnedRecord::class)),
    DashboardMetric.FLOORS to listOf(setOf(FloorsClimbedRecord::class)),
    DashboardMetric.ELEVATION to listOf(setOf(ElevationGainedRecord::class)),
    DashboardMetric.WHEELCHAIR_PUSHES to listOf(setOf(WheelchairPushesRecord::class)),
    DashboardMetric.WORKOUT to listOf(setOf(ExerciseSessionRecord::class)),
    DashboardMetric.SLEEP to listOf(setOf(SleepSessionRecord::class)),
    DashboardMetric.HYDRATION to listOf(setOf(HydrationRecord::class)),
    DashboardMetric.CALORIES_IN to listOf(setOf(NutritionRecord::class)),
    DashboardMetric.PROTEIN to listOf(setOf(NutritionRecord::class)),
    DashboardMetric.CARBS to listOf(setOf(NutritionRecord::class)),
    DashboardMetric.FAT to listOf(setOf(NutritionRecord::class)),
    DashboardMetric.CAFFEINE to listOf(setOf(NutritionRecord::class)),
    DashboardMetric.WEIGHT to listOf(setOf(WeightRecord::class)),
    DashboardMetric.HEIGHT to listOf(setOf(HeightRecord::class)),
    DashboardMetric.BMI to listOf(setOf(WeightRecord::class), setOf(HeightRecord::class)),
    DashboardMetric.FFMI to
        listOf(setOf(WeightRecord::class), setOf(HeightRecord::class), setOf(BodyFatRecord::class)),
    DashboardMetric.BODY_FAT to listOf(setOf(BodyFatRecord::class)),
    DashboardMetric.LEAN_MASS to listOf(setOf(LeanBodyMassRecord::class)),
    DashboardMetric.BMR to listOf(setOf(BasalMetabolicRateRecord::class)),
    DashboardMetric.BONE_MASS to listOf(setOf(BoneMassRecord::class)),
    DashboardMetric.BODY_WATER_MASS to listOf(setOf(BodyWaterMassRecord::class)),
    DashboardMetric.AVG_HEART_RATE to listOf(setOf(HeartRateRecord::class)),
    DashboardMetric.RESTING_HEART_RATE to listOf(setOf(RestingHeartRateRecord::class)),
    DashboardMetric.HRV to listOf(setOf(HeartRateVariabilityRmssdRecord::class)),
    DashboardMetric.BLOOD_PRESSURE to listOf(setOf(BloodPressureRecord::class)),
    DashboardMetric.SPO2 to listOf(setOf(OxygenSaturationRecord::class)),
    DashboardMetric.VO2_MAX to listOf(setOf(Vo2MaxRecord::class)),
    DashboardMetric.RESPIRATORY_RATE to listOf(setOf(RespiratoryRateRecord::class)),
    DashboardMetric.BODY_TEMPERATURE to listOf(setOf(BodyTemperatureRecord::class)),
    DashboardMetric.BLOOD_GLUCOSE to listOf(setOf(BloodGlucoseRecord::class)),
    DashboardMetric.SKIN_TEMPERATURE to listOf(setOf(SkinTemperatureRecord::class)),
    DashboardMetric.WEEKLY_CARDIO_LOAD to
        listOf(setOf(HeartRateRecord::class, ExerciseSessionRecord::class)),
    DashboardMetric.INTENSITY_MINUTES to
        listOf(setOf(HeartRateRecord::class, ExerciseSessionRecord::class)),
    DashboardMetric.MINDFULNESS to listOf(setOf(MindfulnessSessionRecord::class)),
    DashboardMetric.CYCLE to
        listOf(setOf(MenstruationPeriodRecord::class, MenstruationFlowRecord::class)),
)

/** The metrics whose every requirement group has a present type; unmapped metrics always count. */
internal fun metricsPresent(presentTypes: Set<KClass<out Record>>): Set<DashboardMetric> =
    DashboardMetric.entries.filterTo(mutableSetOf()) { metric ->
        val groups = MetricRecordTypes[metric] ?: return@filterTo true
        groups.all { group -> group.any { it in presentTypes } }
    }
