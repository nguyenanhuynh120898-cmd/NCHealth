package tech.mmarca.openvitals.domain.insights

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardWeeklyIntensityMinutes

enum class ReadinessState {
    READY,
    MODERATE,
    RECOVER,
    REST,
    UNKNOWN,
}

enum class ReadinessRecommendationType {
    HARD_TRAINING,
    MODERATE_TRAINING,
    LIGHT_ACTIVITY,
    MOBILITY,
    REST,
    CHECK_SYMPTOMS,
}

enum class ReadinessConfidence {
    HIGH,
    MEDIUM,
    LOW,
}

enum class ReadinessFactorImpact {
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
    WARNING,
}

enum class ReadinessFactorKind {
    SLEEP_BELOW_BASELINE,
    SLEEP_ABOVE_BASELINE,
    RESTING_HR_ELEVATED,
    RESTING_HR_NORMAL,
    HRV_BELOW_BASELINE,
    HRV_ABOVE_BASELINE,
    HRV_NORMAL,
    STRESS_HIGH,
    STRESS_LOW,
    TRAINING_LOAD_HIGH,
    TRAINING_LOAD_NORMAL,
    INTENSITY_MINUTES_ON_TARGET,
    INTENSITY_MINUTES_BEHIND,
    MISSING_INTENSITY_MINUTES,
    PHYSIOLOGICAL_STRESS_HIGH,
    PHYSIOLOGICAL_STRESS_LOW,
    MISSING_STRESS_DATA,
    TEMPERATURE_ELEVATED,
    HYDRATION_LOW,
    BODY_ENERGY_DRAINED,
    BODY_ENERGY_LOW,
    BODY_ENERGY_CHARGED,
    NUTRITION_LOGGED,
    MISSING_SLEEP_DATA,
    MISSING_HRV_DATA,
    NEW_USER_NOT_ENOUGH_BASELINE,
}

enum class HrvStatus {
    BALANCED,
    LOW,
    HIGH,
    UNUSUALLY_LOW,
    UNUSUALLY_HIGH,
    NEEDS_MORE_HRV,
}

data class HrvStatusInsight(
    val status: HrvStatus,
    val label: String,
    val detail: String,
    val currentRmssdMs: Double?,
    val baselineRmssdMs: Double?,
    val percentFromBaseline: Int?,
)

enum class IntensityMinutesStatus {
    GOAL_MET,
    ON_TRACK,
    BEHIND,
    LOW,
    NEEDS_MORE_DATA,
}

data class IntensityMinutesReadinessInsight(
    val status: IntensityMinutesStatus,
    val label: String,
    val detail: String,
    val moderateEquivalentMinutes: Int?,
    val targetMinutes: Int,
    val todayModerateEquivalentMinutes: Int?,
    val progressPercent: Int,
    val confidence: IntensityMinutesConfidence,
)

data class DailyReadinessGoalInputs(
    val stepsGoal: Double = 8_000.0,
    val hydrationLitersGoal: Double = 2.0,
    val activeMinutesGoal: Double = 45.0,
)

data class DailyReadinessFactor(
    val kind: ReadinessFactorKind,
    val label: String,
    val detail: String,
    val impact: ReadinessFactorImpact,
    /** The numbers [detail]'s sentence embeds. Only the body-energy factors carry them. */
    val args: List<Int> = emptyList(),
)

data class DailyReadinessInsight(
    val state: ReadinessState,
    val score: Int,
    val bodyEnergyScore: Int,
    val trainingReadinessScore: Int,
    val recommendationType: ReadinessRecommendationType,
    val statusTitle: String,
    val recommendation: String,
    val explanation: String,
    val alternative: String,
    val suggestedWorkout: String,
    val avoid: String,
    val strainTarget: String,
    val currentStrain: String?,
    val adaptiveGoal: String,
    val confidence: ReadinessConfidence,
    val confidenceReason: String,
    val hrvStatus: HrvStatusInsight,
    val intensityMinutes: IntensityMinutesReadinessInsight,
    val physiologicalStress: PhysiologicalStressEstimate,
    val factors: List<DailyReadinessFactor>,
    val recoveryModeSuggested: Boolean,
)

/**
 * HRV against its 28-day median. HRV norms differ per person, so the
 * comparison is with the person's own baseline. Bands ±15 % and ±30 %: app
 * heuristic, no published source.
 */
