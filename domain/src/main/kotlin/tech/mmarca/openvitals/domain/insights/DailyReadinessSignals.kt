package tech.mmarca.openvitals.domain.insights

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardWeeklyIntensityMinutes

/**
 * One function per signal the day's readiness reads. Each returns what it adds, so the
 * score is a sum and no signal can see another's running total. Every threshold and
 * delta here: app heuristic, not validated.
 */
/**
 * What one signal adds to the day's verdict. Deltas, so the signals are independent and
 * their order is only the order the factors are listed in.
 */
internal data class ReadinessContribution(
    val score: Int = 0,
    val bodyEnergyScore: Int = 0,
    val trainingReadinessScore: Int = 0,
    val availableSignals: Int = 0,
    val baselineSignals: Int = 0,
    val elevatedBodySignals: Int = 0,
    val unusualVitals: Boolean = false,
    val missingReasons: List<String> = emptyList(),
    val factors: List<DailyReadinessFactor> = emptyList(),
    /** Body energy is measured, not estimated. It replaces what the deltas assembled. */
    val measuredBodyEnergy: Int? = null,
) {
    operator fun plus(other: ReadinessContribution) = ReadinessContribution(
        score = score + other.score,
        bodyEnergyScore = bodyEnergyScore + other.bodyEnergyScore,
        trainingReadinessScore = trainingReadinessScore + other.trainingReadinessScore,
        availableSignals = availableSignals + other.availableSignals,
        baselineSignals = baselineSignals + other.baselineSignals,
        elevatedBodySignals = elevatedBodySignals + other.elevatedBodySignals,
        unusualVitals = unusualVitals || other.unusualVitals,
        missingReasons = missingReasons + other.missingReasons,
        factors = factors + other.factors,
        measuredBodyEnergy = other.measuredBodyEnergy ?: measuredBodyEnergy,
    )
}

/** What a signal writes while it reads its own metric. Every field starts at no change. */
internal class ReadinessSignal {
    var score = 0
    var bodyEnergyScore = 0
    var trainingReadinessScore = 0
    var availableSignals = 0
    var baselineSignals = 0
    var elevatedBodySignals = 0
    var unusualVitals = false
    var measuredBodyEnergy: Int? = null
    val missingReasons = mutableListOf<String>()
    val factors = mutableListOf<DailyReadinessFactor>()

    fun addFactor(
        kind: ReadinessFactorKind,
        label: String,
        detail: String,
        impact: ReadinessFactorImpact,
        args: List<Int> = emptyList(),
    ) {
        factors += DailyReadinessFactor(kind, label, detail, impact, args)
    }

    fun contribution() = ReadinessContribution(
        score = score,
        bodyEnergyScore = bodyEnergyScore,
        trainingReadinessScore = trainingReadinessScore,
        availableSignals = availableSignals,
        baselineSignals = baselineSignals,
        elevatedBodySignals = elevatedBodySignals,
        unusualVitals = unusualVitals,
        missingReasons = missingReasons,
        factors = factors,
        measuredBodyEnergy = measuredBodyEnergy,
    )
}

internal fun readinessSignal(read: ReadinessSignal.() -> Unit): ReadinessContribution =
    ReadinessSignal().apply(read).contribution()

