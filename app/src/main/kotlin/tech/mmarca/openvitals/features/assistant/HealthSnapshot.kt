package tech.mmarca.openvitals.features.assistant

import tech.mmarca.openvitals.domain.model.DashboardData
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/**
 * What the assistant is told: the day's numbers as plain lines, only the ones the
 * wearable recorded. Units are spelled out so nothing is left to guess.
 */
internal fun DashboardData.toSnapshotText(now: LocalTime, zone: ZoneId = ZoneId.systemDefault()): String =
    buildList {
        add("Date: $date, local time ${now.withSecond(0).withNano(0)}")
        if (steps > 0) add("Steps today: $steps")
        if (distanceMeters > 0) add("Distance today: ${fmt(distanceMeters / 1000.0)} km")
        activeCaloriesKcal?.takeIf { it > 0 }?.let { add("Active calories today: ${it.toInt()} kcal") }
        if (caloriesKcal > 0) add("Total calories burned today: ${caloriesKcal.toInt()} kcal")
        sleep?.let { night ->
            val start = night.startTime.atZone(zone).toLocalTime().withSecond(0).withNano(0)
            val end = night.endTime.atZone(zone).toLocalTime().withSecond(0).withNano(0)
            add("Last sleep: ${fmt(night.durationHours)} h, from $start to $end")
            stageMinutes(night.stages.map { it.stageType to it.durationMs })
                .takeIf { it.isNotEmpty() }
                ?.let { add("Sleep stages (minutes): $it") }
        }
        if (sleepScore.score > 0) add("Sleep score: ${sleepScore.score}/100")
        if (restingHeartRateBpm > 0) {
            val baseline = restingHeartRateBaselineBpm?.let { " (personal baseline $it bpm)" }.orEmpty()
            add("Resting heart rate: $restingHeartRateBpm bpm$baseline")
        }
        if (avgHeartRateBpm > 0) add("Average heart rate today: $avgHeartRateBpm bpm")
        hrvRmssdMs?.let { hrv ->
            val baseline = hrvBaselineRmssdMs?.let { " (personal baseline ${fmt(it)} ms)" }.orEmpty()
            add("Overnight HRV (RMSSD): ${fmt(hrv)} ms$baseline")
        }
        latestSpO2Percent?.let { add("Blood oxygen (SpO2): ${fmt(it)} %") }
        avgRespiratoryRate?.let { add("Respiratory rate: ${fmt(it)} breaths/min") }
        latestSkinTemperatureDeltaCelsius?.let { add("Skin temperature vs baseline: ${fmt(it)} °C") }
        latestVo2Max?.let { add("VO2 max: ${fmt(it)} ml/kg/min") }
        bodyEnergyTimeline?.let { add("Body energy now: ${it.currentScore}/100 (started the day at ${it.startScore})") }
        weeklyCardioLoad?.let { add("Weekly cardio load: ${it.currentScore} of target ${it.targetScore}") }
        weeklyIntensityMinutes?.let { add("Weekly intensity minutes: $it") }
        workouts.ifEmpty { listOfNotNull(workout) }.forEach { session ->
            val minutes = Duration.ofMillis(session.durationMs).toMinutes()
            val title = session.title?.takeIf { it.isNotBlank() } ?: "exercise type ${session.exerciseType}"
            add("Workout: $title, $minutes min")
        }
        weightKg?.let { add("Weight: ${fmt(it)} kg") }
        if (hydrationLiters > 0) add("Water today: ${fmt(hydrationLiters)} L")
    }.joinToString("\n")

private fun stageMinutes(stages: List<Pair<Int, Long>>): String =
    stages.groupBy({ it.first }, { it.second })
        .mapNotNull { (type, durations) ->
            StageNames[type]?.let { name -> "$name ${Duration.ofMillis(durations.sum()).toMinutes()}" }
        }
        .joinToString(", ")

/** Health Connect's sleep stage codes. */
private val StageNames = mapOf(1 to "awake", 2 to "asleep", 4 to "light", 5 to "deep", 6 to "REM", 7 to "awake in bed")

private fun fmt(value: Double): String = String.format(Locale.US, "%.1f", value)