fun calculateHrvStatus(
    hrvRmssdMs: Double?,
    baselineRmssdMs: Double?,
    hasHrvData: Boolean,
): HrvStatusInsight {
    if (!hasHrvData || hrvRmssdMs == null || hrvRmssdMs <= 0.0) {
        return HrvStatusInsight(
            status = HrvStatus.NEEDS_MORE_HRV,
            label = tr("Needs more HRV", "Cần thêm HRV"),
            detail = tr("HRV was not available for this day.", "Không có HRV cho ngày này."),
            currentRmssdMs = hrvRmssdMs,
            baselineRmssdMs = baselineRmssdMs,
            percentFromBaseline = null,
        )
    }
    if (baselineRmssdMs == null || baselineRmssdMs <= 0.0) {
        return HrvStatusInsight(
            status = HrvStatus.NEEDS_MORE_HRV,
            label = tr("Needs more HRV", "Cần thêm HRV"),
            detail = tr("HRV is recorded, but there is not enough history yet for a personal baseline.", "Đã có HRV nhưng chưa đủ lịch sử để lập mức nền riêng của bạn."),
            currentRmssdMs = hrvRmssdMs,
            baselineRmssdMs = baselineRmssdMs,
            percentFromBaseline = null,
        )
    }

    val percent = ((hrvRmssdMs - baselineRmssdMs) / baselineRmssdMs * 100.0).roundToInt()
    val status = when {
        percent <= -30 -> HrvStatus.UNUSUALLY_LOW
        percent <= -15 -> HrvStatus.LOW
        percent >= 30 -> HrvStatus.UNUSUALLY_HIGH
        percent >= 15 -> HrvStatus.HIGH
        else -> HrvStatus.BALANCED
    }
    val label = when (status) {
        HrvStatus.BALANCED -> tr("Balanced", "Cân bằng")
        HrvStatus.LOW -> tr("Low", "Thấp")
        HrvStatus.HIGH -> tr("High", "Cao")
        HrvStatus.UNUSUALLY_LOW -> tr("Unusually low", "Thấp bất thường")
        HrvStatus.UNUSUALLY_HIGH -> tr("Unusually high", "Cao bất thường")
        HrvStatus.NEEDS_MORE_HRV -> tr("Needs more HRV", "Cần thêm HRV")
    }
    val comparison = when {
        percent == 0 -> tr("near your usual baseline", "gần mức nền thường ngày của bạn")
        percent > 0 -> tr("$percent% above your usual baseline", "cao hơn mức nền thường ngày $percent%")
        else -> tr("${abs(percent)}% below your usual baseline", "thấp hơn mức nền thường ngày ${abs(percent)}%")
    }
    val detail = when (status) {
        HrvStatus.BALANCED -> tr("HRV is $comparison.", "HRV $comparison.")
        HrvStatus.LOW -> tr("HRV is $comparison, which can point to incomplete recovery.", "HRV $comparison, có thể cơ thể chưa hồi phục hẳn.")
        HrvStatus.HIGH -> tr("HRV is $comparison. Higher HRV can be positive when other signals agree.", "HRV $comparison. HRV cao thường là dấu hiệu tốt nếu các chỉ số khác cũng ổn.")
        HrvStatus.UNUSUALLY_LOW -> tr("HRV is $comparison, outside your usual range.", "HRV $comparison, nằm ngoài khoảng thường ngày.")
        HrvStatus.UNUSUALLY_HIGH -> tr("HRV is $comparison, outside your usual range.", "HRV $comparison, nằm ngoài khoảng thường ngày.")
        HrvStatus.NEEDS_MORE_HRV -> tr("HRV status needs more data.", "Cần thêm dữ liệu để đánh giá HRV.")
    }
    return HrvStatusInsight(
        status = status,
        label = label,
        detail = detail,
        currentRmssdMs = hrvRmssdMs,
        baselineRmssdMs = baselineRmssdMs,
        percentFromBaseline = percent,
    )
}

