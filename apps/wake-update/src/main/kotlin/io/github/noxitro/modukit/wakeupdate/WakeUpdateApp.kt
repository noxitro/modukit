package io.github.noxitro.modukit.wakeupdate

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import io.github.noxitro.modukit.wakeupdate.data.InstalledApps
import io.github.noxitro.modukit.wakeupdate.data.Prefs
import io.github.noxitro.modukit.wakeupdate.domain.SessionApp
import io.github.noxitro.modukit.wakeupdate.domain.SessionEvaluator
import io.github.noxitro.modukit.wakeupdate.domain.SessionPhase
import io.github.noxitro.modukit.wakeupdate.domain.SleepingApp
import io.github.noxitro.modukit.wakeupdate.domain.WakeSession
import io.github.noxitro.modukit.wakeupdate.reminder.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WakeUpdateApp : Application() {
    lateinit var installedApps: InstalledApps
        private set
    lateinit var prefs: Prefs
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 画面オフでアプリはディープスリープに戻る。戻らなかったアプリを見分けるため、時刻を記録する
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (prefs.current.session != null) prefs.setScreenOffAt(System.currentTimeMillis())
        }
    }

    override fun onCreate() {
        super.onCreate()
        installedApps = InstalledApps(this)
        prefs = Prefs(this)
        Reminders.createChannel(this)
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    /**
     * 起こすアプリ。
     *
     * @param only 指定したときは、スリープ中のアプリのうちこのパッケージだけ（「もう一度起こす」用）。
     */
    fun wakeTargets(only: Collection<String>? = null): List<SleepingApp> {
        val sleeping = installedApps.sleepingApps()
        return if (only != null) sleeping.filter { it.packageName in only }
        else prefs.current.selection.targets(sleeping)
    }

    /** 起こす直前のバージョンを記録する。続けて起こしたときは、同じセッションにまとめる。 */
    fun beginSession(targets: List<SleepingApp>, now: Long = System.currentTimeMillis()) {
        val snapshot = targets.map {
            SessionApp(it.packageName, it.label, it.versionName, it.versionCode, it.lastUpdateTime)
        }
        val current = prefs.current.session
        val status = current?.let {
            SessionEvaluator.evaluate(
                it,
                installedApps.states(it.apps.map(SessionApp::packageName)),
                now,
                prefs.current.screenOffAt,
            )
        }
        val session = when {
            current == null || status == null -> WakeSession(startedAt = now, apps = snapshot)
            status.phase in CONTINUING -> current.including(snapshot, now)
            else -> {
                if (status.wokenCount > 0) prefs.finishSession(status.toResult(now))
                WakeSession(startedAt = now, apps = snapshot)
            }
        }
        prefs.setSession(session)
    }

    /** 起こした直後に何度か状態を確かめて、起きたアプリを記録する。 */
    fun checkSoon() {
        scope.launch {
            delay(2_500)
            runCatching { recordAwake(markChecked = false) }
            delay(5_000)
            runCatching { recordAwake(markChecked = true) }
        }
    }

    private fun recordAwake(markChecked: Boolean) {
        val session = prefs.current.session ?: return
        val awake = installedApps.states(session.apps.map(SessionApp::packageName))
            .filterValues { it.enabled }
            .keys
        prefs.setSession(
            session.copy(
                observedAwake = session.observedAwake + awake,
                checkedAt = if (markChecked) System.currentTimeMillis() else session.checkedAt,
            ),
        )
    }

    private companion object {
        val CONTINUING = setOf(SessionPhase.WAKING, SessionPhase.AWAKE, SessionPhase.NONE_WOKE)
    }
}

val Context.wakeUpdateApp: WakeUpdateApp
    get() = applicationContext as WakeUpdateApp
