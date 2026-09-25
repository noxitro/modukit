package io.github.noxitro.modukit.wakeupdate.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Galaxy の「バックグラウンドでの使用を制限」の各一覧を開く。
 *
 * Samsung が公開しているディープリンク（https://developer.samsung.com/mobile/app-management.html）を使い、
 * 開けない機種ではバッテリーの設定、最後は設定アプリを開く。
 */
object GalaxySettings {
    enum class SleepList(val activityType: Int) {
        SLEEPING(0),
        DEEP_SLEEPING(1),
        NEVER_SLEEPING(2),
    }

    private const val ACTION_OPEN_LIST = "com.samsung.android.sm.ACTION_OPEN_CHECKABLE_LISTACTIVITY"
    private val DEVICE_CARE_PACKAGES = listOf("com.samsung.android.lool", "com.samsung.android.sm", null)

    fun open(context: Context, list: SleepList) {
        for (pkg in DEVICE_CARE_PACKAGES) {
            val intent = Intent(ACTION_OPEN_LIST)
                .putExtra("activity_type", list.activityType)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (pkg != null) intent.setPackage(pkg)
            if (tryStart(context, intent)) return
        }
        if (tryStart(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))) return
        tryStart(context, Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
