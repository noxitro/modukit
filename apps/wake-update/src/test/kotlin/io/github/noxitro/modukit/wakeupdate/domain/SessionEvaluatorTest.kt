package io.github.noxitro.modukit.wakeupdate.domain

import io.github.noxitro.modukit.wakeupdate.domain.SessionEvaluator.STALE_MS
import io.github.noxitro.modukit.wakeupdate.domain.SessionEvaluator.WAKE_GRACE_MS
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionEvaluatorTest {
    private val t0 = 1_000_000L
    private val a = SessionApp("com.example.a", "A", "1.0", versionCode = 10, lastUpdateTime = 100)
    private val b = SessionApp("com.example.b", "B", "2.0", versionCode = 20, lastUpdateTime = 200)
    private val session = WakeSession(startedAt = t0, apps = listOf(a, b))

    private fun asleep(app: SessionApp) = AppState(false, app.versionName, app.versionCode, app.lastUpdateTime)
    private fun awake(app: SessionApp) = AppState(true, app.versionName, app.versionCode, app.lastUpdateTime)

    private fun evaluate(
        states: Map<String, AppState>,
        now: Long = t0 + 1_000,
        screenOffAt: Long? = null,
        s: WakeSession = session,
    ) = SessionEvaluator.evaluate(s, states, now, screenOffAt)

    @Test
    fun `right after waking nothing is reported as failed`() {
        val status = evaluate(mapOf(a.packageName to asleep(a), b.packageName to asleep(b)))
        assertEquals(SessionPhase.WAKING, status.phase)
        assertEquals(emptyList<SessionApp>(), status.notWoken)
    }

    @Test
    fun `awake apps are listed and missing ones are reported after the check`() {
        val states = mapOf(a.packageName to awake(a), b.packageName to asleep(b))
        val status = evaluate(states, s = session.copy(checkedAt = t0 + 3_000))
        assertEquals(SessionPhase.AWAKE, status.phase)
        assertEquals(listOf(a), status.awake)
        assertEquals(listOf(b), status.notWoken)
        assertEquals(1, status.wokenCount)
    }

    @Test
    fun `updates are detected by version code or install time`() {
        val states = mapOf(
            a.packageName to AppState(true, "1.1", 11, 100),
            b.packageName to AppState(true, "2.0", 20, 999),
        )
        val status = evaluate(states)
        assertEquals(
            listOf(
                UpdatedApp(a.packageName, "A", "1.0", "1.1"),
                UpdatedApp(b.packageName, "B", "2.0", "2.0"),
            ),
            status.updated,
        )
    }

    @Test
    fun `all apps back to sleep ends the session`() {
        val woke = session.copy(observedAwake = setOf(a.packageName, b.packageName), checkedAt = t0 + 3_000)
        val states = mapOf(a.packageName to asleep(a), b.packageName to AppState(false, "2.1", 21, 300))
        val status = evaluate(states, now = t0 + 60_000, screenOffAt = t0 + 50_000, s = woke)
        assertEquals(SessionPhase.ALL_ASLEEP, status.phase)
        val result = status.toResult(now = t0 + 60_000)
        assertEquals(2, result.wokenCount)
        assertEquals(listOf(UpdatedApp(b.packageName, "B", "2.0", "2.1")), result.updated)
        assertEquals(0, result.notBackToSleepCount)
    }

    @Test
    fun `apps still awake after the screen went off are flagged`() {
        val states = mapOf(a.packageName to awake(a), b.packageName to asleep(b))
        val woke = session.copy(observedAwake = setOf(a.packageName, b.packageName))
        val status = evaluate(states, now = t0 + 60_000, screenOffAt = t0 + 30_000, s = woke)
        assertEquals(SessionPhase.NOT_BACK_TO_SLEEP, status.phase)
        assertEquals(listOf(a), status.notBackToSleep)
        assertEquals(emptyList<SessionApp>(), status.awake)
    }

    @Test
    fun `a screen-off from before the wake does not count`() {
        val states = mapOf(a.packageName to awake(a), b.packageName to awake(b))
        val status = evaluate(states, now = t0 + 60_000, screenOffAt = t0 - 5_000)
        assertEquals(SessionPhase.AWAKE, status.phase)
    }

    @Test
    fun `without a screen-off event a long-running session counts as not back to sleep`() {
        val states = mapOf(a.packageName to awake(a), b.packageName to asleep(b))
        val status = evaluate(states, now = t0 + STALE_MS + 1)
        assertEquals(SessionPhase.NOT_BACK_TO_SLEEP, status.phase)
    }

    @Test
    fun `nothing woke after the grace period`() {
        val states = mapOf(a.packageName to asleep(a), b.packageName to asleep(b))
        val status = evaluate(states, now = t0 + WAKE_GRACE_MS + 1)
        assertEquals(SessionPhase.NONE_WOKE, status.phase)
        assertEquals(listOf(a, b), status.notWoken)
    }

    @Test
    fun `uninstalled apps drop out`() {
        val status = evaluate(mapOf(a.packageName to awake(a)))
        assertEquals(listOf(a), status.awake)
        assertEquals(1, status.wokenCount)
    }

    @Test
    fun `retrying keeps the original snapshot and adds new apps`() {
        val c = SessionApp("com.example.c", "C", null, 1, 1)
        val a2 = a.copy(versionCode = 99)
        val retried = session.copy(checkedAt = t0 + 3_000).including(listOf(a2, c), now = t0 + 90_000)
        assertEquals(listOf(a, b, c), retried.apps)
        assertEquals(t0 + 90_000, retried.lastWakeAt)
        assertEquals(null, retried.checkedAt)
    }
}
