package io.github.noxitro.modukit.wakeupdate

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.noxitro.modukit.wakeupdate.domain.AppSource
import io.github.noxitro.modukit.wakeupdate.domain.SessionPhase
import io.github.noxitro.modukit.wakeupdate.domain.SessionStatus
import io.github.noxitro.modukit.wakeupdate.domain.SleepingApp
import io.github.noxitro.modukit.wakeupdate.domain.UpdatedApp
import io.github.noxitro.modukit.wakeupdate.domain.WakeResult
import io.github.noxitro.modukit.wakeupdate.ui.HomeViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 起こす → 更新する → 画面オフでスリープに戻る、の 1 周をエミュレータで確かめる。
 *
 * ディープスリープは Galaxy の機能なので、Galaxy がすることをシェルで再現する。
 * - ディープスリープ中: `pm disable-user`（Galaxy はアプリを無効化状態にする）
 * - 起こす: `pm enable`（Galaxy は起動されたアプリを一時的に有効にする）
 * - Play ストアで更新: `pm install -r` で v1 から v2 に上げる
 * - 画面オフでスリープに戻る: `input keyevent KEYCODE_SLEEP` のあと `pm disable-user`
 *
 * 起こすアプリ（:apps:wake-update:sleeper）の APK は、テストの前に /data/local/tmp に置いておく。
 */
@RunWith(AndroidJUnit4::class)
class SleepCycleTest {
    private val app = ApplicationProvider.getApplicationContext<WakeUpdateApp>()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        shell("input keyevent KEYCODE_WAKEUP")
        shell("wm dismiss-keyguard")
        shell("pm uninstall $SLEEPER")
        assertInstalled(shell("pm install $SLEEPER_V1"))
        shell("pm disable-user --user 0 $SLEEPER")
        app.prefs.clearSession()
        waitUntil("前のセッションが消える") { app.prefs.current.session == null }
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModel = HomeViewModel(app) }
    }

    @After
    fun tearDown() {
        shell("input keyevent KEYCODE_WAKEUP")
        shell("pm uninstall $SLEEPER")
    }

    @Test
    fun updatedAppGoesBackToSleepAndIsReportedAsUpdated() {
        val startedAt = System.currentTimeMillis()
        wakeSleeper()

        assertInstalled(shell("pm install -r $SLEEPER_V2"))
        val awake = awaitPhase(SessionPhase.AWAKE)
        assertEquals(listOf(UPDATED), awake.updated)

        screenOff()
        shell("pm disable-user --user 0 $SLEEPER")

        val result = awaitResult(after = startedAt)
        assertEquals(1, result.wokenCount)
        assertEquals(listOf(UPDATED), result.updated)
        assertEquals(0, result.notBackToSleepCount)
        assertFalse(app.installedApps.states(listOf(SLEEPER)).getValue(SLEEPER).enabled)
    }

    @Test
    fun appStillAwakeAfterScreenOffIsFlagged() {
        wakeSleeper()
        assertInstalled(shell("pm install -r $SLEEPER_V2"))
        awaitPhase(SessionPhase.AWAKE)

        // 画面をオフにしても無効化状態に戻らない（ディープスリープの一覧から外れた）
        screenOff()

        val status = awaitPhase(SessionPhase.NOT_BACK_TO_SLEEP)
        assertEquals(listOf(SLEEPER), status.notBackToSleep.map { it.packageName })
        assertEquals(listOf(UPDATED), status.updated)
    }

    /** ディープスリープ中の Sleeper v1 を起こす直前の記録を取り、Galaxy が起こしたのと同じ状態にする。 */
    private fun wakeSleeper() {
        val asleep = app.installedApps.states(listOf(SLEEPER))[SLEEPER]
        assertNotNull("Sleeper が見えない（<queries> の範囲外）", asleep)
        assertFalse(asleep!!.enabled)
        assertEquals(1L, asleep.versionCode)

        // Galaxy 以外ではランチャーの Activity を解決できず sleepingApps() に出ないので、対象は直接渡す
        app.beginSession(
            listOf(
                SleepingApp(
                    packageName = SLEEPER,
                    label = LABEL,
                    source = AppSource.OTHER,
                    versionName = asleep.versionName,
                    versionCode = asleep.versionCode,
                    lastUpdateTime = asleep.lastUpdateTime,
                ),
            ),
        )
        waitUntil("セッションが始まる") { app.prefs.current.session != null }
        shell("pm enable $SLEEPER")
    }

    private fun screenOff() {
        val lastWakeAt = app.prefs.current.session!!.lastWakeAt
        shell("input keyevent KEYCODE_SLEEP")
        waitUntil("画面オフが記録される") { (app.prefs.current.screenOffAt ?: 0L) > lastWakeAt }
    }

    /** 画面に戻ったときと同じく読み直して、セッションがその段階になるのを待つ。 */
    private fun awaitPhase(phase: SessionPhase): SessionStatus {
        var status: SessionStatus? = null
        waitUntil("セッションが $phase になる（今は ${status?.phase}）") {
            viewModel.refresh()
            status = viewModel.state.value.session
            status?.phase == phase
        }
        return status!!
    }

    /** すべてスリープに戻ると、セッションは終わって「前回」の結果になる。 */
    private fun awaitResult(after: Long): WakeResult {
        waitUntil("セッションが終わる（今は ${viewModel.state.value.session?.phase}）") {
            viewModel.refresh()
            val snapshot = app.prefs.current
            snapshot.session == null && (snapshot.lastResult?.finishedAt ?: 0L) >= after
        }
        return app.prefs.current.lastResult!!
    }

    private fun assertInstalled(output: String) =
        assertTrue("インストールできなかった: $output", output.contains("Success"))

    private fun waitUntil(what: String, timeoutMs: Long = 15_000, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (!condition()) {
            if (SystemClock.uptimeMillis() > deadline) throw AssertionError("待っても変わらなかった: $what")
            SystemClock.sleep(200)
        }
    }

    private fun shell(command: String): String {
        val fd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use { it.readText() }
    }

    private companion object {
        const val SLEEPER = "io.github.noxitro.modukit.sleeper"
        const val LABEL = "Sleeper"
        const val SLEEPER_V1 = "/data/local/tmp/sleeper-v1.apk"
        const val SLEEPER_V2 = "/data/local/tmp/sleeper-v2.apk"
        val UPDATED = UpdatedApp(SLEEPER, LABEL, "1.0", "2.0")
    }
}
