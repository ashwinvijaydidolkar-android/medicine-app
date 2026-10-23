package com.ragini.medreminder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ragini.medreminder.R
import com.ragini.medreminder.data.DoseStatus

// ---------------------------------------------------------------- palette

object Palette {
    val Ivory = Color(0xFFFBF7F0)
    val Sand = Color(0xFFF3EDE2)
    val Indigo = Color(0xFF2E2A6B)
    val IndigoSoft = Color(0xFFE6E3F8)
    val Night = Color(0xFF121126)
    val NightCard = Color(0xFF1C1B36)
    val NightRaised = Color(0xFF272549)
    val Lavender = Color(0xFFBDB7FF)
    val Marigold = Color(0xFFF2A93B)
    val MarigoldSoft = Color(0xFFFCEACB)
    val Ink = Color(0xFF1D1B2E)
}

/** Colours that mean the same thing everywhere in the app – never used for decoration. */
object StatusColors {
    // Validated together for colour-blind separation and contrast in both light and dark mode.
    // Status is never shown by colour alone – every use carries a label or an icon too.
    val Taken = Color(0xFF1F8A70)
    val TakenDark = Color(0xFF2AA07E)
    val NotTaken = Color(0xFFBF7B0E)
    val NotTakenDark = Color(0xFFBD821C)
    val Missed = Color(0xFFB8406E)
    val MissedDark = Color(0xFFD65A8C)

    @Composable
    fun of(s: DoseStatus): Color {
        val dark = LocalIsDark.current
        return when (s) {
            DoseStatus.TAKEN -> if (dark) TakenDark else Taken
            DoseStatus.NOT_TAKEN -> if (dark) NotTakenDark else NotTaken
            DoseStatus.MISSED -> if (dark) MissedDark else Missed
            DoseStatus.PENDING -> MaterialTheme.colorScheme.outline
        }
    }
}

/** The eight colours a medicine can wear, so it is easy to recognise at a glance. */
object MedColors {
    val all = listOf(
        Color(0xFF5B54D6), // indigo
        Color(0xFFF2A93B), // marigold
        Color(0xFF1F9E89), // teal
        Color(0xFFE0607E), // rose
        Color(0xFF3A9BDC), // sky
        Color(0xFF9B5DE5), // plum
        Color(0xFF6BAF3C), // leaf
        Color(0xFFD9734A)  // terracotta
    )
    val names = listOf("Indigo", "Marigold", "Teal", "Rose", "Sky", "Plum", "Leaf", "Terracotta")
    fun of(i: Int): Color = all[((i % all.size) + all.size) % all.size]
}

private val Light: ColorScheme = lightColorScheme(
    primary = Palette.Indigo,
    onPrimary = Color.White,
    primaryContainer = Palette.IndigoSoft,
    onPrimaryContainer = Color(0xFF1C1946),
    secondary = Palette.Marigold,
    onSecondary = Color(0xFF2A1B00),
    secondaryContainer = Palette.MarigoldSoft,
    onSecondaryContainer = Color(0xFF4A3000),
    tertiary = StatusColors.Taken,
    onTertiary = Color.White,
    background = Palette.Ivory,
    onBackground = Palette.Ink,
    surface = Color.White,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.Sand,
    onSurfaceVariant = Color(0xFF5D5870),
    surfaceContainer = Color(0xFFF7F2EA),
    surfaceContainerHigh = Color(0xFFF1EBE0),
    outline = Color(0xFFB9B2A6),
    outlineVariant = Color(0xFFE4DDD0),
    error = StatusColors.Missed,
    onError = Color.White
)

private val Dark: ColorScheme = darkColorScheme(
    primary = Palette.Lavender,
    onPrimary = Color(0xFF1C1946),
    primaryContainer = Color(0xFF3B3685),
    onPrimaryContainer = Color(0xFFE6E3FF),
    secondary = Palette.Marigold,
    onSecondary = Color(0xFF2A1B00),
    secondaryContainer = Color(0xFF5A3D08),
    onSecondaryContainer = Color(0xFFFCE3B8),
    tertiary = StatusColors.TakenDark,
    onTertiary = Color(0xFF00241B),
    background = Palette.Night,
    onBackground = Color(0xFFEDEAF7),
    surface = Palette.NightCard,
    onSurface = Color(0xFFEDEAF7),
    surfaceVariant = Palette.NightRaised,
    onSurfaceVariant = Color(0xFFB8B4D2),
    surfaceContainer = Color(0xFF1A1932),
    surfaceContainerHigh = Color(0xFF23223F),
    outline = Color(0xFF6E6A92),
    outlineVariant = Color(0xFF34325A),
    error = StatusColors.MissedDark,
    onError = Color(0xFF3B0A02)
)

// ---------------------------------------------------------------- type

val Lexend = FontFamily(
    Font(R.font.lexend_light, FontWeight.Light),
    Font(R.font.lexend_regular, FontWeight.Normal),
    Font(R.font.lexend_medium, FontWeight.Medium),
    Font(R.font.lexend_semibold, FontWeight.SemiBold),
    Font(R.font.lexend_bold, FontWeight.Bold)
)

val Fraunces = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_bold, FontWeight.Bold)
)

// Sizes are deliberately larger than Android's defaults – this app is read by older eyes.
private val AppType = Typography(
    displayLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 60.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 46.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.Medium, fontSize = 19.sp, lineHeight = 25.sp),
    titleSmall = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.Normal, fontSize = 19.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 25.sp),
    bodySmall = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 19.sp),
    labelSmall = TextStyle(fontFamily = Lexend, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.6.sp)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

val LocalIsDark = staticCompositionLocalOf { false }

/**
 * @param themeMode 0 = follow the phone, 1 = light, 2 = dark
 * @param textScale extra text size on top of the phone's own setting
 */
@Composable
fun RaginiTheme(themeMode: Int = 0, textScale: Float = 1f, forceDark: Boolean = false, content: @Composable () -> Unit) {
    val dark = forceDark || when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalIsDark provides dark,
        LocalDensity provides Density(base.density, base.fontScale * textScale)
    ) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            typography = AppType,
            shapes = AppShapes,
            content = content
        )
    }
}
