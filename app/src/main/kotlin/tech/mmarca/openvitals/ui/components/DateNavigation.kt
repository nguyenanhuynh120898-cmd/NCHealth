package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun DayNavigator(
    date: LocalDate,
    canGoForward: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    /** The summary keeps its settings button here, beside the calendar, instead of in a top bar. */
    onOpenSettings: (() -> Unit)? = null,
    /** Given, a week strip under the title picks the day; the arrows give way to it. */
    onSelectDate: ((LocalDate) -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .dateNavigationSwipe(
                    canGoForward = canGoForward,
                    onPrevious = onPreviousDay,
                    onNext = onNextDay,
                )
                .clickable(onClick = onOpenCalendar),
        ) {
            // One line: "Hôm nay", "Hôm qua", or the date of an older day, no weekday. It shrinks
            // to fit beside the buttons at a large font instead of wrapping.
            AutoResizeText(
                text = when (date) {
                    LocalDate.now(), LocalDate.now().minusDays(1) -> localizedDayTitle(date)
                    else -> readableDate(date, LocalLocale.current.platformLocale, withWeekday = false)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                softWrap = false,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onSelectDate == null) {
                OpenVitalsIconSurfaceButton(onClick = onPreviousDay) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = stringResource(R.string.cd_previous_day),
                    )
                }

                OpenVitalsIconSurfaceButton(
                    onClick = onNextDay,
                    enabled = canGoForward,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = stringResource(R.string.cd_next_day),
                    )
                }
            }

            OpenVitalsIconSurfaceButton(onClick = onOpenCalendar) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(R.string.cd_open_calendar),
                )
            }

            onOpenSettings?.let { openSettings ->
                OpenVitalsIconSurfaceButton(onClick = openSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                    )
                }
            }
        }
    }
    onSelectDate?.let { select -> WeekDayStrip(selected = date, onSelect = select) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDatePickerDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    /** The latest day a pick may land on. Null for none, such as a visit still to come. */
    latestDate: LocalDate? = LocalDate.now(),
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toUtcDateMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            OpenVitalsTextButton(
                onClick = {
                    val chosenDate = datePickerState.selectedDateMillis
                        ?.let(::utcDateMillisToLocalDate)
                        ?.let { date -> latestDate?.let(date::coerceAtMost) ?: date }
                    if (chosenDate != null) {
                        onConfirm(chosenDate)
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text(stringResource(R.string.action_select))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                headlineContentColor = MaterialTheme.colorScheme.onSurface,
                weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                navigationContentColor = MaterialTheme.colorScheme.onSurface,
                yearContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledYearContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                currentYearContentColor = MaterialTheme.colorScheme.onSurface,
                selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                disabledSelectedYearContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
                selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                disabledSelectedYearContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                dayContentColor = MaterialTheme.colorScheme.onSurface,
                disabledDayContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                disabledSelectedDayContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
                selectedDayContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledSelectedDayContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                todayContentColor = MaterialTheme.colorScheme.onSurface,
                todayDateBorderColor = Color.Transparent,
                dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onSurface,
                dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            ),
        )
    }
}

private fun LocalDate.toUtcDateMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun utcDateMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