internal fun sleepContribution(data: DashboardData): ReadinessContribution = readinessSignal {
    if (DashboardMetric.SLEEP in data.loadedMetrics && data.sleepScore.confidence != SleepScoreConfidence.NO_DATA) {
        availableSignals += 1
        val sleepScore = data.sleepScore.score
        val sleepHours = data.sleepScore.sleepDurationMinutes / 60.0
        val sleepDetail = if (sleepHours > 0.0) {
            tr("Sleep scored $sleepScore/100 after ${formatHours(sleepHours)}.", "Điểm giấc ngủ $sleepScore/100 sau ${formatHours(sleepHours)} ngủ.")
        } else {
            tr("Sleep scored $sleepScore/100.", "Điểm giấc ngủ $sleepScore/100.")
        }
        when {
            sleepScore >= 82 -> {
                score += 12
                bodyEnergyScore += 16
                trainingReadinessScore += 8
                addFactor(
                    kind = ReadinessFactorKind.SLEEP_ABOVE_BASELINE,
                    label = tr("Sleep helped recovery", "Giấc ngủ giúp hồi phục"),
                    detail = sleepDetail,
                    impact = ReadinessFactorImpact.POSITIVE,
                )
            }
            sleepScore >= 65 -> {
                score += 5
                bodyEnergyScore += 7
                trainingReadinessScore += 3
                addFactor(
                    kind = ReadinessFactorKind.SLEEP_ABOVE_BASELINE,
                    label = tr("Sleep was usable", "Giấc ngủ tạm ổn"),
                    detail = sleepDetail,
                    impact = ReadinessFactorImpact.NEUTRAL,
                )
            }
            sleepScore >= 45 -> {
                score -= 8
                bodyEnergyScore -= 12
                trainingReadinessScore -= 6
                addFactor(
                    kind = ReadinessFactorKind.SLEEP_BELOW_BASELINE,
                    label = tr("Sleep may limit recovery", "Giấc ngủ có thể làm chậm hồi phục"),
                    detail = sleepDetail,
                    impact = ReadinessFactorImpact.NEGATIVE,
                )
            }
            else -> {
                score -= 20
                bodyEnergyScore -= 24
                trainingReadinessScore -= 14
                elevatedBodySignals += 1
                addFactor(
                    kind = ReadinessFactorKind.SLEEP_BELOW_BASELINE,
                    label = tr("Sleep was low", "Ngủ chưa đủ"),
                    detail = sleepDetail,
                    impact = ReadinessFactorImpact.WARNING,
                )
            }
        }
    } else {
        score -= 6
        missingReasons += "missing_sleep_data"
        addFactor(
            kind = ReadinessFactorKind.MISSING_SLEEP_DATA,
            label = tr("Sleep data missing", "Thiếu dữ liệu giấc ngủ"),
            detail = tr("Sleep data was not available, so today's recommendation is a rough guide.", "Không có dữ liệu giấc ngủ nên gợi ý hôm nay chỉ mang tính tham khảo."),
            impact = ReadinessFactorImpact.NEUTRAL,
        )
    }
}

internal fun hrvContribution(
    data: DashboardData,
    hrv: Double?,
    hrvBaseline: Double?,
    hrvStatus: HrvStatusInsight,
): ReadinessContribution = readinessSignal {
    if (DashboardMetric.HRV in data.loadedMetrics && hrv != null && hrv > 0.0) {
        availableSignals += 1
        if (hrvBaseline != null && hrvBaseline > 0.0) {
            baselineSignals += 1
            when (hrvStatus.status) {
                HrvStatus.UNUSUALLY_LOW -> {
                    score -= 17
                    bodyEnergyScore -= 11
                    trainingReadinessScore -= 19
                    elevatedBodySignals += 1
                    addFactor(
                        kind = ReadinessFactorKind.HRV_BELOW_BASELINE,
                        label = tr("HRV Status: ${hrvStatus.label}", "Trạng thái HRV: ${hrvStatus.label}"),
                        detail = hrvStatus.detail,
                        impact = ReadinessFactorImpact.WARNING,
                    )
                }
                HrvStatus.LOW -> {
                    score -= 8
                    bodyEnergyScore -= 5
                    trainingReadinessScore -= 10
                    addFactor(
                        kind = ReadinessFactorKind.HRV_BELOW_BASELINE,
                        label = tr("HRV Status: ${hrvStatus.label}", "Trạng thái HRV: ${hrvStatus.label}"),
                        detail = hrvStatus.detail,
                        impact = ReadinessFactorImpact.NEGATIVE,
                    )
                }
                HrvStatus.UNUSUALLY_HIGH -> {
                    score -= 4
                    bodyEnergyScore -= 2
                    trainingReadinessScore -= 3
                    addFactor(
                        kind = ReadinessFactorKind.HRV_ABOVE_BASELINE,
                        label = tr("HRV Status: ${hrvStatus.label}", "Trạng thái HRV: ${hrvStatus.label}"),
                        detail = hrvStatus.detail,
                        impact = ReadinessFactorImpact.NEGATIVE,
                    )
                }
                HrvStatus.HIGH -> {
                    score += 3
                    bodyEnergyScore += 2
                    trainingReadinessScore += 3
                    addFactor(
                        kind = ReadinessFactorKind.HRV_ABOVE_BASELINE,
                        label = tr("HRV Status: ${hrvStatus.label}", "Trạng thái HRV: ${hrvStatus.label}"),
                        detail = hrvStatus.detail,
                        impact = ReadinessFactorImpact.POSITIVE,
                    )
                }
                HrvStatus.BALANCED -> {
                    score += 5
                    bodyEnergyScore += 3
                    trainingReadinessScore += 6
                    addFactor(
                        kind = ReadinessFactorKind.HRV_NORMAL,
                        label = tr("HRV Status: ${hrvStatus.label}", "Trạng thái HRV: ${hrvStatus.label}"),
                        detail = hrvStatus.detail,
                        impact = ReadinessFactorImpact.POSITIVE,
                    )
                }
                HrvStatus.NEEDS_MORE_HRV -> Unit
            }
        } else {
            missingReasons += "new_user_not_enough_baseline"
            addFactor(
                kind = ReadinessFactorKind.NEW_USER_NOT_ENOUGH_BASELINE,
                label = tr("HRV baseline building", "Đang lập mức nền HRV"),
                detail = tr("HRV is recorded, but there is not enough history yet for a personal baseline.", "Đã có HRV nhưng chưa đủ lịch sử để lập mức nền riêng của bạn."),
                impact = ReadinessFactorImpact.NEUTRAL,
            )
        }
    } else {
        missingReasons += "missing_hrv_data"
        addFactor(
            kind = ReadinessFactorKind.MISSING_HRV_DATA,
            label = tr("HRV data missing", "Thiếu dữ liệu HRV"),
            detail = tr("HRV was not available, which lowers recommendation confidence.", "Không có HRV nên gợi ý kém chắc chắn hơn."),
            impact = ReadinessFactorImpact.NEUTRAL,
        )
    }
}

