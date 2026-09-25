package io.github.noxitro.modukit.wakeupdate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightScheme = lightColorScheme(
    primary = Palette.DawnDeep,
    onPrimary = Color.White,
    primaryContainer = Palette.Dawn,
    onPrimaryContainer = Palette.DawnInk,
    secondary = Palette.Periwinkle,
    onSecondary = Color.White,
    secondaryContainer = Palette.DawnMist,
    onSecondaryContainer = Palette.DawnInk,
    tertiary = Palette.Sprout,
    background = Palette.Mist,
    onBackground = Palette.Ink,
    surface = Palette.Mist,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.MistContainer,
    onSurfaceVariant = Palette.InkMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Palette.MistContainer,
    surfaceContainerHighest = Palette.MistLine,
    outline = Color(0xFF8B93AE),
    outlineVariant = Palette.MistLine,
    error = Palette.Coral,
    onError = Color.White,
    errorContainer = Palette.CoralMist,
    onErrorContainer = Palette.Coral,
)

private val DarkScheme = darkColorScheme(
    primary = Palette.DawnBright,
    onPrimary = Palette.DawnInk,
    primaryContainer = Palette.DawnBright,
    onPrimaryContainer = Palette.DawnInk,
    secondary = Palette.PeriwinkleLight,
    onSecondary = Palette.Night,
    secondaryContainer = Palette.DawnNight,
    onSecondaryContainer = Palette.DawnBright,
    tertiary = Palette.SproutLight,
    background = Palette.Night,
    onBackground = Palette.Starlight,
    surface = Palette.Night,
    onSurface = Palette.Starlight,
    surfaceVariant = Palette.NightContainer,
    onSurfaceVariant = Palette.StarlightMuted,
    surfaceContainerLowest = Palette.NightSurface,
    surfaceContainerLow = Palette.NightSurface,
    surfaceContainer = Palette.NightSurface,
    surfaceContainerHigh = Palette.NightContainer,
    surfaceContainerHighest = Palette.NightLine,
    outline = Color(0xFF6C7596),
    outlineVariant = Palette.NightLine,
    error = Palette.CoralLight,
    onError = Palette.Night,
    errorContainer = Palette.CoralNight,
    onErrorContainer = Palette.CoralLight,
)

// 日本語は文節の途中で改行しない（「更新され / ません」ではなく「Play ストアで / 更新されません」）
private val Heading = LineBreak.Heading
private val Paragraph = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)

// 64 / 20 / 17 / 15 / 13 / 11sp の段階だけを使う。書体は One UI の端末フォントに任せる。
private val WakeTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 64.sp,
        lineHeight = 68.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum",
    ),
    headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, lineBreak = Heading),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, lineBreak = Heading),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, lineBreak = Heading),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, lineBreak = Heading),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp, lineBreak = Paragraph),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp, lineBreak = Paragraph),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, lineBreak = Paragraph),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.3.sp,
    ),
)

// One UI に合わせて角を大きめに丸める
private val WakeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun WakeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWakeColors provides if (darkTheme) DarkWakeColors else LightWakeColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = WakeTypography,
            shapes = WakeShapes,
            content = content,
        )
    }
}

object WakeTheme {
    val colors: WakeColors
        @Composable @ReadOnlyComposable
        get() = LocalWakeColors.current
}
