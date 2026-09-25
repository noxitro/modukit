package io.github.noxitro.modukit.wakeupdate.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** アプリ固有のアイコン。Icon() で色を付けて使う。 */
object WakeIcons {
    /** 三日月（ディープスリープ）。Material Symbols の bedtime と同じ形。 */
    val Moon: ImageVector by lazy {
        ImageVector.Builder("Moon", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.34f, 2.02f)
                curveTo(6.59f, 1.82f, 2f, 6.42f, 2f, 12f)
                curveToRelative(0f, 5.52f, 4.48f, 10f, 10f, 10f)
                curveToRelative(3.71f, 0f, 6.93f, -2.02f, 8.66f, -5.02f)
                curveTo(13.15f, 16.73f, 8.57f, 8.55f, 12.34f, 2.02f)
                close()
            }
        }.build()
    }

    /** 地平線から昇る太陽と上向きの矢印（起こして更新）。アプリのアイコンと同じ形。 */
    val Wake: ImageVector by lazy {
        ImageVector.Builder("Wake", 24.dp, 24.dp, 24f, 24f).apply {
            // 太陽
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.5f, 19f)
                arcTo(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 17.5f, y1 = 19f)
                close()
            }
            // 地平線
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(3f, 20f)
                lineTo(21f, 20f)
            }
            // 矢印
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 11f)
                lineTo(12f, 3.5f)
                moveTo(8.6f, 6.9f)
                lineTo(12f, 3.5f)
                lineTo(15.4f, 6.9f)
            }
        }.build()
    }
}
