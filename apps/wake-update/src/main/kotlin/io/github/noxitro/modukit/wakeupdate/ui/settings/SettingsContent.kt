package io.github.noxitro.modukit.wakeupdate.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.noxitro.modukit.wakeupdate.R
import io.github.noxitro.modukit.wakeupdate.reminder.Reminders
import io.github.noxitro.modukit.wakeupdate.ui.home.GroupedRow
import io.github.noxitro.modukit.wakeupdate.ui.icons.WakeIcons
import io.github.noxitro.modukit.wakeupdate.ui.theme.WakeTheme

data class SettingsActions(
    val onReminderDays: (Int) -> Unit = {},
    val onAllowNotifications: () -> Unit = {},
    val onKeepAwake: () -> Unit = {},
    val onOpenDeepSleepList: () -> Unit = {},
    val onShowHowTo: () -> Unit = {},
)

@Composable
fun SettingsContent(
    reminderDays: Int,
    canNotify: Boolean,
    versionName: String,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp).semantics { heading() },
        )

        Header(stringResource(R.string.settings_reminder))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        ) {
            Text(
                stringResource(R.string.settings_reminder_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
            )
            val labels = listOf(
                R.string.reminder_off,
                R.string.reminder_weekly,
                R.string.reminder_biweekly,
                R.string.reminder_monthly,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Reminders.INTERVALS.forEachIndexed { index, days ->
                    SegmentedButton(
                        selected = reminderDays == days,
                        onClick = { actions.onReminderDays(days) },
                        shape = SegmentedButtonDefaults.itemShape(index, Reminders.INTERVALS.size),
                        icon = {},
                        label = { Text(stringResource(labels[index]), maxLines = 1) },
                    )
                }
            }
            if (reminderDays > 0 && !canNotify) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.settings_notifications_off),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WakeTheme.colors.attention,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = actions.onAllowNotifications) {
                        Text(stringResource(R.string.settings_allow_notifications))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        GroupedRow(0, 1) {
            LinkRow(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.settings_keep_awake),
                body = stringResource(R.string.settings_keep_awake_body),
                onClick = actions.onKeepAwake,
            )
        }

        Header(stringResource(R.string.settings_galaxy))
        GroupedRow(0, 1) {
            LinkRow(
                icon = WakeIcons.Moon,
                title = stringResource(R.string.settings_deep_sleep_list),
                body = stringResource(R.string.settings_deep_sleep_list_body),
                onClick = actions.onOpenDeepSleepList,
            )
        }

        Header(stringResource(R.string.settings_about))
        GroupedRow(0, 1) {
            LinkRow(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.settings_how_to),
                body = stringResource(R.string.settings_how_to_body),
                onClick = actions.onShowHowTo,
            )
        }
        Text(
            stringResource(R.string.app_name) + " · " + stringResource(R.string.settings_version, versionName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 8.dp, top = 16.dp),
        )
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp).semantics { heading() },
    )
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}
