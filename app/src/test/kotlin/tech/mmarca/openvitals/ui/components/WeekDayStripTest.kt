package tech.mmarca.openvitals.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeekDayStripTest {
    private val today = LocalDate.of(2026, 11, 2)

    @Test
    fun `a day in the last week shows the week ending today`() {
        val days = weekStripDays(selected = today.minusDays(1), today = today)

        assertEquals(today.minusDays(6), days.first())
        assertEquals(today, days.last())
    }

    @Test
    fun `an older day keeps three later days in view`() {
        val selected = today.minusDays(20)

        val days = weekStripDays(selected = selected, today = today)

        assertEquals(selected.plusDays(3), days.last())
        assertEquals(7, days.size)
    }
}
