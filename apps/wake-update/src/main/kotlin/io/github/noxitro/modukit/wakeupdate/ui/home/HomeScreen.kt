package io.github.noxitro.modukit.wakeupdate.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.noxitro.modukit.wakeupdate.R
import io.github.noxitro.modukit.wakeupdate.domain.AppSource
import io.github.noxitro.modukit.wakeupdate.domain.SessionPhase
import io.github.noxitro.modukit.wakeupdate.domain.SessionStatus
import io.github.noxitro.modukit.wakeupdate.domain.SleepingApp
import io.github.noxitro.modukit.wakeupdate.domain.UpdatedApp
import io.github.noxitro.modukit.wakeupdate.domain.WakeResult
import io.github.noxitro.modukit.wakeupdate.ui.AppItem
import io.github.noxitro.modukit.wakeupdate.ui.HomeUiState
import io.github.noxitro.modukit.wakeupdate.ui.formatAge
import io.github.noxitro.modukit.wakeupdate.ui.icons.WakeIcons
import io.github.noxitro.modukit.wakeupdate.ui.theme.WakeTheme
import java.util.Date

data class HomeActions(
    val onToggle: (SleepingApp, Boolean) -> Unit = { _, _ -> },
    val onSetAll: (List<SleepingApp>, Boolean) -> Unit = { _, _ -> },
    val onWake: () -> Unit = {},
    val onWakeAgain: (List<String>) -> Unit = {},
    val onOpenPlayStore: () -> Unit = {},
    val onDismissSession: () -> Unit = {},
    val onOpenDeepSleepList: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
)

private const val ICON_STACK_SIZE = 6

@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis(),
    animate: Boolean = true,
) {
    val session = state.session
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            when {
                state.loading || !state.isGalaxy -> Unit
                session != null -> SessionBottomBar(session, actions)
                else -> IdleBottomBar(state, actions)
            }
        },
    ) { padding ->
        val direction = LocalLayoutDirection.current
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(direction) + 16.dp,
                end = padding.calculateEndPadding(direction) + 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item(key = "top") { TopBar(actions.onOpenSettings) }
            when {
                state.loading -> item(key = "sky") {
                    SkyPanel(
                        sky = Sky.NIGHT,
                        eyebrow = stringResource(R.string.sky_eyebrow_sleeping),
                        caption = stringResource(R.string.sky_caption_loading),
                        animate = animate,
                    ) { SkyTitle("…") }
                }

                !state.isGalaxy -> item(key = "sky") {
                    SkyPanel(
                        sky = Sky.NIGHT,
                        eyebrow = stringResource(R.string.sky_eyebrow_sleeping),
                        caption = stringResource(R.string.sky_caption_not_galaxy),
                        animate = animate,
                    ) { SkyTitle(stringResource(R.string.sky_title_not_galaxy)) }
                }

                session != null -> sessionContent(state, session, actions, animate)
                else -> idleContent(state, actions, now, animate)
            }
        }
    }
}

