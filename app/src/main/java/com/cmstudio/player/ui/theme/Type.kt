package com.cmstudio.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cmstudio.player.R

@OptIn(ExperimentalTextApi::class)
private fun variableRoundedFont(weight: FontWeight): Font = Font(
    resId = R.font.gflex_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.Setting("ROND", 100f),
        FontVariation.weight(weight.weight)
    )
)

@OptIn(ExperimentalTextApi::class)
private fun variableFlexFont(weight: FontWeight): Font = Font(
    resId = R.font.roboto_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight)
    )
)

val GoogleSansRounded = FontFamily(
    variableRoundedFont(FontWeight.Normal),
    variableRoundedFont(FontWeight.Medium),
    variableRoundedFont(FontWeight.SemiBold),
    variableRoundedFont(FontWeight.Bold),
    variableRoundedFont(FontWeight.ExtraBold)
)

val RobotoFlex = FontFamily(
    variableFlexFont(FontWeight.Light),
    variableFlexFont(FontWeight.Normal),
    variableFlexFont(FontWeight.Medium),
    variableFlexFont(FontWeight.SemiBold),
    variableFlexFont(FontWeight.Bold)
)

private fun buildTypography(displayFamily: FontFamily, bodyFamily: FontFamily, tightTracking: Boolean): Typography {
    return Typography(
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = if (tightTracking) (-0.3).sp else 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = if (tightTracking) (-0.25).sp else 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 21.sp,
            lineHeight = 27.sp,
            letterSpacing = if (tightTracking) (-0.2).sp else 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            lineHeight = 25.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.2.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            letterSpacing = 0.2.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.35.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.45.sp
        )
    )
}

val AppTypography: Typography = buildTypography(GoogleSansRounded, RobotoFlex, tightTracking = true)
val SystemTypography: Typography = buildTypography(FontFamily.Default, FontFamily.Default, tightTracking = false)
val AppEmphasizedTypography: Typography = buildTypography(GoogleSansRounded, GoogleSansRounded, tightTracking = true)

val LocalEmphasizedTypography = staticCompositionLocalOf { AppEmphasizedTypography }

fun mpvxTypography(useSystemFont: Boolean): Typography =
    if (useSystemFont) SystemTypography else AppTypography

val Typography = AppTypography