internal fun restingHeartRateContribution(data: DashboardData): ReadinessContribution = readinessSignal {
    if (DashboardMetric.RESTING_HEART_RATE in data.loadedMetrics && data.restingHeartRateBpm > 0) {
        availableSignals += 1
        val baseline = data.restingHeartRateBaselineBpm
        if (baseline != null && baseline > 0) {
            baselineSignals += 1
            val delta = data.restingHeartRateBpm - baseline
            val detail = when {
                delta > 0 -> tr("Resting heart rate is +$delta bpm compared with your usual baseline.", "Nhịp tim lúc nghỉ cao hơn mức nền thường ngày $delta bpm.")
                delta < 0 -> tr("Resting heart rate is ${abs(delta)} bpm below your usual baseline.", "Nhịp tim lúc nghỉ thấp hơn mức nền thường ngày ${abs(delta)} bpm.")
                else -> tr("Resting heart rate is near your usual baseline.", "Nhịp tim lúc nghỉ gần mức nền thường ngày.")
            }
            when {
                delta >= 8 -> {
                    score -= 16
                    bodyEnergyScore -= 12
                    trainingReadinessScore -= 12
                    elevatedBodySignals += 1
                    addFactor(
                        kind = ReadinessFactorKind.RESTING_HR_ELEVATED,
                        label = tr("Resting HR is elevated", "Nhịp tim nghỉ đang cao"),
                        detail = detail,
                        impact = ReadinessFactorImpact.WARNING,
                    )
                }
                delta >= 4 -> {
                    score -= 8
                    bodyEnergyScore -= 5
                    trainingReadinessScore -= 6
                    addFactor(
                        kind = ReadinessFactorKind.RESTING_HR_ELEVATED,
                        label = tr("Resting HR is slightly elevated", "Nhịp tim nghỉ hơi cao"),
                        detail = detail,
                        impact = ReadinessFactorImpact.NEGATIVE,
                    )
                }
                else -> {
                    score += 4
                    bodyEnergyScore += 3
                    trainingReadinessScore += 3
                    addFactor(
                        kind = ReadinessFactorKind.RESTING_HR_NORMAL,
                        label = tr("Resting HR looks normal", "Nhịp tim nghỉ bình thường"),
                        detail = detail,
                        impact = ReadinessFactorImpact.POSITIVE,
                    )
                }
            }
        } else {
            missingReasons += "new_user_not_enough_baseline"
            addFactor(
                kind = ReadinessFactorKind.NEW_USER_NOT_ENOUGH_BASELINE,
                label = tr("Resting HR baseline building", "Đang lập mức nền nhịp tim nghỉ"),
                detail = tr("Resting heart rate is available, but there is not enough history yet for a personal baseline.", "Đã có nhịp tim lúc nghỉ nhưng chưa đủ lịch sử để lập mức nền riêng."),
                impact = ReadinessFactorImpact.NEUTRAL,
            )
        }
    }
}