@Composable
private fun TopBar(onOpenSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- ディープスリープ中のアプリを選んで起こす ----

private fun LazyListScope.idleContent(state: HomeUiState, actions: HomeActions, now: Long, animate: Boolean) {
    item(key = "sky") {
        val all = state.playApps + state.otherApps
        if (all.isEmpty()) {
            SkyPanel(
                sky = Sky.NIGHT,
                eyebrow = stringResource(R.string.sky_eyebrow_sleeping),
                caption = stringResource(R.string.sky_caption_none),
                animate = animate,
            ) { SkyTitle(stringResource(R.string.sky_title_none)) }
        } else {
            SkyPanel(
                sky = Sky.NIGHT,
                eyebrow = stringResource(R.string.sky_eyebrow_sleeping),
                caption = stringResource(R.string.sky_caption_sleeping),
                iconPackages = all.take(ICON_STACK_SIZE).map { it.app.packageName },
                moreCount = all.size - ICON_STACK_SIZE,
                animate = animate,
            ) { SkyCount(all.size, pluralStringResource(R.plurals.sky_unit_apps, all.size)) }
        }
    }

    state.lastResult?.let { result ->
        item(key = "last") { LastResult(result, Modifier.padding(top = 12.dp)) }
    }

    if (state.playApps.isNotEmpty()) {
        item(key = "play-header") {
            val allSelected = state.playApps.all { it.selected }
            SectionHeader(
                title = stringResource(R.string.section_wake),
                detail = stringResource(
                    R.string.section_selected_of,
                    state.playApps.count { it.selected },
                    state.playApps.size,
                ),
                action = stringResource(if (allSelected) R.string.action_select_none else R.string.action_select_all),
                onAction = { actions.onSetAll(state.playApps.map { it.app }, !allSelected) },
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
        }
        appRows("play", state.playApps, now, actions)
    }

    if (state.otherApps.isNotEmpty()) {
        item(key = "other-header") { OtherAppsHeader(state, actions, now) }
    }
}

@Composable
private fun OtherAppsHeader(state: HomeUiState, actions: HomeActions, now: Long) {
    var expanded by rememberSaveable { mutableStateOf(state.otherApps.any { it.selected }) }
    Column(Modifier.padding(top = 20.dp)) {
        SectionHeader(
            title = stringResource(R.string.section_other),
            detail = state.otherApps.size.toString(),
            action = stringResource(if (expanded) R.string.action_hide else R.string.action_show),
            onAction = { expanded = !expanded },
        )
        Text(
            stringResource(R.string.section_other_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
        )
        if (expanded) {
            state.otherApps.forEachIndexed { index, item ->
                GroupedRow(index, state.otherApps.size) {
                    SelectableAppRow(item, now) { actions.onToggle(item.app, it) }
                }
            }
        }
    }
}

private fun LazyListScope.appRows(keyPrefix: String, items: List<AppItem>, now: Long, actions: HomeActions) {
    itemsIndexed(items, key = { _, item -> "$keyPrefix:${item.app.packageName}" }) { index, item ->
        GroupedRow(index, items.size) {
            SelectableAppRow(item, now) { actions.onToggle(item.app, it) }
        }
    }
}

@Composable
private fun SelectableAppRow(item: AppItem, now: Long, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = item.selected, role = Role.Checkbox, onValueChange = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(item.app.packageName, 40.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.app.label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                appMeta(item.app, now),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        RoundCheck(item.selected)
    }
}

/** 「v5.2.0 · 最終更新 4 か月前」。どれだけ更新されていないかが分かるようにする。 */
@Composable
private fun appMeta(app: SleepingApp, now: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val updated = stringResource(R.string.app_meta_updated, formatAge(app.lastUpdateTime, now, locale))
    val first = when (app.source) {
        AppSource.PLAY_STORE -> app.versionName?.let { "v$it" }
        AppSource.GALAXY_STORE -> stringResource(R.string.source_galaxy_store)
        AppSource.OTHER -> stringResource(R.string.source_other)
    }
    return listOfNotNull(first, updated).joinToString(" · ")
}

@Composable
private fun LastResult(result: WakeResult, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(result.finishedAt, locale) {
        DateFormat.format(DateFormat.getBestDateTimePattern(locale, "MMMd"), Date(result.finishedAt)).toString()
    }
    val summary = if (result.updated.isEmpty()) {
        pluralStringResource(R.plurals.last_result_woken, result.wokenCount, result.wokenCount)
    } else {
        stringResource(R.string.last_result_woken_updated, result.wokenCount, result.updated.size)
    }
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = WakeTheme.colors.success,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.last_result_label, date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (result.notBackToSleepCount > 0) {
                Text(
                    pluralStringResource(
                        R.plurals.last_result_not_back,
                        result.notBackToSleepCount,
                        result.notBackToSleepCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WakeTheme.colors.attention,
                )
            }
        }
    }
}

@Composable
private fun IdleBottomBar(state: HomeUiState, actions: HomeActions) {
    when {
        state.sleepingCount == 0 -> BottomActionBar(
            label = stringResource(R.string.action_open_play_store),
            onClick = actions.onOpenPlayStore,
            outlined = true,
        )

        state.selectedCount == 0 -> BottomActionBar(
            label = stringResource(R.string.action_choose_apps),
            onClick = {},
            enabled = false,
        )

        else -> BottomActionBar(
            label = pluralStringResource(R.plurals.action_wake, state.selectedCount, state.selectedCount),
            onClick = actions.onWake,
            icon = WakeIcons.Wake,
            helper = stringResource(R.string.action_wake_helper),
        )
    }
}

// ---- 起こしたあと ----

private fun LazyListScope.sessionContent(
    state: HomeUiState,
    session: SessionStatus,
    actions: HomeActions,
    animate: Boolean,
) {
    item(key = "sky") { SessionSky(state, session, animate) }

    when (session.phase) {
        SessionPhase.WAKING, SessionPhase.AWAKE -> {
            item(key = "steps") { NextSteps(Modifier.padding(top = 20.dp)) }
            if (session.notWoken.isNotEmpty()) {
                item(key = "not-woken") {
                    NoticeCard(
                        tone = Tone.ATTENTION,
                        icon = Icons.Rounded.Warning,
                        title = pluralStringResource(
                            R.plurals.notice_not_woken_title,
                            session.notWoken.size,
                            session.notWoken.size,
                        ),
                        body = stringResource(R.string.notice_not_woken_body),
                        modifier = Modifier.padding(top = 16.dp),
                        content = { AppNamesOf(session.notWoken.map { it.label }) },
                        actions = {
                            TextButton(onClick = { actions.onWakeAgain(session.notWoken.map { it.packageName }) }) {
                                Text(stringResource(R.string.action_wake_again))
                            }
                        },
                    )
                }
            }
        }

        SessionPhase.NOT_BACK_TO_SLEEP -> item(key = "not-back") {
            NoticeCard(
                tone = Tone.ATTENTION,
                icon = Icons.Rounded.Warning,
                title = pluralStringResource(
                    R.plurals.notice_not_back_title,
                    session.notBackToSleep.size,
                    session.notBackToSleep.size,
                ),
                body = stringResource(R.string.notice_not_back_body),
                modifier = Modifier.padding(top = 16.dp),
                content = { AppNamesOf(session.notBackToSleep.map { it.label }) },
                actions = {
                    TextButton(onClick = actions.onDismissSession) { Text(stringResource(R.string.action_dismiss)) }
                },
            )
        }

        SessionPhase.NONE_WOKE -> item(key = "none-woke") {
            NoticeCard(
                tone = Tone.ATTENTION,
                icon = Icons.Rounded.Warning,
                title = stringResource(R.string.notice_none_woke_title),
                body = stringResource(R.string.notice_none_woke_body),
                modifier = Modifier.padding(top = 16.dp),
                actions = {
                    TextButton(onClick = actions.onDismissSession) { Text(stringResource(R.string.action_dismiss)) }
                },
            )
        }

        SessionPhase.ALL_ASLEEP -> Unit
    }

    if (session.updated.isNotEmpty()) {
        item(key = "updated-header") {
            SectionHeader(
                title = stringResource(R.string.section_updated),
                detail = session.updated.size.toString(),
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
            )
        }
        itemsIndexed(session.updated, key = { _, app -> "updated:${app.packageName}" }) { index, app ->
            GroupedRow(index, session.updated.size) { UpdatedAppRow(app) }
        }
    }
}

@Composable
private fun SessionSky(state: HomeUiState, session: SessionStatus, animate: Boolean) {
    when (session.phase) {
        SessionPhase.WAKING -> SkyPanel(
            sky = Sky.DAWN,
            eyebrow = stringResource(R.string.sky_eyebrow_waking),
            caption = stringResource(R.string.sky_caption_waking),
            animate = animate,
        ) { SkyTitle(stringResource(R.string.sky_title_waking)) }

        SessionPhase.AWAKE -> SkyPanel(
            sky = Sky.DAWN,
            eyebrow = stringResource(R.string.sky_eyebrow_awake),
            caption = stringResource(R.string.sky_caption_awake),
            iconPackages = session.awake.take(ICON_STACK_SIZE).map { it.packageName },
            moreCount = session.awake.size - ICON_STACK_SIZE,
            animate = animate,
        ) { SkyCount(session.awake.size, pluralStringResource(R.plurals.sky_unit_apps, session.awake.size)) }

        SessionPhase.NOT_BACK_TO_SLEEP -> SkyPanel(
            sky = Sky.DAWN,
            eyebrow = stringResource(R.string.sky_eyebrow_still_awake),
            caption = stringResource(R.string.sky_caption_still_awake),
            iconPackages = session.notBackToSleep.take(ICON_STACK_SIZE).map { it.packageName },
            moreCount = session.notBackToSleep.size - ICON_STACK_SIZE,
            animate = animate,
        ) {
            SkyCount(
                session.notBackToSleep.size,
                pluralStringResource(R.plurals.sky_unit_apps, session.notBackToSleep.size),
            )
        }

        SessionPhase.NONE_WOKE, SessionPhase.ALL_ASLEEP -> {
            val all = state.playApps + state.otherApps
            SkyPanel(
                sky = Sky.NIGHT,
                eyebrow = stringResource(R.string.sky_eyebrow_sleeping),
                caption = stringResource(R.string.sky_caption_none_woke),
                iconPackages = all.take(ICON_STACK_SIZE).map { it.app.packageName },
                moreCount = all.size - ICON_STACK_SIZE,
                animate = animate,
            ) { SkyCount(all.size, pluralStringResource(R.plurals.sky_unit_apps, all.size)) }
        }
    }
}

@Composable
private fun NextSteps(modifier: Modifier = Modifier) {
    Column(modifier) {
        SectionHeader(title = stringResource(R.string.section_next), modifier = Modifier.padding(bottom = 4.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(WakeTheme.colors.listContainer, RoundedCornerShape(26.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            StepRow(1, stringResource(R.string.step_update_title), stringResource(R.string.step_update_body))
            StepRow(2, stringResource(R.string.step_screen_off_title), stringResource(R.string.step_screen_off_body))
        }
    }
}

@Composable
private fun UpdatedAppRow(app: UpdatedApp) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.packageName, 40.dp)
        Spacer(Modifier.width(16.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        val versions = listOfNotNull(app.fromVersion, app.toVersion)
        if (versions.size == 2 && app.fromVersion != app.toVersion) {
            Text(
                "${app.fromVersion} → ${app.toVersion}",
                style = MaterialTheme.typography.labelMedium,
                color = WakeTheme.colors.success,
                maxLines = 1,
            )
        } else {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.updated),
                tint = WakeTheme.colors.success,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AppNamesOf(names: List<String>) {
    val rest = (names.size - 4).coerceAtLeast(0)
    val moreText = if (rest > 0) pluralStringResource(R.plurals.names_more, rest, rest) else ""
    AppNames(names, moreFormat = { moreText })
}

@Composable
private fun SessionBottomBar(session: SessionStatus, actions: HomeActions) {
    when (session.phase) {
        SessionPhase.WAKING, SessionPhase.AWAKE, SessionPhase.ALL_ASLEEP -> BottomActionBar(
            label = stringResource(R.string.action_update_in_play_store),
            onClick = actions.onOpenPlayStore,
            helper = stringResource(R.string.action_update_helper),
        )

        SessionPhase.NOT_BACK_TO_SLEEP -> BottomActionBar(
            label = stringResource(R.string.action_open_deep_sleep_list),
            onClick = actions.onOpenDeepSleepList,
            icon = WakeIcons.Moon,
        )

        SessionPhase.NONE_WOKE -> BottomActionBar(
            label = stringResource(R.string.action_wake_again),
            onClick = { actions.onWakeAgain(session.notWoken.map { it.packageName }) },
            icon = WakeIcons.Wake,
            helper = stringResource(R.string.action_wake_helper),
        )
    }
}
