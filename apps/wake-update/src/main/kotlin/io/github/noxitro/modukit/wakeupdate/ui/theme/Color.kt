package io.github.noxitro.modukit.wakeupdate.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 夜（ディープスリープ）から夜明け（起きている）へ。
 *
 * 夜空のパネルだけを強い色にして、ほかは静かな面と線でまとめる。
 */
internal object Palette {
    val NightPanel = Color(0xFF141B35)
    val NightPanelRaised = Color(0xFF192144)
    val NightPanelLine = Color(0xFF2A3463)
    val Night = Color(0xFF0D1222)
    val NightSurface = Color(0xFF161C30)
    val NightContainer = Color(0xFF1D2439)
    val NightLine = Color(0xFF2A3350)

    val Mist = Color(0xFFF3F5FA)
    val MistContainer = Color(0xFFE8ECF5)
    val MistLine = Color(0xFFDCE1ED)
    val Ink = Color(0xFF121831)
    val InkMuted = Color(0xFF596281)
    val Starlight = Color(0xFFE9ECF5)
    val StarlightMuted = Color(0xFF9CA5C2)

    val Dawn = Color(0xFFF5A524)
    val DawnBright = Color(0xFFFFB547)
    val DawnInk = Color(0xFF241600)
    val DawnDeep = Color(0xFF955800)
    val DawnMist = Color(0xFFFFEBCB)
    val DawnNight = Color(0xFF3A2A12)

    val Moon = Color(0xFFD3DAFF)
    val MoonMuted = Color(0xFFA9B3D9)
    val Periwinkle = Color(0xFF4B57A6)
    val PeriwinkleLight = Color(0xFFB3BCF3)

    val Sprout = Color(0xFF237F55)
    val SproutLight = Color(0xFF63D6A0)
    val SproutMist = Color(0xFFDDF3E8)
    val SproutNight = Color(0xFF15372A)

    val Coral = Color(0xFFC53A42)
    val CoralLight = Color(0xFFFF8F94)
    val CoralMist = Color(0xFFFCE3E3)
    val CoralNight = Color(0xFF45191D)
}

/** Material の色の役割に当てはまらない、このアプリ固有の色。 */
@Immutable
data class WakeColors(
    val skyPanel: Color,
    val skyPanelLine: Color,
    val onSky: Color,
    val onSkyMuted: Color,
    val moon: Color,
    val sun: Color,
    val sleepingTag: Color,
    val success: Color,
    val successContainer: Color,
    val attention: Color,
    val attentionContainer: Color,
    val listContainer: Color,
    val bottomBar: Color,
)

internal val LightWakeColors = WakeColors(
    skyPanel = Palette.NightPanel,
    skyPanelLine = Palette.NightPanel,
    onSky = Color.White,
    onSkyMuted = Palette.MoonMuted,
    moon = Palette.Moon,
    sun = Palette.DawnBright,
    sleepingTag = Palette.Periwinkle,
    success = Palette.Sprout,
    successContainer = Palette.SproutMist,
    attention = Palette.Coral,
    attentionContainer = Palette.CoralMist,
    listContainer = Color.White,
    bottomBar = Palette.Mist,
)

internal val DarkWakeColors = WakeColors(
    skyPanel = Palette.NightPanelRaised,
    skyPanelLine = Palette.NightPanelLine,
    onSky = Color.White,
    onSkyMuted = Palette.MoonMuted,
    moon = Palette.Moon,
    sun = Palette.DawnBright,
    sleepingTag = Palette.PeriwinkleLight,
    success = Palette.SproutLight,
    successContainer = Palette.SproutNight,
    attention = Palette.CoralLight,
    attentionContainer = Palette.CoralNight,
    listContainer = Palette.NightSurface,
    bottomBar = Palette.Night,
)

val LocalWakeColors = staticCompositionLocalOf { LightWakeColors }
