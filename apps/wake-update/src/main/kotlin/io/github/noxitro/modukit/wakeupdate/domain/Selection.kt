package io.github.noxitro.modukit.wakeupdate.domain

/**
 * どのアプリを起こすか。
 *
 * Play ストアのアプリは既定で対象、それ以外（Play ストアでは更新されない）は既定で対象外にして、
 * ユーザーが既定から変えたものだけを保存する。こうすると新しくスリープしたアプリも既定どおりに扱える。
 */
data class Selection(
    val excluded: Set<String> = emptySet(),
    val included: Set<String> = emptySet(),
) {
    fun isSelected(app: SleepingApp): Boolean =
        if (app.source == AppSource.PLAY_STORE) app.packageName !in excluded
        else app.packageName in included

    fun with(app: SleepingApp, selected: Boolean): Selection =
        if (app.source == AppSource.PLAY_STORE) {
            copy(excluded = if (selected) excluded - app.packageName else excluded + app.packageName)
        } else {
            copy(included = if (selected) included + app.packageName else included - app.packageName)
        }

    fun withAll(apps: Collection<SleepingApp>, selected: Boolean): Selection =
        apps.fold(this) { acc, app -> acc.with(app, selected) }

    fun targets(apps: Collection<SleepingApp>): List<SleepingApp> = apps.filter(::isSelected)
}
