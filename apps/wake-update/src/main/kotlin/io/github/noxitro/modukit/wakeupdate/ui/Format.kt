package io.github.noxitro.modukit.wakeupdate.ui

import android.icu.text.RelativeDateTimeFormatter
import android.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import android.icu.text.RelativeDateTimeFormatter.Direction
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit
import android.icu.util.ULocale
import java.util.Locale

private const val DAY_MS = 24 * 60 * 60 * 1000L

/**
 * 最後に更新されてからの期間を「3 日前」「2 週間前」「4 か月前」「1 年前」のように表す。
 * 長く更新されていないアプリほど目に留まるよう、日付ではなく経過時間で見せる。
 */
fun formatAge(time: Long, now: Long, locale: Locale): String {
    val formatter = RelativeDateTimeFormatter.getInstance(ULocale.forLocale(locale))
    val days = ((now - time) / DAY_MS).coerceAtLeast(0)
    return when {
        days < 1 -> formatter.format(Direction.THIS, AbsoluteUnit.DAY)
        days < 14 -> formatter.format(days.toDouble(), Direction.LAST, RelativeUnit.DAYS)
        days < 60 -> formatter.format((days / 7).toDouble(), Direction.LAST, RelativeUnit.WEEKS)
        days < 365 -> formatter.format((days / 30).toDouble(), Direction.LAST, RelativeUnit.MONTHS)
        else -> formatter.format((days / 365).toDouble(), Direction.LAST, RelativeUnit.YEARS)
    }
}
