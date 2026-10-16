package io.github.noxitro.modukit.wakeupdate.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.noxitro.modukit.wakeupdate.WakeUpdateApp
import io.github.noxitro.modukit.wakeupdate.domain.SessionApp
import io.github.noxitro.modukit.wakeupdate.domain.WakeResult
import io.github.noxitro.modukit.wakeupdate.domain.WakeSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PrefsTest {
    private val prefs = ApplicationProvider.getApplicationContext<WakeUpdateApp>().prefs
    private val session = WakeSession(
        startedAt = 1_000L,
        apps = listOf(SessionApp("com.example.maps", "Maps", "1.0", 1, 0)),
    )

    @Test
    fun `a session closed while the app states were being read does not come back`() {
        prefs.setSession(session)
        val readBefore = prefs.current.session!!

        prefs.clearSession()
        prefs.updateSession(readBefore.startedAt) { it.copy(observedAwake = setOf("com.example.maps")) }
        prefs.finishSession(readBefore.startedAt, WakeResult(finishedAt = 2_000L, wokenCount = 1, updated = emptyList()))

        assertNull(prefs.current.session)
        assertNull(prefs.current.lastResult)
    }

    @Test
    fun `updates keep what was added to the session meanwhile`() {
        prefs.setSession(session)
        val more = SessionApp("com.example.cards", "Cards", "1.0", 1, 0)
        prefs.setSession(session.including(listOf(more), now = 3_000L))

        prefs.updateSession(session.startedAt) { it.copy(observedAwake = it.observedAwake + "com.example.maps") }

        val stored = prefs.current.session!!
        assertEquals(listOf("com.example.maps", "com.example.cards"), stored.apps.map { it.packageName })
        assertEquals(3_000L, stored.lastWakeAt)
        assertEquals(setOf("com.example.maps"), stored.observedAwake)
    }

    @Test
    fun `does not touch a newer session`() {
        prefs.setSession(session)
        val newer = session.copy(startedAt = 5_000L, lastWakeAt = 5_000L)
        prefs.setSession(newer)

        prefs.updateSession(session.startedAt) { it.copy(observedAwake = setOf("com.example.maps")) }

        assertEquals(newer, prefs.current.session)
    }
}