internal fun trainingLoadContribution(data: DashboardData): ReadinessContribution = readinessSignal {
    data.weeklyCardioLoad?.let { load ->
        availableSignals += 1
        val ratio = if (load.targetScore > 0) {
            load.currentScore / load.targetScore.toDouble()
        } else {
            null
        }
        when {
            ratio == null -> Unit
            ratio > 1.35 -> {
                score -= 12
                bodyEnergyScore -= 8
                trainingReadinessScore -= 13
                elevatedBodySignals += 1
                addFactor(
                    kind = ReadinessFactorKind.TRAINING_LOAD_HIGH,
                    label = tr("Training load is high", "Tải tập đang cao"),
                    detail = tr("This week is ${(ratio * 100.0).roundToInt()}% of your current load target.", "Tuần này đạt ${(ratio * 100.0).roundToInt()}% mục tiêu tải tập hiện tại."),
                    impact = ReadinessFactorImpact.WARNING,
                )
            }
            ratio in 0.75..1.20 -> {
                score += 4
                trainingReadinessScore += 5
                addFactor(
                    kind = ReadinessFactorKind.TRAINING_LOAD_NORMAL,
                    label = tr("Training load is stable", "Tải tập ổn định"),
                    detail = tr("This week is ${(ratio * 100.0).roundToInt()}% of your current load target.", "Tuần này đạt ${(ratio * 100.0).roundToInt()}% mục tiêu tải tập hiện tại."),
                    impact = ReadinessFactorImpact.POSITIVE,
                )
            }
            else -> {
                addFactor(
                    kind = ReadinessFactorKind.TRAINING_LOAD_NORMAL,
                    label = tr("Training load is light", "Tải tập đang nhẹ"),
                    detail = tr("This week is ${(ratio * 100.0).roundToInt()}% of your current load target.", "Tuần này đạt ${(ratio * 100.0).roundToInt()}% mục tiêu tải tập hiện tại."),
                    impact = ReadinessFactorImpact.NEUTRAL,
                )
            }
        }
    }
}

internal fun intensityMinutesContribution(
    data: DashboardData,
    intensityMinutes: IntensityMinutesReadinessInsight,
): ReadinessContribution = readinessSignal {
    if (DashboardMetric.INTENSITY_MINUTES in data.loadedMetrics) {
        if (data.weeklyIntensityMinutes != null &&
            data.weeklyIntensityMinutes.confidence != IntensityMinutesConfidence.NO_DATA
        ) {
            availableSignals += 1
            when (intensityMinutes.status) {
                IntensityMinutesStatus.GOAL_MET -> {
                    score += 3
                    trainingReadinessScore += 6
                    addFactor(
                        kind = ReadinessFactorKind.INTENSITY_MINUTES_ON_TARGET,
                        label = tr("Intensity minutes goal met", "Đã đạt mục tiêu phút cường độ"),
                        detail = intensityMinutes.detail,
                        impact = ReadinessFactorImpact.POSITIVE,
                    )
                }
                IntensityMinutesStatus.ON_TRACK -> {
                    score += 2
                    trainingReadinessScore += 4
                    addFactor(
                        kind = ReadinessFactorKind.INTENSITY_MINUTES_ON_TARGET,
                        label = tr("Intensity minutes on track", "Phút cường độ đúng tiến độ"),
                        detail = intensityMinutes.detail,
                        impact = ReadinessFactorImpact.POSITIVE,
                    )
                }
                IntensityMinutesStatus.BEHIND -> {
                    addFactor(
                        kind = ReadinessFactorKind.INTENSITY_MINUTES_BEHIND,
                        label = tr("Intensity minutes behind pace", "Phút cường độ chậm tiến độ"),
                        detail = intensityMinutes.detail,
                        impact = ReadinessFactorImpact.NEUTRAL,
                    )
                }
                IntensityMinutesStatus.LOW -> {
                    trainingReadinessScore -= 2
                    addFactor(
                        kind = ReadinessFactorKind.INTENSITY_MINUTES_BEHIND,
                        label = tr("Intensity minutes are low", "Phút cường độ còn thấp"),
                        detail = intensityMinutes.detail,
                        impact = ReadinessFactorImpact.NEUTRAL,
                    )
                }
                IntensityMinutesStatus.NEEDS_MORE_DATA -> Unit
            }
        } else {
            addFactor(
                kind = ReadinessFactorKind.MISSING_INTENSITY_MINUTES,
                label = tr("Intensity minutes need more data", "Phút cường độ cần thêm dữ liệu"),
                detail = intensityMinutes.detail,
                impact = ReadinessFactorImpact.NEUTRAL,
            )
        }
    }
}

