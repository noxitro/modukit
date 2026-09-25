package io.github.noxitro.modukit.wakeupdate.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.noxitro.modukit.wakeupdate.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * nox-apk-manager の配布フォルダに置く icon.png（512px）を、ランチャーアイコンと同じ素材から描く。
 * manager は角丸に切り抜いて表示するので、端まで塗った正方形にする。
 *
 * ./gradlew :apps:wake-update:recordRoborazziDebug で distribution/icon.png が作り直される。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w640dp-h640dp-mdpi")
class DistributionIconTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun icon() {
        compose.setContent {
            Box(
                Modifier
                    .testTag("icon")
                    .size(ICON_PX.dp)
                    .clipToBounds()
                    .background(colorResource(R.color.night)),
                contentAlignment = Alignment.Center,
            ) {
                // アダプティブアイコンは 108dp のうち中央 72dp が見える範囲
                Image(
                    painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.requiredSize((ICON_PX * 108 / 72).dp),
                )
            }
        }
        compose.onNodeWithTag("icon").captureRoboImage("distribution/icon.png")
    }

    private companion object {
        // mdpi（1dp = 1px）で描く
        const val ICON_PX = 512
    }
}
