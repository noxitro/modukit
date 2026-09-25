package io.github.noxitro.modukit.wakeupdate.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.noxitro.modukit.wakeupdate.domain.AppSource
import io.github.noxitro.modukit.wakeupdate.domain.SessionApp
import io.github.noxitro.modukit.wakeupdate.domain.SessionPhase
import io.github.noxitro.modukit.wakeupdate.domain.SessionStatus
import io.github.noxitro.modukit.wakeupdate.domain.SleepingApp
import io.github.noxitro.modukit.wakeupdate.domain.UpdatedApp
import io.github.noxitro.modukit.wakeupdate.domain.WakeResult
import io.github.noxitro.modukit.wakeupdate.ui.home.AppIconLoader

/** スクリーンショット用の見本データ（実在のアプリではない）。 */
object Samples {
    const val DAY = 24 * 60 * 60 * 1000L
    const val NOW = 1_790_000_000_000L

    private val playNames = listOf(
        "天気予報" to 34, "家計簿ノート" to 61, "乗換案内" to 22, "レシピ帳" to 140, "ポイントカード" to 97,
        "ホテル予約" to 45, "フードデリバリー" to 28, "電子書籍リーダー" to 73, "英単語 3000" to 190,
        "歩数計" to 52, "クーポン" to 38, "映画館チケット" to 119, "カーシェア" to 64, "美術館ガイド" to 230,
        "駐車場さがし" to 41, "ラジオ" to 88, "観光マップ" to 156, "ドラッグストア" to 30, "スーパーのチラシ" to 26,
        "年賀状作成" to 280,
    )

    private fun app(index: Int, name: String, days: Int, source: AppSource) = SleepingApp(
        packageName = "com.example.app$index",
        label = name,
        source = source,
        versionName = "${1 + index % 6}.${(index * 7) % 20}.${index % 4}",
        versionCode = 100L + index,
        lastUpdateTime = NOW - days * DAY,
    )

    val playApps: List<SleepingApp> = playNames.mapIndexed { i, (name, days) -> app(i, name, days, AppSource.PLAY_STORE) }
    val otherApps: List<SleepingApp> = listOf(
        app(90, "Galaxy テーマ", 70, AppSource.GALAXY_STORE),
        app(91, "Good Lock", 55, AppSource.GALAXY_STORE),
        app(92, "社内アプリ", 300, AppSource.OTHER),
    )

    private val excluded = setOf("com.example.app13", "com.example.app19")

    val idle = HomeUiState(
        loading = false,
        playApps = playApps.map { AppItem(it, it.packageName !in excluded) },
        otherApps = otherApps.map { AppItem(it, false) },
        lastResult = WakeResult(
            finishedAt = NOW - 11 * DAY,
            wokenCount = 12,
            updated = List(5) { UpdatedApp("com.example.app$it", "app$it") },
        ),
    )

    private fun sessionApp(app: SleepingApp) =
        SessionApp(app.packageName, app.label, app.versionName, app.versionCode, app.lastUpdateTime)

    private val woken = playApps.filter { it.packageName !in excluded }.map(::sessionApp)

    val awake = idle.copy(
        playApps = idle.playApps.filter { it.app.packageName in excluded },
        session = SessionStatus(
            phase = SessionPhase.AWAKE,
            awake = woken.drop(1),
            notBackToSleep = emptyList(),
            notWoken = woken.take(1),
            updated = listOf(
                UpdatedApp(woken[2].packageName, woken[2].label, "3.14.2", "3.15.0"),
                UpdatedApp(woken[5].packageName, woken[5].label, "2.8.1", "2.9.0"),
                UpdatedApp(woken[9].packageName, woken[9].label, "1.2.0", "1.3.4"),
            ),
            observedAwake = woken.drop(1).map { it.packageName }.toSet(),
            wokenCount = woken.size - 1,
        ),
    )

    val notBack = idle.copy(
        session = SessionStatus(
            phase = SessionPhase.NOT_BACK_TO_SLEEP,
            awake = emptyList(),
            notBackToSleep = woken.take(3),
            notWoken = emptyList(),
            updated = emptyList(),
            observedAwake = woken.map { it.packageName }.toSet(),
            wokenCount = woken.size,
        ),
    )

    val noneWoke = idle.copy(
        session = SessionStatus(
            phase = SessionPhase.NONE_WOKE,
            awake = emptyList(),
            notBackToSleep = emptyList(),
            notWoken = woken,
            updated = emptyList(),
            observedAwake = emptySet(),
            wokenCount = 0,
        ),
    )

    val empty = HomeUiState(loading = false, lastResult = idle.lastResult)
    val notGalaxy = HomeUiState(loading = false, isGalaxy = false)

    /** アプリのアイコンの代わりに、色の付いた角丸の四角を描く。 */
    val icons = AppIconLoader { packageName, sizePx -> fakeIcon(packageName, sizePx) }

    private val iconColors = listOf(
        0xFF3D7BF7, 0xFFE8554E, 0xFF22A06B, 0xFFF29D38, 0xFF8E5BD8, 0xFF1FA2B8, 0xFFEC6FA0, 0xFF5B6B7F,
    ).map { it.toInt() }

    private fun fakeIcon(packageName: String, sizePx: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val color = iconColors[Math.floorMod(packageName.hashCode(), iconColors.size)]
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val r = sizePx * 0.28f
        canvas.drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), r, r, paint)
        paint.color = 0xE6FFFFFF.toInt()
        canvas.drawCircle(sizePx * 0.5f, sizePx * 0.5f, sizePx * 0.2f, paint)
        return bitmap.asImageBitmap()
    }
}
