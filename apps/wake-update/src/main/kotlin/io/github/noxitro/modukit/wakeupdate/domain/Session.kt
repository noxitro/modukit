package io.github.noxitro.modukit.wakeupdate.domain

import kotlinx.serialization.Serializable

/** 起こす直前のアプリの状態。更新されたかどうかをこの値と比べて判定する。 */
@Serializable
data class SessionApp(
    val packageName: String,
    val label: String,
    val versionName: String? = null,
    val versionCode: Long,
    val lastUpdateTime: Long,
)

/**
 * アプリを起こしてから、画面オフでまたスリープに戻るまでの 1 回分の記録。
 *
 * @property observedAwake 一度でも起きているのを確認できたアプリ。起こせなかったアプリの判定に使う。
 * @property checkedAt 起こした直後の確認（WakeUpdateApp.checkSoon）が済んだ時刻。
 */
@Serializable
data class WakeSession(
    val startedAt: Long,
    val lastWakeAt: Long = startedAt,
    val apps: List<SessionApp>,
    val observedAwake: Set<String> = emptySet(),
    val checkedAt: Long? = null,
) {
    /** 「もう一度起こす」で起こしたアプリを加える。更新の判定のため、元からあるアプリの記録は残す。 */
    fun including(more: List<SessionApp>, now: Long): WakeSession {
        val known = apps.mapTo(HashSet()) { it.packageName }
        return copy(
            lastWakeAt = now,
            apps = apps + more.filter { it.packageName !in known },
            checkedAt = null,
        )
    }
}

@Serializable
data class UpdatedApp(
    val packageName: String,
    val label: String,
    val fromVersion: String? = null,
    val toVersion: String? = null,
)

/** 終わったセッションの結果。次に開いたときに「前回」として表示する。 */
@Serializable
data class WakeResult(
    val finishedAt: Long,
    val wokenCount: Int,
    val updated: List<UpdatedApp>,
    val notBackToSleepCount: Int = 0,
)

enum class SessionPhase {
    /** 起こした直後で、まだどのアプリも起きたことを確認できていない */
    WAKING,

    /** アプリが起きている。Play ストアで更新できる */
    AWAKE,

    /** 画面をオフにしたのに起きたままのアプリがある（ディープスリープの一覧から外れた可能性） */
    NOT_BACK_TO_SLEEP,

    /** 起きたアプリがすべてスリープに戻った。セッションは終わり */
    ALL_ASLEEP,

    /** 1 つも起こせなかった */
    NONE_WOKE,
}

data class SessionStatus(
    val phase: SessionPhase,
    val awake: List<SessionApp>,
    val notBackToSleep: List<SessionApp>,
    val notWoken: List<SessionApp>,
    val updated: List<UpdatedApp>,
    val observedAwake: Set<String>,
    val wokenCount: Int,
) {
    fun toResult(now: Long) = WakeResult(
        finishedAt = now,
        wokenCount = wokenCount,
        updated = updated,
        notBackToSleepCount = notBackToSleep.size,
    )
}

object SessionEvaluator {
    /** 起こした直後はまだ起きていないことがあるので、この時間までは「起こせなかった」と判定しない。 */
    const val WAKE_GRACE_MS = 15_000L

    /** 画面オフを検知できなかったとき、スリープに戻っているはずとみなすまでの時間。 */
    const val STALE_MS = 6 * 60 * 60 * 1000L

    /**
     * @param states 今のアプリの状態。アンインストールされたアプリは含まれないので、結果からも外れる。
     * @param screenOffAt 最後に画面がオフになった時刻（検知できていれば）。
     */
    fun evaluate(
        session: WakeSession,
        states: Map<String, AppState>,
        now: Long,
        screenOffAt: Long?,
    ): SessionStatus {
        val present = session.apps.filter { it.packageName in states }
        val awakeNow = present.filter { states.getValue(it.packageName).enabled }
        val updated = present.mapNotNull { app ->
            val state = states.getValue(app.packageName)
            val isNewer = state.versionCode > app.versionCode || state.lastUpdateTime > app.lastUpdateTime
            if (isNewer) UpdatedApp(app.packageName, app.label, app.versionName, state.versionName) else null
        }
        // 更新できたアプリは、その時点で起きていたことになる
        val observed = session.observedAwake + awakeNow.map { it.packageName } + updated.map { it.packageName }
        val woken = present.count { it.packageName in observed }

        val screenWentOff = screenOffAt != null && screenOffAt > session.lastWakeAt
        val stale = now - session.lastWakeAt > STALE_MS
        val notBack = if (screenWentOff || stale) awakeNow else emptyList()
        val checked = session.checkedAt != null || now - session.lastWakeAt > WAKE_GRACE_MS
        val notWoken = if (checked) present.filter { it.packageName !in observed } else emptyList()

        val phase = when {
            notBack.isNotEmpty() -> SessionPhase.NOT_BACK_TO_SLEEP
            awakeNow.isNotEmpty() -> SessionPhase.AWAKE
            woken > 0 -> SessionPhase.ALL_ASLEEP
            !checked -> SessionPhase.WAKING
            else -> SessionPhase.NONE_WOKE
        }
        return SessionStatus(
            phase = phase,
            awake = if (notBack.isEmpty()) awakeNow else emptyList(),
            notBackToSleep = notBack,
            notWoken = notWoken,
            updated = updated,
            observedAwake = observed,
            wokenCount = woken,
        )
    }
}
