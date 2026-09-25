package io.github.noxitro.modukit.wakeupdate.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class FormatTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_790_000_000_000L

    private fun ja(days: Int) = formatAge(now - days * day, now, Locale.JAPAN).replace(" ", "")
    private fun en(days: Int) = formatAge(now - days * day, now, Locale.US)

    @Test
    fun `picks a unit that fits the age`() {
        assertEquals("今日", ja(0))
        assertEquals("3日前", ja(3))
        assertEquals("2週間前", ja(20))
        assertEquals("4か月前", ja(125))
        assertEquals("1年前", ja(400))
    }

    @Test
    fun `english uses the same thresholds`() {
        assertEquals("today", en(0))
        assertEquals("3 days ago", en(3))
        assertEquals("2 weeks ago", en(20))
        assertEquals("4 months ago", en(125))
        assertEquals("1 year ago", en(400))
    }
}