fun calculateIntensityMinutesReadiness(
    weeklyIntensityMinutes: DashboardWeeklyIntensityMinutes?,
    hasIntensityData: Boolean,
): IntensityMinutesReadinessInsight {
    if (!hasIntensityData || weeklyIntensityMinutes == null ||
        weeklyIntensityMinutes.confidence == IntensityMinutesConfidence.NO_DATA
    ) {
        return IntensityMinutesReadinessInsight(
            status = IntensityMinutesStatus.NEEDS_MORE_DATA,
            label = tr("Needs more data", "Cần thêm dữ liệu"),
            detail = tr("Intensity minutes need workouts, heart rate, active calories, or activity load history.", "Phút cường độ cần dữ liệu buổi tập, nhịp tim, calo vận động hoặc lịch sử tải tập."),
            moderateEquivalentMinutes = null,
            targetMinutes = DefaultWeeklyIntensityMinutesTarget,
            todayModerateEquivalentMinutes = null,
            progressPercent = 0,
            confidence = IntensityMinutesConfidence.NO_DATA,
        )
    }

    val minutes = weeklyIntensityMinutes.moderateEquivalentMinutes
    val target = weeklyIntensityMinutes.targetMinutes
    val status = when {
        target > 0 && minutes >= target -> IntensityMinutesStatus.GOAL_MET
        weeklyIntensityMinutes.isOnPace -> IntensityMinutesStatus.ON_TRACK
        target > 0 && minutes >= target * 0.5 -> IntensityMinutesStatus.BEHIND
        else -> IntensityMinutesStatus.LOW
    }
    val label = when (status) {
        IntensityMinutesStatus.GOAL_MET -> tr("Goal met", "Đã đạt mục tiêu")
        IntensityMinutesStatus.ON_TRACK -> tr("On track", "Đúng tiến độ")
        IntensityMinutesStatus.BEHIND -> tr("Behind pace", "Chậm tiến độ")
        IntensityMinutesStatus.LOW -> tr("Low", "Thấp")
        IntensityMinutesStatus.NEEDS_MORE_DATA -> tr("Needs more data", "Cần thêm dữ liệu")
    }
    val confidenceText = when (weeklyIntensityMinutes.confidence) {
        IntensityMinutesConfidence.HIGH -> tr("high confidence", "độ tin cậy cao")
        IntensityMinutesConfidence.MEDIUM -> tr("medium confidence", "độ tin cậy trung bình")
        IntensityMinutesConfidence.LOW -> tr("low confidence estimate", "ước tính, độ tin cậy thấp")
        IntensityMinutesConfidence.NO_DATA -> tr("no data", "không có dữ liệu")
    }
    val todayText = if (weeklyIntensityMinutes.todayModerateEquivalentMinutes > 0) {
        tr(" Today added ${weeklyIntensityMinutes.todayModerateEquivalentMinutes}.", " Hôm nay thêm ${weeklyIntensityMinutes.todayModerateEquivalentMinutes}.")
    } else {
        ""
    }
    val detail = when (status) {
        IntensityMinutesStatus.GOAL_MET -> {
            tr("$minutes/$target moderate-equivalent min this week; vigorous minutes count double.$todayText", "$minutes/$target phút (quy đổi mức vừa) tuần này; phút cường độ cao tính gấp đôi.$todayText")
        }
        IntensityMinutesStatus.ON_TRACK -> {
            tr("$minutes/$target moderate-equivalent min this week, on pace for day ${weeklyIntensityMinutes.daysElapsed}.", "$minutes/$target phút (quy đổi mức vừa) tuần này, đúng nhịp cho ngày thứ ${weeklyIntensityMinutes.daysElapsed}.")
        }
        IntensityMinutesStatus.BEHIND -> {
            tr("$minutes/$target moderate-equivalent min this week; expected about ${weeklyIntensityMinutes.expectedByNowMinutes} by now.", "$minutes/$target phút (quy đổi mức vừa) tuần này; đến giờ nên đạt khoảng ${weeklyIntensityMinutes.expectedByNowMinutes}.")
        }
        IntensityMinutesStatus.LOW -> {
            tr("$minutes/$target moderate-equivalent min this week; add easy aerobic work if recovery allows.", "$minutes/$target phút (quy đổi mức vừa) tuần này; thêm bài aerobic nhẹ nếu cơ thể đã hồi phục.")
        }
        IntensityMinutesStatus.NEEDS_MORE_DATA -> tr("Intensity minutes need more data.", "Phút cường độ cần thêm dữ liệu.")
    }
    return IntensityMinutesReadinessInsight(
        status = status,
        label = label,
        detail = "$detail $confidenceText.",
        moderateEquivalentMinutes = minutes,
        targetMinutes = target,
        todayModerateEquivalentMinutes = weeklyIntensityMinutes.todayModerateEquivalentMinutes,
        progressPercent = weeklyIntensityMinutes.progressPercent,
        confidence = weeklyIntensityMinutes.confidence,
    )
}

