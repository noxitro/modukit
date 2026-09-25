package io.github.noxitro.modukit.wakeupdate.wake

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.noxitro.modukit.wakeupdate.WakeUpdateApp
import io.github.noxitro.modukit.wakeupdate.domain.AppSource
import io.github.noxitro.modukit.wakeupdate.domain.Selection
import io.github.noxitro.modukit.wakeupdate.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowApplicationPackageManager

/**
 * Galaxy では、ディープスリープ中（無効化状態）のアプリでもランチャーの Activity を解決できる。
 * AOSP と同じ動きをする Robolectric に、その違いだけを足す。
 */
@Implements(className = "android.app.ApplicationPackageManager", isInAndroidSdk = false)
class ShadowGalaxyPackageManager : ShadowApplicationPackageManager() {
    @Implementation
    override fun getLaunchIntentForPackage(packageName: String): Intent? =
        super.getLaunchIntentForPackage(packageName) ?: sleepingLaunchers[packageName]?.let {
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(packageName, it)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    companion object {
        val sleepingLaunchers = mutableMapOf<String, String>()
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], shadows = [ShadowGalaxyPackageManager::class])
class WakeActivityTest {
    private val app = ApplicationProvider.getApplicationContext<WakeUpdateApp>()
    private val pm = shadowOf(app.packageManager)

    init {
        ShadowGalaxyPackageManager.sleepingLaunchers.clear()
    }

    /**
     * Galaxy のディープスリープを再現する。アプリ自体は無効化状態だが、
     * ランチャーの Activity は起動できる（起動すると一時的に起きる）。
     */
    private fun install(name: String, installer: String?, sleeping: Boolean = true) {
        val appInfo = ApplicationInfo().apply {
            packageName = name
            nonLocalizedLabel = name.substringAfterLast('.')
            enabled = !sleeping
        }
        val launcher = ActivityInfo().apply {
            packageName = name
            this.name = "$name.Main"
            exported = true
        }
        if (sleeping) ShadowGalaxyPackageManager.sleepingLaunchers[name] = launcher.name
        pm.installPackage(
            PackageInfo().apply {
                packageName = name
                applicationInfo = appInfo
                versionName = "1.0"
                longVersionCode = 1
            },
        )
        pm.addOrUpdateActivity(launcher)
        pm.addIntentFilterForActivity(
            ComponentName(name, launcher.name),
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        )
        if (installer != null) pm.setInstallSourceInfo(name, installer, installer)
    }

    /** 起動された順に並べる（nextStartedActivity は新しいものから返す）。 */
    private fun startedFrom(activity: WakeActivity): List<String> =
        generateSequence { shadowOf(activity).nextStartedActivity }
            .map { it.component?.let { c -> "${c.packageName}/${c.className}" } ?: "${it.`package`} ${it.action}" }
            .toList()
            .reversed()

    @Test
    fun `wakes the selected sleeping apps, returns to this app, then opens the Play Store`() {
        install("com.example.maps", AppSource.PLAY_STORE_PACKAGE)
        install("com.example.cards", AppSource.PLAY_STORE_PACKAGE)
        install("com.example.theme", AppSource.GALAXY_STORE_PACKAGE)
        install("com.example.awake", AppSource.PLAY_STORE_PACKAGE, sleeping = false)
        app.prefs.setSelection(Selection(excluded = setOf("com.example.cards")))

        val activity = Robolectric.buildActivity(WakeActivity::class.java).setup().get()

        assertEquals(
            listOf(
                "com.example.maps/com.example.maps.Main",
                "${app.packageName}/${MainActivity::class.java.name}",
                "com.android.vending com.google.android.finsky.VIEW_MY_DOWNLOADS",
            ),
            startedFrom(activity),
        )
        assertTrue(activity.isFinishing)
        assertEquals(listOf("com.example.maps"), app.prefs.current.session?.apps?.map { it.packageName })
    }

    @Test
    fun `wake again only wakes the given apps and keeps the session`() {
        install("com.example.maps", AppSource.PLAY_STORE_PACKAGE)
        install("com.example.cards", AppSource.PLAY_STORE_PACKAGE)
        Robolectric.buildActivity(WakeActivity::class.java).setup()

        val retry = Robolectric.buildActivity(
            WakeActivity::class.java,
            WakeActivity.intent(app, only = listOf("com.example.cards")),
        ).setup().get()

        assertEquals("com.example.cards/com.example.cards.Main", startedFrom(retry).first())
        assertEquals(
            listOf("com.example.cards", "com.example.maps"),
            app.prefs.current.session?.apps?.map { it.packageName }?.sorted(),
        )
    }

    @Test
    fun `does nothing when no app is asleep`() {
        install("com.example.awake", AppSource.PLAY_STORE_PACKAGE, sleeping = false)

        val activity = Robolectric.buildActivity(WakeActivity::class.java).setup().get()

        assertEquals(emptyList<String>(), startedFrom(activity))
        assertNull(app.prefs.current.session)
        assertTrue(activity.isFinishing)
    }
}
