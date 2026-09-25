package io.github.noxitro.modukit.wakeupdate.wake

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.noxitro.modukit.wakeupdate.R
import io.github.noxitro.modukit.wakeupdate.wakeUpdateApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** クイック設定パネルから 1 タップで起こして更新する。 */
class WakeTileService : TileService() {
    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val listening = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = listening
        listening.launch {
            val count = withContext(Dispatchers.IO) { wakeUpdateApp.wakeTargets().size }
            val tile = qsTile ?: return@launch
            tile.state = if (count > 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.subtitle = if (count > 0) {
                resources.getQuantityString(R.plurals.tile_subtitle_sleeping, count, count)
            } else {
                getString(R.string.tile_subtitle_none)
            }
            tile.updateTile()
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun(::openWake) else openWake()
    }

    private fun openWake() {
        val intent = WakeActivity.intent(this).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