/**
 * Readiness, body energy and training readiness: a start value plus each
 * signal's delta ([DailyReadinessSignals]). Start values, deltas and state
 * bands: app heuristic, not validated.
 */
fun calculateDailyReadiness(
    data: DashboardData,
    goals: DailyReadinessGoalInputs = DailyReadinessGoalInputs(),
): DailyReadinessInsight {
    val hrv = data.hrvRmssdMs
    val hrvBaseline = data.hrvBaselineRmssdMs
    val hrvStatus = calculateHrvStatus(
        hrvRmssdMs = hrv,
        baselineRmssdMs = hrvBaseline,
        hasHrvData = DashboardMetric.HRV in data.loadedMetrics,
    )
    val intensityMinutes = calculateIntensityMinutesReadiness(
        weeklyIntensityMinutes = data.weeklyIntensityMinutes,
        hasIntensityData = DashboardMetric.INTENSITY_MINUTES in data.loadedMetrics,
    )
    val physiologicalStress = calculatePhysiologicalStress(data)

    val signals = sleepContribution(data) +
        hrvContribution(data, hrv, hrvBaseline, hrvStatus) +
        restingHeartRateContribution(data) +
        trainingLoadContribution(data) +
        intensityMinutesContribution(data, intensityMinutes) +
        physiologicalStressContribution(data, physiologicalStress) +
        temperatureContribution(data) +
        hydrationContribution(data, goals) +
        nutritionContribution(data) +
        mindfulnessContribution(data) +
        bodyEnergyContribution(data)

    var score = 68 + signals.score
    var bodyEnergyScore = signals.measuredBodyEnergy ?: (64 + signals.bodyEnergyScore)
    var trainingReadinessScore = 66 + signals.trainingReadinessScore
    val availableSignals = signals.availableSignals
    val baselineSignals = signals.baselineSignals
    val unusualVitals = signals.unusualVitals
    val missingReasons = signals.missingReasons
    val factors = signals.factors.toMutableList()

    // Not a signal of its own: what several signals being out of range says together.
    if (signals.elevatedBodySignals >= 2) {
        score -= 6
        bodyEnergyScore -= 6
        trainingReadinessScore -= 8
        factors += DailyReadinessFactor(
            kind = ReadinessFactorKind.STRESS_HIGH,
            label = tr("Body signals look elevated", "Chỉ số cơ thể đang tăng cao"),
            detail = tr("Several recovery signals are outside your usual range.", "Một số chỉ số hồi phục nằm ngoài khoảng thường ngày."),
            impact = ReadinessFactorImpact.WARNING,
        )
    }

    val clampedScore = if (availableSignals == 0) 0 else score.coerceIn(0, 100)
    val state = when {
        availableSignals == 0 -> ReadinessState.UNKNOWN
        unusualVitals && clampedScore < 55 -> ReadinessState.REST
        clampedScore >= 80 -> ReadinessState.READY
        clampedScore >= 60 -> ReadinessState.MODERATE
        clampedScore >= 40 -> ReadinessState.RECOVER
        else -> ReadinessState.REST
    }
    val recommendationType = when {
        unusualVitals && state == ReadinessState.REST -> ReadinessRecommendationType.CHECK_SYMPTOMS
        state == ReadinessState.READY -> ReadinessRecommendationType.HARD_TRAINING
        state == ReadinessState.MODERATE -> ReadinessRecommendationType.MODERATE_TRAINING
        state == ReadinessState.RECOVER -> ReadinessRecommendationType.LIGHT_ACTIVITY
        state == ReadinessState.REST -> ReadinessRecommendationType.REST
        else -> ReadinessRecommendationType.MOBILITY
    }
    val confidence = when {
        availableSignals >= 5 && baselineSignals >= 2 && missingReasons.isEmpty() -> ReadinessConfidence.HIGH
        availableSignals >= 3 && baselineSignals >= 1 -> ReadinessConfidence.MEDIUM
        else -> ReadinessConfidence.LOW
    }
    val confidenceReason = when {
        confidence == ReadinessConfidence.HIGH -> "complete_data"
        "missing_sleep_data" in missingReasons -> "missing_sleep_data"
        "missing_hrv_data" in missingReasons -> "missing_hrv_data"
        "new_user_not_enough_baseline" in missingReasons -> "new_user_not_enough_baseline"
        else -> "partial_data"
    }

    return DailyReadinessInsight(
        state = state,
        score = clampedScore,
        bodyEnergyScore = if (availableSignals == 0) 0 else bodyEnergyScore.coerceIn(0, 100),
        trainingReadinessScore = if (availableSignals == 0) 0 else trainingReadinessScore.coerceIn(0, 100),
        recommendationType = recommendationType,
        statusTitle = statusTitleFor(state),
        recommendation = recommendationFor(state, recommendationType),
        explanation = explanationFor(state, factors),
        alternative = alternativeFor(state),
        suggestedWorkout = suggestedWorkoutFor(state),
        avoid = avoidFor(state),
        strainTarget = strainTargetFor(state),
        currentStrain = currentStrainFor(data),
        adaptiveGoal = adaptiveGoalFor(state, goals),
        confidence = confidence,
        confidenceReason = confidenceReason,
        hrvStatus = hrvStatus,
        intensityMinutes = intensityMinutes,
        physiologicalStress = physiologicalStress,
        factors = factors.sortedWith(compareByDescending<DailyReadinessFactor> { it.impact.priority }.thenBy { it.label }),
        recoveryModeSuggested = state == ReadinessState.REST || clampedScore < 35,
    )
}

