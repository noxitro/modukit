package io.github.noxitro.modukit.wakeupdate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.noxitro.modukit.wakeupdate.WakeUpdateApp
import io.github.noxitro.modukit.wakeupdate.data.Prefs
import io.github.noxitro.modukit.wakeupdate.domain.AppSource
import io.github.noxitro.modukit.wakeupdate.domain.SessionApp
import io.github.noxitro.modukit.wakeupdate.domain.SessionEvaluator
import io.github.noxitro.modukit.wakeupdate.domain.SessionPhase
import io.github.noxitro.modukit.wakeupdate.domain.SessionStatus
import io.github.noxitro.modukit.wakeupdate.domain.SleepingApp
import io.github.noxitro.modukit.wakeupdate.domain.WakeResult
import io.github.noxitro.modukit.wakeupdate.reminder.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppItem(val app: SleepingApp, val selected: Boolean)

data class HomeUiState(
    val loading: Boolean = true,
    val isGalaxy: Boolean = true,
    /** Play ストアから入れた、ディープスリープ中のアプリ */
    val playApps: List<AppItem> = emptyList(),
    /** Play ストア以外から入れた、ディープスリープ中のアプリ */
    val otherApps: List<AppItem> = emptyList(),
    val session: SessionStatus? = null,
    val lastResult: WakeResult? = null,
    val reminderDays: Int = 0,
    val showOnboarding: Boolean = false,
) {
    val sleepingCount: Int get() = playApps.size + otherApps.size
    val selectedCount: Int get() = playApps.count { it.selected } + otherApps.count { it.selected }
}

class HomeViewModel(private val app: WakeUpdateApp) : ViewModel() {
    private val prefs: Prefs = app.prefs

    private data class DeviceState(
        val isGalaxy: Boolean,
        val sleeping: List<SleepingApp>,
        val session: SessionStatus?,
    )

    private val device = MutableStateFlow<DeviceState?>(null)

    val state: StateFlow<HomeUiState> = combine(device, prefs.state, ::toUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState(showOnboarding = !prefs.current.onboardingDone))

    /** 画面に戻るたびに呼ぶ。アプリの状態は Play ストアや画面オフで変わるので、毎回読み直す。 */
    fun refresh() {
        viewModelScope.launch {
            device.value = withContext(Dispatchers.IO) { load() }
        }
    }

    fun setSelected(app: SleepingApp, selected: Boolean) {
        prefs.setSelection(prefs.current.selection.with(app, selected))
    }

    fun setAllSelected(apps: List<SleepingApp>, selected: Boolean) {
        prefs.setSelection(prefs.current.selection.withAll(apps, selected))
    }

    /** セッションの表示を閉じる。起こせたアプリがあれば「前回」として残す。 */
    fun dismissSession() {
        val status = device.value?.session
        if (status != null && status.wokenCount > 0) {
            prefs.finishSession(status.toResult(System.currentTimeMillis()))
        } else {
            prefs.clearSession()
        }
    }

    fun setReminderDays(days: Int) {
        prefs.setReminderDays(days)
        Reminders.schedule(app, days)
    }

    fun completeOnboarding() = prefs.setOnboardingDone()

    private fun load(): DeviceState {
        val installed = app.installedApps
        val snapshot = prefs.current
        val now = System.currentTimeMillis()
        val session = snapshot.session
        var status: SessionStatus? = null
        if (session != null) {
            val evaluated = SessionEvaluator.evaluate(
                session,
                installed.states(session.apps.map(SessionApp::packageName)),
                now,
                snapshot.screenOffAt,
            )
            if (evaluated.phase == SessionPhase.ALL_ASLEEP) {
                prefs.finishSession(evaluated.toResult(now))
            } else {
                if (evaluated.observedAwake != session.observedAwake) {
                    prefs.setSession(session.copy(observedAwake = evaluated.observedAwake))
                }
                status = evaluated
            }
        }
        return DeviceState(installed.isGalaxy, installed.sleepingApps(), status)
    }

    private fun toUiState(device: DeviceState?, prefs: Prefs.Snapshot): HomeUiState {
        if (device == null) return HomeUiState(showOnboarding = !prefs.onboardingDone)
        val items = device.sleeping.map { AppItem(it, prefs.selection.isSelected(it)) }
        val (play, other) = items.partition { it.app.source == AppSource.PLAY_STORE }
        return HomeUiState(
            loading = false,
            isGalaxy = device.isGalaxy,
            playApps = play,
            otherApps = other,
            session = device.session.takeIf { prefs.session != null },
            lastResult = prefs.lastResult,
            reminderDays = prefs.reminderDays,
            showOnboarding = !prefs.onboardingDone,
        )
    }

    companion object {
        fun factory(app: WakeUpdateApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(app) }
        }
    }
}
