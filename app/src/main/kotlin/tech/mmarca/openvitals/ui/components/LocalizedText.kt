package tech.mmarca.openvitals.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.core.period.rollingSpanTitle
import tech.mmarca.openvitals.core.period.usesRollingDates
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun timeRangeLabel(range: TimeRange): String = stringResource(
    when (range) {
        TimeRange.DAY -> R.string.range_day
        TimeRange.WEEK -> R.string.range_week
        TimeRange.MONTH -> R.string.range_month
        TimeRange.YEAR -> R.string.range_year
    }
)

@Composable
fun localizedDayTitle(date: LocalDate): String = when (date) {
    LocalDate.now() -> stringResource(R.string.period_today)
    LocalDate.now().minusDays(1) -> stringResource(R.string.period_yesterday)
    else -> readableDate(date, LocalLocale.current.platformLocale)
}

/**
 * A date in full words: "Thứ Hai, 2 tháng 11" / "Monday, 2 November". No "Th 2" or "thg"
 * abbreviations, which read badly in Vietnamese; the year only when it is not this year.
 */
fun readableDate(
    date: LocalDate,
    locale: Locale,
    withWeekday: Boolean = true,
    today: LocalDate = LocalDate.now(),
): String {
    val pattern = buildString {
        if (withWeekday) append("EEEE, ")
        append("d MMMM")
        if (date.year != today.year) append(", yyyy")
    }
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
}

@Composable
fun localizedPeriodTitle(
    range: TimeRange,
    period: DatePeriod,
    today: LocalDate = LocalDate.now(),
    /** Defaulted from the ambient mode, so no call site can fall back to calendar weeks by mistake. */
    weekPeriodMode: WeekPeriodMode = LocalPeriodWeekMode.current,
): String {
    val locale = LocalLocale.current.platformLocale
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM", locale)
    val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    val yearFormatter = DateTimeFormatter.ofPattern("yyyy", locale)

    return when (range) {
        TimeRange.DAY -> when (period.start) {
            today -> stringResource(R.string.period_today)
            today.minusDays(1) -> stringResource(R.string.period_yesterday)
            else -> readableDate(period.start, locale, today = today)
        }

        TimeRange.WEEK -> if (weekPeriodMode.usesRollingDates() && period.end == today) {
            stringResource(R.string.period_last_7_days)
        } else if (weekPeriodMode.usesRollingDates()) {
            // A past rolling window is a dated span, not the calendar week of its start.
            rollingSpanTitle(period, locale)
        } else if (today in period.start..period.end) {
            stringResource(R.string.period_this_week)
        } else {
            stringResource(R.string.period_week_of, dateFormatter.format(period.start))
        }

        TimeRange.MONTH -> if (weekPeriodMode.usesRollingDates() && period.end == today) {
            stringResource(R.string.period_last_30_days)
        } else if (weekPeriodMode.usesRollingDates()) {
            rollingSpanTitle(period, locale)
        } else if (period.end == today) {
            stringResource(R.string.period_this_month)
        } else {
            monthFormatter.format(period.start)
        }

        TimeRange.YEAR -> if (weekPeriodMode.usesRollingDates() && period.end == today) {
            stringResource(R.string.period_last_365_days)
        } else if (weekPeriodMode.usesRollingDates()) {
            rollingSpanTitle(period, locale)
        } else if (period.end == today) {
            stringResource(R.string.period_this_year)
        } else {
            yearFormatter.format(period.start)
        }
    }
}

fun localizedPeriodSubtitle(range: TimeRange, period: DatePeriod): String {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
    return when (range) {
        TimeRange.DAY -> readableDate(period.start, Locale.getDefault())
        TimeRange.WEEK,
        TimeRange.MONTH,
        TimeRange.YEAR -> "${dateFormatter.format(period.start)} - ${dateFormatter.format(period.end)}"
    }
}
