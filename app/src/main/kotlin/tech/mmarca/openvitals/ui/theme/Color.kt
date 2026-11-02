package tech.mmarca.openvitals.ui.theme

import androidx.compose.ui.graphics.Color

// Primary brand
val OpenVitalsBlue = Color(0xFF006E8F)
val OpenVitalsGreen = Color(0xFF2F6F4F)
val OpenVitalsCoral = Color(0xFF9B4438)

val Blue80 = Color(0xFF82D2F2)
val BlueGrey80 = Color(0xFFC6C7D0)
val Teal80 = Color(0xFF80D7BE)

val Blue40 = OpenVitalsBlue
val BlueGrey40 = Color(0xFF5E5F68)
val Teal40 = OpenVitalsGreen

/**
 * Metric accent colours from the design system, contrast-audited: every
 * one clears WCAG 3:1 against both surfaces, worst case 3.09. Never
 * brighten one without re-measuring. Data only, never interactive chrome.
 */
val StepsColor = Color(0xFF21A380)
val DistanceColor = Color(0xFF3A8FD6)
val SleepColor = Color(0xFF7564E8)
val HeartColor = Color(0xFFE8536A)
val VitalsColor = Color(0xFFE2604F)
val WeightColor = Color(0xFFC68226)
val CaloriesColor = Color(0xFFEC692E)
val HydrationColor = Color(0xFF2E9BCA)
val NutritionColor = Color(0xFF54A24C)
val WorkoutColor = Color(0xFF1F9E99)
val BodyFatColor = Color(0xFFB0846A)
val FloorsColor = Color(0xFFAE8D29)
val ActiveCaloriesColor = Color(0xFFEF6640)
val ElevationColor = Color(0xFF6F9E41)
val WheelchairPushesColor = Color(0xFF2A9888)
val MindfulnessColor = Color(0xFF9A79C4)
val CycleColor = Color(0xFFDE6697)

// Cycle phases reuse audited accents, so they clear 3:1 on both surfaces. Menstrual is CycleColor.
val FollicularPhaseColor = StepsColor
val OvulatoryPhaseColor = FloorsColor
val LutealPhaseColor = SleepColor

/** Medical records borrow the distance blue: a calm, clinical accent with its contrast already checked. */
val MedicalRecordsColor = DistanceColor

// Surface variants
val SurfaceDark = Color(0xFF1A1C1E)
val SurfaceContainerDark = Color(0xFF2B2D30)

// Activity recording outdoor (sunlight) readability
val RecordingOutdoorAccent = Color(0xFFFFB300)
val RecordingOutdoorLightAccent = Color(0xFFE65100)
val RecordingOutdoorAccentMuted = Color(0xFFFF8F00)
val RecordingOutdoorBackground = Color(0xFF000000)
val RecordingOutdoorSurface = Color(0xFF121212)
