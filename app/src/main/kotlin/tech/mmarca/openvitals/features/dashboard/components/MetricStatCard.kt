package tech.mmarca.openvitals.features.dashboard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.ui.components.AnimatedMetricIcon
import tech.mmarca.openvitals.ui.components.ShimmerLine
import tech.mmarca.openvitals.ui.theme.Motion
import tech.mmarca.openvitals.features.dashboard.dashboardDisplayValue
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.accentSurfaceContainerColor

private val MetricStatIconSize = 28.dp
private val MetricStatIconGlyphSize = 16.dp
private val MetricStatProgressHeight = 3.dp

@Composable
internal fun MetricStatCard(
    title: String,
    value: DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    message: String? = null,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showTitle: Boolean = true,
    progressFraction: Float? = null,
    /** Optional action pinned to the tile's trailing edge (the watch's sync). */
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val iconContainerColor = accentSurfaceContainerColor(
        accentColor = accentColor,
        amoledAlpha = 0.16f,
        fallback = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    val progressFillColor = accentColor.copy(alpha = 0.55f)
    // The tiles pass the "Loading…" label as their message; it is drawn as a shimmer instead.
    val isLoading = message == stringResource(R.string.loading)

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                // fillMaxSize: a wrap-height Row in a fixed-height Box pins to the top.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedMetricIcon(
                    icon = icon,
                    accentColor = accentColor,
                    containerColor = iconContainerColor,
                    settled = !isLoading,
                )
                Spacer(Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (showTitle) {
                        AutoResizeText(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    // A shimmer holds the value's place while it loads, then the value fades
                    // and rises in; a later change of value slides over the old one.
                    AnimatedContent(
                        targetState = if (isLoading) null else (message ?: dashboardDisplayValue(value)),
                        transitionSpec = {
                            (fadeIn(tween(Motion.standardMillis)) +
                                slideInVertically(tween(Motion.standardMillis)) { it / 3 }) togetherWith
                                fadeOut(tween(Motion.pressMillis))
                        },
                        label = "MetricStatValue",
                    ) { shown ->
                        if (shown == null) {
                            ShimmerLine(
                                width = 72.dp,
                                height = 18.dp,
                                modifier = Modifier.padding(vertical = 3.dp),
                            )
                        } else {
                            AutoResizeText(
                                text = shown,
                                // titleMedium, measured off the shipping app; the design screenshot is stale.
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (message == null) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                            )
                        }
                    }
                    if (message == null && subtitle != null) {
                        AutoResizeText(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = subtitleColor,
                            maxLines = 1,
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(Spacing.xs))
                    trailing()
                }
            }
            if (progressFraction != null) {
                val animatedFraction by animateFloatAsState(
                    targetValue = progressFraction.coerceIn(0f, 1f),
                    animationSpec = tween(Motion.revealMillis, easing = FastOutSlowInEasing),
                    label = "MetricStatProgress",
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .height(MetricStatProgressHeight)
                        .fillMaxWidth(animatedFraction)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(progressFillColor),
                )
            }
        }
    }
}
