package io.github.noxitro.modukit.wakeupdate.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.noxitro.modukit.wakeupdate.ui.theme.WakeTheme

private val GroupRadius = 26.dp

/** One UI のように、並んだ行を 1 枚の角丸の面にまとめる。index 番目の行の形。 */
fun groupedShape(index: Int, count: Int): Shape = when {
    count == 1 -> RoundedCornerShape(GroupRadius)
    index == 0 -> RoundedCornerShape(topStart = GroupRadius, topEnd = GroupRadius)
    index == count - 1 -> RoundedCornerShape(bottomStart = GroupRadius, bottomEnd = GroupRadius)
    else -> RoundedCornerShape(0.dp)
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        if (detail != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                detail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.weight(1f))
        if (action != null) {
            TextButton(onClick = onAction) { Text(action, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** 並んだ行の 1 行分の面（背景と区切り線）。 */
@Composable
fun GroupedRow(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(groupedShape(index, count))
            .background(WakeTheme.colors.listContainer),
    ) {
        content()
        if (index < count - 1) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp, end = 20.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

/** One UI 風の丸いチェック。 */
@Composable
fun RoundCheck(checked: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (checked) scheme.primaryContainer else Color.Transparent, label = "fill")
    val ring by animateColorAsState(if (checked) scheme.primaryContainer else scheme.outline, label = "ring")
    Box(
        modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = scheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** 手順の番号。順番そのものに意味があるところだけで使う。 */
@Composable
fun StepNumber(number: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(28.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
fun StepRow(number: Int, title: String, body: String?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        StepNumber(number)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(top = 3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (body != null) {
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

enum class Tone { NEUTRAL, SUCCESS, ATTENTION }

/** 注意や結果を知らせるカード。状態は色だけでなくアイコンと見出しでも伝える。 */
@Composable
fun NoticeCard(
    tone: Tone,
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = WakeTheme.colors
    val (container, accent) = when (tone) {
        Tone.NEUTRAL -> colors.listContainer to MaterialTheme.colorScheme.primary
        Tone.SUCCESS -> colors.successContainer to colors.success
        Tone.ATTENTION -> colors.attentionContainer to colors.attention
    }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = container) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = if (actions != null) 8.dp else 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (body != null) {
                Spacer(Modifier.height(6.dp))
                Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
            if (actions != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

/** アプリ名を読点でつなげて表示する（多いときは「ほか N 個」）。 */
@Composable
fun AppNames(names: List<String>, moreFormat: (Int) -> String, modifier: Modifier = Modifier) {
    val shown = names.take(4)
    val rest = names.size - shown.size
    val text = shown.joinToString("、") + if (rest > 0) " " + moreFormat(rest) else ""
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(top = 10.dp),
    )
}

/** 親指の届く位置に置く、画面下の主な操作。 */
@Composable
fun BottomActionBar(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    outlined: Boolean = false,
    helper: String? = null,
) {
    Surface(modifier.fillMaxWidth(), color = WakeTheme.colors.bottomBar) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val content: @Composable RowScope.() -> Unit = {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize))
                }
                val buttonModifier = Modifier.fillMaxWidth().height(56.dp)
                if (outlined) {
                    OutlinedButton(onClick, buttonModifier, enabled = enabled, shape = CircleShape, content = content)
                } else {
                    Button(
                        onClick = onClick,
                        modifier = buttonModifier,
                        enabled = enabled,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        content = content,
                    )
                }
                if (helper != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        helper,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
