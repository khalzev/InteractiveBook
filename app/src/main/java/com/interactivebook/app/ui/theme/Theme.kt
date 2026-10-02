package com.interactivebook.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.interactivebook.app.R

// Основной шрифт (пункт 3 — Epistle, SIL OFL, кириллица). Пока доступна только одна
// гарнитура (Regular); для headline/title Compose синтезирует полужирное начертание
// автоматически (стандартное поведение fontSynthesis) — если позже появится настоящий
// Bold-файл, достаточно добавить Font(R.font.epistle_bold, FontWeight.Bold) сюда же.
private val EpistleFontFamily = FontFamily(
    Font(R.font.epistle_regular, FontWeight.Normal)
)

// Книжная палитра: тёплый пергамент на тёмном фоне, вдохновлено разделом 80 (визуальная философия)
private val Ink = Color(0xFF1C1712)
private val Parchment = Color(0xFFE8DCC4)
private val ParchmentDim = Color(0xFFB5A27C)
private val Accent = Color(0xFF9C6B3E)
private val AccentLight = Color(0xFFD8B98A)

private val InteractiveBookDarkColors = darkColorScheme(
    primary = AccentLight,
    onPrimary = Ink,
    secondary = ParchmentDim,
    onSecondary = Ink,
    background = Ink,
    onBackground = Parchment,
    surface = Color(0xFF261F17),
    onSurface = Parchment,
    surfaceVariant = Color(0xFF332A1E),
    onSurfaceVariant = ParchmentDim,
    outline = ParchmentDim
)

private val InteractiveBookLightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = ParchmentDim,
    onSecondary = Ink,
    background = Parchment,
    onBackground = Ink,
    surface = Color(0xFFF3EAD6),
    onSurface = Ink,
    surfaceVariant = Color(0xFFDCCBA6),
    onSurfaceVariant = Ink,
    outline = ParchmentDim
)

private val InteractiveBookTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = EpistleFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = EpistleFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = EpistleFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp
    ),
    labelLarge = TextStyle(
        fontFamily = EpistleFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )
)

@Composable
fun InteractiveBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) InteractiveBookDarkColors else InteractiveBookLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = InteractiveBookTypography,
        content = content
    )
}