internal fun physiologicalStressContribution(
    data: DashboardData,
    physiologicalStress: PhysiologicalStressEstimate,
): ReadinessContribution = readinessSignal {
    when (physiologicalStress.level) {
        PhysiologicalStressLevel.HIGH -> {
            addFactor(
                kind = ReadinessFactorKind.PHYSIOLOGICAL_STRESS_HIGH,
                label = tr("Physiological stress: ${physiologicalStress.label}", "Căng thẳng sinh lý: ${physiologicalStress.label}"),
                detail = physiologicalStress.summary,
                impact = ReadinessFactorImpact.WARNING,
            )
        }
        PhysiologicalStressLevel.MEDIUM -> {
            addFactor(
                kind = ReadinessFactorKind.PHYSIOLOGICAL_STRESS_HIGH,
                label = tr("Physiological stress: ${physiologicalStress.label}", "Căng thẳng sinh lý: ${physiologicalStress.label}"),
                detail = physiologicalStress.summary,
                impact = ReadinessFactorImpact.NEGATIVE,
            )
        }
        PhysiologicalStressLevel.RESTING,
        PhysiologicalStressLevel.LOW -> {
            addFactor(
                kind = ReadinessFactorKind.PHYSIOLOGICAL_STRESS_LOW,
                label = tr("Physiological stress: ${physiologicalStress.label}", "Căng thẳng sinh lý: ${physiologicalStress.label}"),
                detail = physiologicalStress.summary,
                impact = ReadinessFactorImpact.POSITIVE,
            )
        }
        PhysiologicalStressLevel.NEEDS_MORE_DATA -> {
            addFactor(
                kind = ReadinessFactorKind.MISSING_STRESS_DATA,
                label = tr("Physiological stress needs more data", "Căng thẳng sinh lý cần thêm dữ liệu"),
                detail = physiologicalStress.summary,
                impact = ReadinessFactorImpact.NEUTRAL,
            )
        }
    }
}

internal fun temperatureContribution(data: DashboardData): ReadinessContribution = readinessSignal {
    val bodyTemperature = data.latestBodyTemperatureCelsius
    val skinDelta = data.latestSkinTemperatureDeltaCelsius
    if (
        (DashboardMetric.BODY_TEMPERATURE in data.loadedMetrics && bodyTemperature != null) ||
        (DashboardMetric.SKIN_TEMPERATURE in data.loadedMetrics && skinDelta != null)
    ) {
        availableSignals += 1
        val tempWarning = (bodyTemperature != null && bodyTemperature >= 37.7) ||
            (skinDelta != null && skinDelta >= 1.0)
        val tempElevated = tempWarning ||
            (bodyTemperature != null && bodyTemperature >= 37.2) ||
            (skinDelta != null && skinDelta >= 0.5)
        if (tempElevated) {
            val detail = buildList {
                bodyTemperature?.let { add(tr("body temperature ${formatOneDecimal(it)} C", "thân nhiệt ${formatOneDecimal(it)} °C")) }
                skinDelta?.let { add(tr("skin temperature ${formatSignedOneDecimal(it)} C", "nhiệt độ da ${formatSignedOneDecimal(it)} °C")) }
            }.joinToString(separator = ", ")
            score -= if (tempWarning) 20 else 10
            bodyEnergyScore -= if (tempWarning) 18 else 8
            trainingReadinessScore -= if (tempWarning) 18 else 9
            elevatedBodySignals += 1
            unusualVitals = tempWarning
            addFactor(
                kind = ReadinessFactorKind.TEMPERATURE_ELEVATED,
                label = tr("Temperature signal elevated", "Nhiệt độ cơ thể tăng"),
                detail = tr("Some temperature signals look elevated ($detail). If you feel unwell, consider resting.", "Một số chỉ số nhiệt độ đang tăng ($detail). Nếu thấy không khỏe, hãy nghỉ ngơi."),
                impact = if (tempWarning) ReadinessFactorImpact.WARNING else ReadinessFactorImpact.NEGATIVE,
            )
        }
    }
}