internal fun DashboardData.hasLoggedNutrition(): Boolean =
    (caloriesInKcal != null && caloriesInKcal > 0.0) ||
        (proteinGrams != null && proteinGrams > 0.0) ||
        (carbsGrams != null && carbsGrams > 0.0) ||
        (fatGrams != null && fatGrams > 0.0)

private val ReadinessFactorImpact.priority: Int
    get() = when (this) {
        ReadinessFactorImpact.WARNING -> 3
        ReadinessFactorImpact.NEGATIVE -> 2
        ReadinessFactorImpact.POSITIVE -> 1
        ReadinessFactorImpact.NEUTRAL -> 0
    }

private fun statusTitleFor(state: ReadinessState): String =
    when (state) {
        ReadinessState.READY -> tr("Ready to train", "Sẵn sàng tập")
        ReadinessState.MODERATE -> tr("Train, but keep it controlled", "Tập được, nhưng giữ sức")
        ReadinessState.RECOVER -> tr("Recovery day", "Ngày hồi phục")
        ReadinessState.REST -> tr("Take it easy", "Nghỉ ngơi nhẹ nhàng")
        ReadinessState.UNKNOWN -> tr("Needs more data", "Cần thêm dữ liệu")
    }

private fun recommendationFor(
    state: ReadinessState,
    type: ReadinessRecommendationType,
): String =
    when (type) {
        ReadinessRecommendationType.HARD_TRAINING -> tr("Good day for hard training if you feel normal.", "Hôm nay hợp để tập nặng nếu bạn thấy khỏe.")
        ReadinessRecommendationType.MODERATE_TRAINING -> tr("Do moderate training today, but avoid maximal effort.", "Hôm nay tập vừa sức, tránh dốc toàn lực.")
        ReadinessRecommendationType.LIGHT_ACTIVITY -> tr("Keep it light today with easy movement or mobility.", "Hôm nay chỉ nên vận động nhẹ hoặc giãn cơ.")
        ReadinessRecommendationType.REST -> tr("Avoid intense training today and focus on recovery.", "Hôm nay tránh tập nặng, tập trung hồi phục.")
        ReadinessRecommendationType.CHECK_SYMPTOMS -> {
            tr("Some signals are outside your usual range. If you feel unwell, prioritize rest and hydration.", "Vài chỉ số nằm ngoài khoảng thường ngày. Nếu thấy mệt, hãy ưu tiên nghỉ ngơi và uống đủ nước.")
        }
        ReadinessRecommendationType.MOBILITY -> when (state) {
            ReadinessState.UNKNOWN -> tr("Connect sleep, HRV, resting heart rate, and workouts for a better guide.", "Kết nối giấc ngủ, HRV, nhịp tim lúc nghỉ và buổi tập để có gợi ý chính xác hơn.")
            else -> tr("Choose mobility or an easy walk until more data is available.", "Chọn giãn cơ hoặc đi bộ nhẹ cho đến khi có thêm dữ liệu.")
        }
    }

