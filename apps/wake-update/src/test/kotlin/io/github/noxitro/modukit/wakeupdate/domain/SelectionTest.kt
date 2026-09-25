package io.github.noxitro.modukit.wakeupdate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionTest {
    private val play = app("com.example.play", AppSource.PLAY_STORE)
    private val galaxy = app("com.example.galaxy", AppSource.GALAXY_STORE)
    private val other = app("com.example.other", AppSource.OTHER)

    @Test
    fun `Play apps are selected by default and others are not`() {
        val selection = Selection()
        assertTrue(selection.isSelected(play))
        assertFalse(selection.isSelected(galaxy))
        assertFalse(selection.isSelected(other))
    }

    @Test
    fun `toggling stores only the difference from the default`() {
        val selection = Selection()
            .with(play, selected = false)
            .with(galaxy, selected = true)
        assertEquals(setOf(play.packageName), selection.excluded)
        assertEquals(setOf(galaxy.packageName), selection.included)

        val back = selection.with(play, selected = true).with(galaxy, selected = false)
        assertEquals(Selection(), back)
    }

    @Test
    fun `withAll and targets`() {
        val all = listOf(play, galaxy, other)
        assertEquals(listOf(play), Selection().targets(all))
        assertEquals(all, Selection().withAll(all, selected = true).targets(all))
        assertEquals(emptyList<SleepingApp>(), Selection().withAll(all, selected = false).targets(all))
    }

    private fun app(pkg: String, source: AppSource) = SleepingApp(
        packageName = pkg,
        label = pkg,
        source = source,
        versionName = "1.0",
        versionCode = 1,
        lastUpdateTime = 0,
    )
}
