package io.github.noxitro.modukit.wakeupdate.ui.home

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import io.github.noxitro.modukit.wakeupdate.data.InstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface AppIconLoader {
    suspend fun load(packageName: String, sizePx: Int): ImageBitmap?
}

val LocalAppIconLoader = staticCompositionLocalOf { AppIconLoader { _, _ -> null } }

/** 端末のアプリのアイコンを読み込む。一覧をスクロールしても読み直さないようにキャッシュする。 */
class PackageIconLoader(private val apps: InstalledApps) : AppIconLoader {
    private val cache = LruCache<String, ImageBitmap>(160)

    override suspend fun load(packageName: String, sizePx: Int): ImageBitmap? {
        val key = "$packageName@$sizePx"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            apps.icon(packageName)?.toBitmap(sizePx, sizePx)?.asImageBitmap()
        }?.also { cache.put(key, it) }
    }
}

@Composable
fun AppIcon(packageName: String, size: Dp, modifier: Modifier = Modifier) {
    val loader = LocalAppIconLoader.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(null, packageName, sizePx) {
        value = loader.load(packageName, sizePx)
    }
    val icon = bitmap
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(
            modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}