private fun explanationFor(
    state: ReadinessState,
    factors: List<DailyReadinessFactor>,
): String {
    val meaningful = factors
        .filterNot {
            it.kind == ReadinessFactorKind.MISSING_SLEEP_DATA ||
                it.kind == ReadinessFactorKind.MISSING_HRV_DATA ||
                it.kind == ReadinessFactorKind.MISSING_INTENSITY_MINUTES ||
                it.kind == ReadinessFactorKind.MISSING_STRESS_DATA ||
                it.kind == ReadinessFactorKind.NEW_USER_NOT_ENOUGH_BASELINE
        }
        .sortedByDescending { it.impact.priority }
        .take(3)
    if (meaningful.isEmpty()) {
        return if (state == ReadinessState.UNKNOWN) {
            tr("There is not enough local data yet to explain readiness with confidence.", "Chưa đủ dữ liệu để giải thích mức sẵn sàng một cách chắc chắn.")
        } else {
            tr("Your recommendation is based on the local signals available today.", "Gợi ý này dựa trên các chỉ số có được hôm nay.")
        }
    }
    return tr("Your signals suggest this mainly because ", "Gợi ý này chủ yếu vì ") +
        meaningful.joinToNaturalText { it.detail.toExplanationClause() } +
        "."
}

private fun String.toExplanationClause(): String =
    replaceFirstChar { char -> char.lowercase(Locale.US) }
        .trimEnd()
        .trimEnd('.')

private fun alternativeFor(state: ReadinessState): String =
    when (state) {
        ReadinessState.READY -> tr("If you feel sore, reduce the plan to 30-45 min easy cardio or mobility.", "Nếu thấy đau mỏi, giảm xuống 30-45 phút cardio nhẹ hoặc giãn cơ.")
        ReadinessState.MODERATE -> tr("If you feel tired, choose a 30 min easy walk or easy ride instead.", "Nếu thấy mệt, thay bằng 30 phút đi bộ hoặc đạp xe nhẹ.")
        ReadinessState.RECOVER -> tr("If you still want movement, keep it conversational and stop if it feels harder than expected.", "Nếu vẫn muốn vận động, giữ nhịp vừa đủ để nói chuyện được và dừng lại nếu thấy nặng hơn dự kiến.")
        ReadinessState.REST -> tr("If you feel unwell, rest or seek medical advice when needed.", "Nếu thấy không khỏe, hãy nghỉ ngơi hoặc hỏi ý kiến bác sĩ khi cần.")
        ReadinessState.UNKNOWN -> tr("Start with a walk, mobility, or breathing session until more recovery data is available.", "Bắt đầu với đi bộ, giãn cơ hoặc bài thở cho đến khi có thêm dữ liệu hồi phục.")
    }

private fun suggestedWorkoutFor(state: ReadinessState): String =
    when (state) {
        ReadinessState.READY -> tr("Strength training, intervals, a long bike ride, or your normal planned workout.", "Tập sức mạnh, interval, đạp xe đường dài hoặc bài tập theo kế hoạch.")
        ReadinessState.MODERATE -> tr("Zone 2 cardio, moderate strength, technique work, or an easy bike ride.", "Cardio vùng 2, tập sức mạnh vừa phải, tập kỹ thuật hoặc đạp xe nhẹ.")
        ReadinessState.RECOVER -> tr("Walk, stretching, mobility, breathing, or an early night.", "Đi bộ, kéo giãn, giãn cơ, tập thở hoặc đi ngủ sớm.")
        ReadinessState.REST -> tr("Rest, gentle movement only, and symptom monitoring if you feel unwell.", "Nghỉ ngơi, chỉ vận động nhẹ và theo dõi triệu chứng nếu thấy không khỏe.")
        ReadinessState.UNKNOWN -> tr("Easy walk, mobility, or light stretching.", "Đi bộ nhẹ, giãn cơ hoặc kéo giãn nhẹ.")
    }

