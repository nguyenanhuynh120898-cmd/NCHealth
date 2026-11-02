package tech.mmarca.openvitals.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import tech.mmarca.openvitals.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A week of days to jump between with one tap, the way Whoop and Garmin open their day view.
 * The strip ends today while the chosen day is in the last week; an older day sits a few days
 * from the end, so its neighbours stay one tap away. Future days never show.
 */
@Composable
fun WeekDayStrip(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val locale = LocalLocale.current.platformLocale
    val days = remember(selected, today) { weekStripDays(selected, today) }
    // "T2" … "CN" in Vietnamese; "Th 2" would crowd the chip. English keeps "Mon".
    val weekday = remember(locale) {
        DateTimeFormatter.ofPattern(if (locale.language == "vi") "EEEEE" else "EEE", locale)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        days.forEach { day ->
            DayChip(
                weekday = weekday.format(day),
                dayOfMonth = day.dayOfMonth.toString(),
                description = readableDate(day, locale, today = today),
                isSelected = day == selected,
                isToday = day == today,
                onClick = { onSelect(day) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayChip(
    weekday: String,
    dayOfMonth: String,
    description: String,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (isSelected) scheme.primary else scheme.surfaceContainerHigh,
        label = "DayChipContainer",
    )
    val content by animateColorAsState(
        when {
            isSelected -> scheme.onPrimary
            isToday -> scheme.primary
            else -> scheme.onSurface
        },
        label = "DayChipContent",
    )
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = description
                selected = isSelected
            }
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AutoResizeText(
            text = weekday,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) content else scheme.onSurfaceVariant,
            softWrap = false,
        )
        AutoResizeText(
            text = dayOfMonth,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
            color = content,
            softWrap = false,
        )
    }
}

/** Seven consecutive days that include [selected] and never pass [today]. */
internal fun weekStripDays(selected: LocalDate, today: LocalDate): List<LocalDate> {
    val end = if (selected > today.minusDays(StripDays.toLong())) {
        today
    } else {
        selected.plusDays(LeadDays).coerceAtMost(today)
    }
    return (StripDays - 1 downTo 0).map { end.minusDays(it.toLong()) }
}

private const val StripDays = 7
private const val LeadDays = 3L

