package io.github.noxitro.modukit.wakeupdate.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.noxitro.modukit.wakeupdate.domain.Selection
import io.github.noxitro.modukit.wakeupdate.domain.WakeResult
import io.github.noxitro.modukit.wakeupdate.domain.WakeSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * 設定と、起こしたアプリの記録。
 *
 * タイルやショートカットから起動したときにすぐ読めるよう、同期で読める SharedPreferences を使う。
 */
class Prefs(context: Context) {
    data class Snapshot(
        val selection: Selection = Selection(),
        val onboardingDone: Boolean = false,
        val reminderDays: Int = 0,
        val session: WakeSession? = null,
        val lastResult: WakeResult? = null,
        val screenOffAt: Long? = null,
    )

    private val sp: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(read())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    // SharedPreferences は listener を弱参照で持つので、フィールドで保持する
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> _state.value = read() }

    init {
        sp.registerOnSharedPreferenceChangeListener(listener)
    }

    val current: Snapshot get() = _state.value

    fun setSelection(selection: Selection) = sp.edit {
        putStringSet(KEY_EXCLUDED, HashSet(selection.excluded))
        putStringSet(KEY_INCLUDED, HashSet(selection.included))
    }

    fun setOnboardingDone() = sp.edit { putBoolean(KEY_ONBOARDING_DONE, true) }

    fun setReminderDays(days: Int) = sp.edit { putInt(KEY_REMINDER_DAYS, days) }

    fun setSession(session: WakeSession) = sp.edit(commit = true) {
        putString(KEY_SESSION, json.encodeToString(WakeSession.serializer(), session))
    }

    fun clearSession() = sp.edit { remove(KEY_SESSION) }

    fun finishSession(result: WakeResult) = sp.edit {
        remove(KEY_SESSION)
        putString(KEY_LAST_RESULT, json.encodeToString(WakeResult.serializer(), result))
    }

    fun setScreenOffAt(time: Long) = sp.edit { putLong(KEY_SCREEN_OFF_AT, time) }

    private fun read() = Snapshot(
        selection = Selection(
            excluded = sp.getStringSet(KEY_EXCLUDED, null).orEmpty().toSet(),
            included = sp.getStringSet(KEY_INCLUDED, null).orEmpty().toSet(),
        ),
        onboardingDone = sp.getBoolean(KEY_ONBOARDING_DONE, false),
        reminderDays = sp.getInt(KEY_REMINDER_DAYS, 0),
        session = sp.getString(KEY_SESSION, null)?.let { decode(it, WakeSession.serializer()) },
        lastResult = sp.getString(KEY_LAST_RESULT, null)?.let { decode(it, WakeResult.serializer()) },
        screenOffAt = sp.getLong(KEY_SCREEN_OFF_AT, 0L).takeIf { it > 0L },
    )

    private fun <T> decode(text: String, serializer: kotlinx.serialization.KSerializer<T>): T? =
        runCatching { json.decodeFromString(serializer, text) }.getOrNull()

    private companion object {
        const val FILE = "wake_update"
        const val KEY_EXCLUDED = "excluded"
        const val KEY_INCLUDED = "included"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_REMINDER_DAYS = "reminder_days"
        const val KEY_SESSION = "session"
        const val KEY_LAST_RESULT = "last_result"
        const val KEY_SCREEN_OFF_AT = "screen_off_at"
    }
}
