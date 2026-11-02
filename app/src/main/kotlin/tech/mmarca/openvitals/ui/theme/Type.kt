package tech.mmarca.openvitals.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import tech.mmarca.openvitals.R

/**
 * Every style claims its full line-height box, as Flutter's `Text` does.
 * Compose trims a lone line to its glyphs, which made every stacked-text
 * card ~9dp shorter than the design. Applied through one helper.
 */
private val FullLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun TextStyle.withFullLineHeight(): TextStyle = copy(
    lineHeightStyle = FullLineHeight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

private val BaseTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ).withFullLineHeight(),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontFeatureSettings = "tnum",
    ).withFullLineHeight(),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ).withFullLineHeight(),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ).withFullLineHeight(),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ).withFullLineHeight(),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ).withFullLineHeight(),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ).withFullLineHeight(),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ).withFullLineHeight(),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ).withFullLineHeight(),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ).withFullLineHeight(),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ).withFullLineHeight(),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ).withFullLineHeight(),
)

/**
 * Be Vietnam Pro, bundled so the app reads the same on every phone and every Vietnamese
 * diacritic is drawn by a face designed for it (SIL Open Font License, see assets/licenses).
 */
val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
)

val AppTypography = BaseTypography.run {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = BeVietnamPro),
        displayMedium = displayMedium.copy(fontFamily = BeVietnamPro),
        displaySmall = displaySmall.copy(fontFamily = BeVietnamPro),
        headlineLarge = headlineLarge.copy(fontFamily = BeVietnamPro),
        headlineMedium = headlineMedium.copy(fontFamily = BeVietnamPro),
        headlineSmall = headlineSmall.copy(fontFamily = BeVietnamPro),
        titleLarge = titleLarge.copy(fontFamily = BeVietnamPro),
        titleMedium = titleMedium.copy(fontFamily = BeVietnamPro),
        titleSmall = titleSmall.copy(fontFamily = BeVietnamPro),
        bodyLarge = bodyLarge.copy(fontFamily = BeVietnamPro),
        bodyMedium = bodyMedium.copy(fontFamily = BeVietnamPro),
        bodySmall = bodySmall.copy(fontFamily = BeVietnamPro),
        labelLarge = labelLarge.copy(fontFamily = BeVietnamPro),
        labelMedium = labelMedium.copy(fontFamily = BeVietnamPro),
        labelSmall = labelSmall.copy(fontFamily = BeVietnamPro),
    )
}
