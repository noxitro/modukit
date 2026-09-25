package io.github.noxitro.modukit.wakeupdate.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.noxitro.modukit.wakeupdate.reminder.Reminders
import io.github.noxitro.modukit.wakeupdate.system.GalaxySettings
import io.github.noxitro.modukit.wakeupdate.ui.home.HomeActions
import io.github.noxitro.modukit.wakeupdate.ui.home.HomeScreen
import io.github.noxitro.modukit.wakeupdate.ui.home.LocalAppIconLoader
import io.github.noxitro.modukit.wakeupdate.ui.home.PackageIconLoader
import io.github.noxitro.modukit.wakeupdate.ui.onboarding.OnboardingContent
import io.github.noxitro.modukit.wakeupdate.ui.settings.SettingsActions
import io.github.noxitro.modukit.wakeupdate.ui.settings.SettingsContent
import io.github.noxitro.modukit.wakeupdate.ui.theme.WakeTheme
import io.github.noxitro.modukit.wakeupdate.wake.WakeActivity
import io.github.noxitro.modukit.wakeupdate.wake.WakeLauncher
import io.github.noxitro.modukit.wakeupdate.wakeUpdateApp

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels { HomeViewModel.factory(wakeUpdateApp) }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val iconLoader = PackageIconLoader(wakeUpdateApp.installedApps)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

        setContent {
            WakeTheme {
                CompositionLocalProvider(LocalAppIconLoader provides iconLoader) {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    var showSettings by rememberSaveable { mutableStateOf(false) }
                    var showHowTo by rememberSaveable { mutableStateOf(false) }
                    var canNotify by remember { mutableStateOf(Reminders.canNotify(this)) }

                    LifecycleResumeEffect(Unit) {
                        viewModel.refresh()
                        canNotify = Reminders.canNotify(this@MainActivity)
                        onPauseOrDispose { }
                    }

                    val notificationPermission = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission(),
                    ) { granted -> canNotify = granted }
                    val askNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    HomeScreen(
                        state = state,
                        actions = HomeActions(
                            onToggle = viewModel::setSelected,
                            onSetAll = viewModel::setAllSelected,
                            onWake = { startActivity(WakeActivity.intent(this)) },
                            onWakeAgain = { startActivity(WakeActivity.intent(this, only = it)) },
                            onOpenPlayStore = { WakeLauncher.openPlayStoreUpdates(this) },
                            onDismissSession = viewModel::dismissSession,
                            onOpenDeepSleepList = {
                                GalaxySettings.open(this, GalaxySettings.SleepList.DEEP_SLEEPING)
                            },
                            onOpenSettings = { showSettings = true },
                        ),
                    )

                    if (showSettings) {
                        ModalBottomSheet(
                            onDismissRequest = { showSettings = false },
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            containerColor = MaterialTheme.colorScheme.background,
                        ) {
                            SettingsContent(
                                reminderDays = state.reminderDays,
                                canNotify = canNotify,
                                versionName = versionName,
                                actions = SettingsActions(
                                    onReminderDays = { days ->
                                        viewModel.setReminderDays(days)
                                        if (days > 0 && !canNotify) askNotifications()
                                    },
                                    onAllowNotifications = { openNotificationSettings() },
                                    onKeepAwake = {
                                        GalaxySettings.open(this@MainActivity, GalaxySettings.SleepList.NEVER_SLEEPING)
                                    },
                                    onOpenDeepSleepList = {
                                        GalaxySettings.open(this@MainActivity, GalaxySettings.SleepList.DEEP_SLEEPING)
                                    },
                                    onShowHowTo = {
                                        showSettings = false
                                        showHowTo = true
                                    },
                                ),
                            )
                        }
                    }

                    if (state.showOnboarding || showHowTo) {
                        ModalBottomSheet(
                            onDismissRequest = {
                                showHowTo = false
                                viewModel.completeOnboarding()
                            },
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            containerColor = MaterialTheme.colorScheme.background,
                        ) {
                            OnboardingContent(
                                onStart = {
                                    showHowTo = false
                                    viewModel.completeOnboarding()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        runCatching { startActivity(intent) }.onFailure {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        }
    }
}
