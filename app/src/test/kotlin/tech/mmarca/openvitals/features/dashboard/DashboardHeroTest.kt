package tech.mmarca.openvitals.features.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.SleepScoreEstimate
import tech.mmarca.openvitals.domain.model.DashboardData
import java.time.LocalDate

class DashboardHeroTest {

    private val day = DashboardData(date = LocalDate.of(2026, 11, 2))

    @Test
    fun `nothing to go on gives no score`() {
        assertNull(heroReadiness(day))
    }

    @Test
    fun `sleep alone is the score`() {
        val readiness = heroReadiness(day.copy(sleepScore = SleepScoreEstimate(score = 82)))!!
        assertEquals(82, readiness.score)
        assertEquals(listOf(ReadinessInput.SLEEP), readiness.inputs)
        assertEquals(ReadinessZone.HIGH, readiness.zone)
    }

    @Test
    fun `hrv below baseline and a raised resting heart rate pull readiness down`() {
        val readiness = heroReadiness(
            day.copy(
                hrvRmssdMs = 30.0,
                hrvBaselineRmssdMs = 40.0,
                restingHeartRateBpm = 66,
                restingHeartRateBaselineBpm = 60,
            ),
        )!!
        // HRV: 50 + (0.75 - 1) * 150 = 12.5; resting: 50 - 6 * 6 = 14.
        assertEquals(13, readiness.score)
        assertEquals(ReadinessZone.LOW, readiness.zone)
    }

    @Test
    fun `values at baseline sit in the middle`() {
        val readiness = heroReadiness(
            day.copy(hrvRmssdMs = 40.0, hrvBaselineRmssdMs = 40.0, restingHeartRateBpm = 60, restingHeartRateBaselineBpm = 60),
        )!!
        assertEquals(50, readiness.score)
        assertEquals(ReadinessZone.MID, readiness.zone)
    }
}
