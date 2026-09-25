package io.github.noxitro.modukit.wakeupdate.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.noxitro.modukit.wakeupdate.ui.icons.WakeIcons
import io.github.noxitro.modukit.wakeupdate.ui.theme.WakeColors
import io.github.noxitro.modukit.wakeupdate.ui.theme.WakeTheme

enum class Sky { NIGHT, DAWN }

private val PanelShape = RoundedCornerShape(28.dp)

/**
 * 画面上部の夜空。ディープスリープ中は月と星、アプリが起きている間は地平線から太陽が昇る。
 * 画面で一番強い色はここだけにして、ほかは静かにまとめる。
 */
@Composable
fun SkyPanel(
    sky: Sky,
    eyebrow: String,
    caption: String,
    modifier: Modifier = Modifier,
    iconPackages: List<String> = emptyList(),
    moreCount: Int = 0,
    animate: Boolean = true,
    headline: @Composable () -> Unit,
) {
    val colors = WakeTheme.colors
    val dawn by animateFloatAsState(
        targetValue = if (sky == Sky.DAWN) 1f else 0f,
        animationSpec = tween(durationMillis = if (animate) 1100 else 0, easing = FastOutSlowInEasing),
        label = "dawn",
    )
    Box(
        modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(colors.skyPanel)
            .border(1.dp, colors.skyPanelLine, PanelShape)
            .drawBehind { drawSky(dawn, colors) },
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (sky == Sky.DAWN) WakeIcons.Wake else WakeIcons.Moon,
                    contentDescription = null,
                    tint = if (sky == Sky.DAWN) colors.sun else colors.moon,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = colors.onSkyMuted)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.semantics { heading() }) { headline() }
            Spacer(Modifier.height(6.dp))
            Text(
                caption,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSkyMuted,
                modifier = Modifier.widthIn(max = 264.dp),
            )
            if (iconPackages.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                IconStack(iconPackages, moreCount, ring = colors.skyPanel, onRing = colors.onSky)
            }
        }
    }
}

/** 大きな件数と単位（「23 個のアプリ」）。 */
@Composable
fun SkyCount(count: Int, unit: String) {
    val colors = WakeTheme.colors
    Row {
        Text(
            count.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = colors.onSky,
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            unit,
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onSky,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/** 件数のない見出し（「スリープ中のアプリはありません」など）。右上の月と重ならない幅にする。 */
@Composable
fun SkyTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        color = WakeTheme.colors.onSky,
        modifier = Modifier.padding(vertical = 6.dp).widthIn(max = 232.dp),
    )
}

@Composable
private fun IconStack(packages: List<String>, more: Int, ring: Color, onRing: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp), verticalAlignment = Alignment.CenterVertically) {
        packages.forEach { pkg ->
            Box(Modifier.size(38.dp).background(ring, CircleShape), contentAlignment = Alignment.Center) {
                AppIcon(pkg, 32.dp)
            }
        }
        if (more > 0) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(ring, CircleShape)
                    .padding(3.dp)
                    .background(onRing.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("+$more", style = MaterialTheme.typography.labelMedium, color = onRing)
            }
        }
    }
}

// 星の位置（幅・高さに対する割合と半径 dp）。文字と重ならないよう右側に置く
private val Stars = listOf(
    Triple(0.74f, 0.14f, 1.3f),
    Triple(0.93f, 0.32f, 1.0f),
    Triple(0.81f, 0.47f, 0.9f),
    Triple(0.96f, 0.63f, 1.2f),
    Triple(0.72f, 0.74f, 0.8f),
    Triple(0.88f, 0.86f, 1.0f),
)

private fun DrawScope.drawSky(dawn: Float, colors: WakeColors) {
    val night = 1f - dawn

    // 星
    Stars.forEach { (fx, fy, r) ->
        drawCircle(
            color = colors.moon.copy(alpha = 0.55f * night),
            radius = r.dp.toPx(),
            center = Offset(size.width * fx, size.height * fy),
        )
    }

    // 月（夜明けとともに沈む）
    val moonRadius = 20.dp.toPx()
    val moonCenter = Offset(size.width - 56.dp.toPx(), 52.dp.toPx() + 36.dp.toPx() * dawn)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(colors.moon.copy(alpha = 0.14f * night), Color.Transparent),
            center = moonCenter,
            radius = moonRadius * 3.4f,
        ),
        radius = moonRadius * 3.4f,
        center = moonCenter,
    )
    val crescent = Path().apply {
        op(
            Path().apply { addOval(androidx.compose.ui.geometry.Rect(moonCenter, moonRadius)) },
            Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        moonCenter + Offset(moonRadius * 0.5f, -moonRadius * 0.38f),
                        moonRadius * 0.86f,
                    ),
                )
            },
            PathOperation.Difference,
        )
    }
    drawPath(crescent, color = colors.moon.copy(alpha = night))

    if (dawn <= 0f) return

    // 地平線の光と、昇る太陽
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to colors.sun.copy(alpha = 0.22f * dawn),
            startY = size.height * 0.30f,
            endY = size.height,
        ),
    )
    val sunRadius = 46.dp.toPx()
    val sunCenter = Offset(
        size.width - 80.dp.toPx(),
        size.height + sunRadius * 0.30f + (1f - dawn) * sunRadius * 1.8f,
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(colors.sun.copy(alpha = 0.42f * dawn), Color.Transparent),
            center = sunCenter,
            radius = sunRadius * 3f,
        ),
        radius = sunRadius * 3f,
        center = sunCenter,
    )
    drawCircle(colors.sun.copy(alpha = dawn), radius = sunRadius, center = sunCenter)
}
