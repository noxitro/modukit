package io.github.noxitro.modukit.wakeupdate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.noxitro.modukit.wakeupdate.ui.home.HomeActions
import io.github.noxitro.modukit.wakeupdate.ui.home.HomeScreen
import io.github.noxitro.modukit.wakeupdate.ui.home.LocalAppIconLoader
import io.github.noxitro.modukit.wakeupdate.ui.onboarding.OnboardingContent
import io.github.noxitro.modukit.wakeupdate.ui.settings.SettingsActions
import io.github.noxitro.modukit.wakeupdate.ui.settings.SettingsContent
import io.github.noxitro.modukit.wakeupdate.ui.theme.WakeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 主な画面の見た目を PNG にする（./gradlew :apps:wake-update:recordRoborazziDebug）。
 * Galaxy S25 の既定の表示サイズ（幅 384dp）で描く。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "ja-rJP-w384dp-h832dp-450dpi")
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            WakeTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalAppIconLoader provides Samples.icons) { content() }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    @Composable
    private fun Home(state: HomeUiState) =
        HomeScreen(state, HomeActions(), now = Samples.NOW, animate = false)

    @Test fun idle() = capture("1_idle") { Home(Samples.idle) }

    @Test fun idleDark() = capture("1_idle_dark", dark = true) { Home(Samples.idle) }


    @Test fun awake() = capture("2_awake") { Home(Samples.awake) }

    @Test fun awakeDark() = capture("2_awake_dark", dark = true) { Home(Samples.awake) }

    @Test fun notBackToSleep() = capture("3_not_back_to_sleep") { Home(Samples.notBack) }

    @Test fun noneWoke() = capture("4_none_woke") { Home(Samples.noneWoke) }

    @Test fun empty() = capture("5_empty") { Home(Samples.empty) }

    @Test fun notGalaxy() = capture("6_not_galaxy") { Home(Samples.notGalaxy) }

    @Test fun onboarding() = capture("7_onboarding") {
        Sheet(Samples.idle) { OnboardingContent(onStart = {}) }
    }

    @Test fun settings() = capture("8_settings") {
        Sheet(Samples.idle) {
            SettingsContent(reminderDays = 14, canNotify = true, versionName = "1.0.0", actions = SettingsActions())
        }
    }

    /** ボトムシートを開いた状態の見た目（実際の ModalBottomSheet は別ウィンドウなので、同じ形で重ねる）。 */
    @Composable
    private fun Sheet(state: HomeUiState, content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize()) {
            Home(state)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .padding(vertical = 16.dp)
                            .size(width = 32.dp, height = 4.dp)
                            .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
                    )
                    content()
                }
            }
        }
    }
}