internal fun hydrationContribution(
    data: DashboardData,
    goals: DailyReadinessGoalInputs,
): ReadinessContribution = readinessSignal {
    if (DashboardMetric.HYDRATION in data.loadedMetrics && goals.hydrationLitersGoal > 0.0) {
        availableSignals += 1
        val hydrationRatio = data.hydrationLiters / goals.hydrationLitersGoal
        if (hydrationRatio < 0.35) {
            score -= 4
            bodyEnergyScore -= 4
            addFactor(
                kind = ReadinessFactorKind.HYDRATION_LOW,
                label = tr("Hydration is behind", "Uống nước chưa đủ"),
                detail = tr("Hydration is ${(hydrationRatio * 100.0).roundToInt()}% of today's goal.", "Mới uống ${(hydrationRatio * 100.0).roundToInt()}% mục tiêu nước hôm nay."),
                impact = ReadinessFactorImpact.NEGATIVE,
            )
        }
    }
}

internal fun nutritionContribution(data: DashboardData): ReadinessContribution = readinessSignal {
    if (data.hasLoggedNutrition()) {
        availableSignals += 1
        bodyEnergyScore += 2
        addFactor(
            kind = ReadinessFactorKind.NUTRITION_LOGGED,
            label = tr("Nutrition is logged", "Đã ghi dinh dưỡng"),
            detail = tr("Meal data is available for today's energy context.", "Có dữ liệu bữa ăn để đánh giá năng lượng hôm nay."),
            impact = ReadinessFactorImpact.POSITIVE,
        )
    }
}

internal fun mindfulnessContribution(data: DashboardData): ReadinessContribution = readinessSignal {
    if ((data.mindfulnessMinutes ?: 0) >= 5) {
        availableSignals += 1
        score += 2
        bodyEnergyScore += 3
        addFactor(
            kind = ReadinessFactorKind.STRESS_LOW,
            label = tr("Recovery moment recorded", "Đã ghi thời gian thư giãn"),
            detail = tr("${data.mindfulnessMinutes} min of mindfulness is logged today.", "Hôm nay đã ghi ${data.mindfulnessMinutes} phút chánh niệm."),
            impact = ReadinessFactorImpact.POSITIVE,
        )
    }
}

internal fun bodyEnergyContribution(data: DashboardData): ReadinessContribution = readinessSignal {
    val bodyEnergy = data.bodyEnergyTimeline
    if (bodyEnergy != null) {
        availableSignals += 1
        measuredBodyEnergy = bodyEnergy.currentScore
        val detail = tr("Body energy is at ${bodyEnergy.currentScore} after starting the day ", "Năng lượng cơ thể còn ${bodyEnergy.currentScore}, đầu ngày ") +
            tr("at ${bodyEnergy.startScore}.", "ở mức ${bodyEnergy.startScore}.")
        if (bodyEnergy.currentScore <= 25) {
            // Strong enough to pull a perfect day out of "ready".
            score -= 20
            trainingReadinessScore -= 20
            elevatedBodySignals += 1
            addFactor(
                kind = ReadinessFactorKind.BODY_ENERGY_DRAINED,
                label = tr("Body energy is drained", "Năng lượng cơ thể cạn kiệt"),
                detail = detail,
                impact = ReadinessFactorImpact.WARNING,
                args = listOf(bodyEnergy.currentScore, bodyEnergy.startScore),
            )
        } else if (bodyEnergy.currentScore <= 45 || bodyEnergy.startScore <= 30) {
            score -= 8
            trainingReadinessScore -= 9
            addFactor(
                kind = ReadinessFactorKind.BODY_ENERGY_LOW,
                label = tr("Body energy is low", "Năng lượng cơ thể thấp"),
                detail = detail,
                impact = ReadinessFactorImpact.NEGATIVE,
                args = listOf(bodyEnergy.currentScore, bodyEnergy.startScore),
            )
        } else if (bodyEnergy.currentScore >= 80) {
            score += 6
            trainingReadinessScore += 5
            addFactor(
                kind = ReadinessFactorKind.BODY_ENERGY_CHARGED,
                label = tr("Body energy is charged", "Năng lượng cơ thể dồi dào"),
                detail = detail,
                impact = ReadinessFactorImpact.POSITIVE,
                args = listOf(bodyEnergy.currentScore, bodyEnergy.startScore),
            )
        }
    }
}
