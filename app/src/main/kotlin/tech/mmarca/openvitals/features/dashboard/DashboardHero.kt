package tech.mmarca.openvitals.features.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.Motion
import tech.mmarca.openvitals.ui.theme.SleepColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.WeightColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.text.NumberFormat

/** What fed the readiness score. */
internal enum class ReadinessInput { SLEEP, HRV, RESTING_HR, BODY_ENERGY }

internal enum class ReadinessZone { HIGH, MID, LOW }

internal data class HeroReadiness(val score: Int, val inputs: List<ReadinessInput>) {
    val zone: ReadinessZone = when {
        score >= 67 -> ReadinessZone.HIGH
        score >= 34 -> ReadinessZone.MID
        else -> ReadinessZone.LOW
    }
}

/**
 * A day's readiness in the spirit of Whoop's recovery: the plain average of the signals the
 * wearable gave, each read against the person's own baseline. Sleep score as is; HRV above
 * baseline raises it, resting heart rate above baseline lowers it. Null with nothing to go on.
 */
internal fun heroReadiness(data: DashboardData): HeroReadiness? {
    val parts = buildList {
        data.sleepScore.score.takeIf { it > 0 }?.let { add(ReadinessInput.SLEEP to it.toDouble()) }
        val hrv = data.hrvRmssdMs
        val hrvBase = data.hrvBaselineRmssdMs
        if (hrv != null && hrvBase != null && hrvBase > 0) {
            add(ReadinessInput.HRV to (50 + (hrv / hrvBase - 1) * 150).coerceIn(0.0, 100.0))
        }
        val rhrBase = data.restingHeartRateBaselineBpm
        if (data.restingHeartRateBpm > 0 && rhrBase != null && rhrBase > 0) {
            add(ReadinessInput.RESTING_HR to (50 + (rhrBase - data.restingHeartRateBpm) * 6.0).coerceIn(0.0, 100.0))
        }
        // Body energy counts only when it was measured: a timeline with no heart rate and no
        // sleep behind it is the model's starting value, not a reading.
        data.bodyEnergyTimeline
            ?.takeIf { it.confidence != BodyEnergyConfidence.NO_DATA }
            ?.takeIf { it.inputSummary.heartRateSampleCount > 0 || it.inputSummary.sleepSessionCount > 0 }
            ?.let { add(ReadinessInput.BODY_ENERGY to it.currentScore.toDouble().coerceIn(0.0, 100.0)) }
    }
    if (parts.isEmpty()) return null
    return HeroReadiness(parts.map { it.second }.average().toInt(), parts.map { it.first })
}

private data class HeroBar(val label: String, val value: String, val fraction: Float, val color: Color)

/** The top of the summary: one big readiness dial and the three numbers that matter most. */
@Composable
internal fun DashboardHeroCard(
    data: DashboardData,
    display: DashboardDisplayState,
    modifier: Modifier = Modifier,
) {
    val readiness = remember(data) { heroReadiness(data) }
    val numbers = NumberFormat.getIntegerInstance()
    val bars = buildList {
        if (data.sleepScore.score > 0) {
            add(HeroBar(stringResource(R.string.recovery_sleep_score), "${data.sleepScore.score}", data.sleepScore.score / 100f, SleepColor))
        } else {
            data.sleep?.let { night ->
                val hours = String.format("%.1f", night.durationHours)
                add(HeroBar(stringResource(R.string.metric_sleep), stringResource(R.string.hero_hours, hours), (night.durationHours / 8.0).toFloat(), SleepColor))
            }
        }
        if (data.steps > 0) {
            val fraction = display.widgets[DashboardWidgetId.STEPS]?.progress?.fraction ?: (data.steps / 8000f)
            add(HeroBar(stringResource(R.string.metric_steps), numbers.format(data.steps), fraction, StepsColor))
        }
        data.weeklyCardioLoad?.takeIf { it.targetScore > 0 }?.let { load ->
            add(HeroBar(stringResource(R.string.metric_weekly_cardio_load), "${load.currentScore}/${load.targetScore}", load.currentScore.toFloat() / load.targetScore, WorkoutColor))
        }
    }
    if (readiness == null && bars.isEmpty()) return

    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReadinessDial(readiness = readiness, size = 128.dp)
            Spacer(Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                readiness?.let { ReadinessHeadline(it) }
                    ?: Text(
                        text = stringResource(R.string.hero_no_data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                bars.forEach { bar -> HeroBarRow(bar) }
            }
        }
    }
}

@Composable
private fun ReadinessHeadline(readiness: HeroReadiness) {
    val (text, color) = when (readiness.zone) {
        ReadinessZone.HIGH -> stringResource(R.string.hero_zone_high) to StepsColor
        ReadinessZone.MID -> stringResource(R.string.hero_zone_mid) to WeightColor
        ReadinessZone.LOW -> stringResource(R.string.hero_zone_low) to HeartColor
    }
    val inputs = readiness.inputs.map { input ->
        stringResource(
            when (input) {
                ReadinessInput.SLEEP -> R.string.hero_input_sleep
                ReadinessInput.HRV -> R.string.hero_input_hrv
                ReadinessInput.RESTING_HR -> R.string.hero_input_rhr
                ReadinessInput.BODY_ENERGY -> R.string.hero_input_energy
            },
        )
    }.joinToString(", ")
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AutoResizeText(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = color, maxLines = 2)
        AutoResizeText(
            text = stringResource(R.string.hero_basis, inputs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

@Composable
private fun ReadinessDial(readiness: HeroReadiness?, size: Dp) {
    val color = when (readiness?.zone) {
        ReadinessZone.HIGH -> StepsColor
        ReadinessZone.MID -> WeightColor
        ReadinessZone.LOW -> HeartColor
        null -> MaterialTheme.colorScheme.outline
    }
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(readiness?.score) {
        sweep.animateTo((readiness?.score ?: 0) / 100f, tween(Motion.ambientMillis, easing = FastOutSlowInEasing))
    }
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = 12.dp.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(track, startAngle = 135f, sweepAngle = 270f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, startAngle = 135f, sweepAngle = 270f * sweep.value, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = readiness?.let { "${(sweep.value * 100).toInt()}" } ?: "–",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AutoResizeText(
                text = stringResource(R.string.hero_readiness_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeroBarRow(bar: HeroBar) {
    val fill = remember { Animatable(0f) }
    LaunchedEffect(bar.fraction) {
        fill.animateTo(bar.fraction.coerceIn(0f, 1f), tween(Motion.revealMillis, easing = FastOutSlowInEasing))
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AutoResizeText(
                text = bar.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            Text(text = bar.value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill.value)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(bar.color),
            )
        }
    }
}