private fun avoidFor(state: ReadinessState): String =
    when (state) {
        ReadinessState.READY -> tr("Overreaching if you feel sore.", "Tập quá sức khi đang đau mỏi.")
        ReadinessState.MODERATE -> tr("Max effort, HIIT, and very long sessions.", "Dốc toàn lực, HIIT và buổi tập quá dài.")
        ReadinessState.RECOVER -> tr("Hard cardio, heavy lifting, and long intense sessions.", "Cardio nặng, nâng tạ nặng và buổi tập dài cường độ cao.")
        ReadinessState.REST -> tr("Intense training today.", "Tập cường độ cao hôm nay.")
        ReadinessState.UNKNOWN -> tr("Making a hard training decision from incomplete data.", "Quyết định tập nặng khi dữ liệu còn thiếu.")
    }

private fun strainTargetFor(state: ReadinessState): String =
    when (state) {
        ReadinessState.READY -> tr("Today's strain target: 10-14", "Mục tiêu gắng sức hôm nay: 10-14")
        ReadinessState.MODERATE -> tr("Today's strain target: 7-10", "Mục tiêu gắng sức hôm nay: 7-10")
        ReadinessState.RECOVER -> tr("Today's strain target: 3-6", "Mục tiêu gắng sức hôm nay: 3-6")
        ReadinessState.REST -> tr("Today's strain target: 0-3", "Mục tiêu gắng sức hôm nay: 0-3")
        ReadinessState.UNKNOWN -> tr("Today's strain target: 3-6", "Mục tiêu gắng sức hôm nay: 3-6")
    }

private fun currentStrainFor(data: DashboardData): String? {
    val load = data.weeklyCardioLoad ?: return null
    if (load.targetScore <= 0) return null
    val dailyTarget = (load.targetScore / 7.0).coerceAtLeast(1.0)
    val strain = (load.todayScore / dailyTarget * 7.0).coerceIn(0.0, 15.0)
    return tr("Current strain: ${formatOneDecimal(strain)}", "Mức gắng sức hiện tại: ${formatOneDecimal(strain)}")
}

private fun adaptiveGoalFor(
    state: ReadinessState,
    goals: DailyReadinessGoalInputs,
): String {
    val normalSteps = goals.stepsGoal.roundToNearestHundred()
    return when (state) {
        ReadinessState.READY -> {
            tr("Adaptive goal: ${(goals.stepsGoal * 1.1).roundToNearestHundred()} steps + workout", "Mục tiêu thích ứng: ${(goals.stepsGoal * 1.1).roundToNearestHundred()} bước + một buổi tập")
        }
        ReadinessState.MODERATE -> {
            tr("Adaptive goal: $normalSteps steps + ${goals.activeMinutesGoal.roundToInt()} active minutes", "Mục tiêu thích ứng: $normalSteps bước + ${goals.activeMinutesGoal.roundToInt()} phút vận động")
        }
        ReadinessState.RECOVER -> {
            tr("Adaptive goal: ${(goals.stepsGoal * 0.5).roundToNearestHundred()} steps + 15 min mobility", "Mục tiêu thích ứng: ${(goals.stepsGoal * 0.5).roundToNearestHundred()} bước + 15 phút giãn cơ")
        }
        ReadinessState.REST -> tr("Adaptive goal: goals are reduced; focus on rest and hydration", "Mục tiêu thích ứng: giảm mục tiêu, tập trung nghỉ ngơi và uống đủ nước")
        ReadinessState.UNKNOWN -> tr("Adaptive goal: keep movement light until more data is available", "Mục tiêu thích ứng: vận động nhẹ cho đến khi có thêm dữ liệu")
    }
}

private fun Double.roundToNearestHundred(): Int =
    ((this / 100.0).roundToInt() * 100).coerceAtLeast(0)

internal fun formatHours(hours: Double): String {
    val totalMinutes = (hours * 60.0).roundToInt().coerceAtLeast(0)
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return "${h}h ${m.toString().padStart(2, '0')}m"
}

internal fun formatOneDecimal(value: Double): String =
    String.format(Locale.US, "%.1f", value)

internal fun formatSignedOneDecimal(value: Double): String {
    val prefix = if (value > 0.0) "+" else ""
    return prefix + formatOneDecimal(value)
}

private fun <T> List<T>.joinToNaturalText(transform: (T) -> String): String =
    when (size) {
        0 -> ""
        1 -> transform(first())
        2 -> tr("${transform(this[0])} and ${transform(this[1])}", "${transform(this[0])} và ${transform(this[1])}")
        else -> dropLast(1).joinToString(separator = ", ", transform = transform) + tr(", and ", " và ") + transform(last())
    }
