package io.github.noxitro.modukit.wakeupdate.wake

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import io.github.noxitro.modukit.wakeupdate.domain.AppSource

object WakeLauncher {
    private const val ACTION_PLAY_MY_APPS = "com.google.android.finsky.VIEW_MY_DOWNLOADS"

    /**
     * アプリを順に起動して、ディープスリープから起こす。
     *
     * Galaxy はアプリが起動されると一時的に起こし、画面オフでまたディープスリープに戻す。
     * バックグラウンドからの起動は制限されているので、前面にある Activity から一気に呼ぶ。
     *
     * @return 起動できたアプリの数
     */
    fun launchAll(activity: Activity, packageNames: List<String>): Int {
        val pm = activity.packageManager
        var launched = 0
        for (name in packageNames) {
            val intent = pm.getLaunchIntentForPackage(name) ?: continue
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            if (tryStart(activity, intent)) launched++
        }
        return launched
    }

    /** Play ストアの「アプリとデバイスの管理」を開く。開けないときは Play ストアのトップを開く。 */
    fun openPlayStoreUpdates(context: Context): Boolean {
        val myApps = Intent(ACTION_PLAY_MY_APPS)
            .setPackage(AppSource.PLAY_STORE_PACKAGE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (tryStart(context, myApps)) return true
        val main = context.packageManager.getLaunchIntentForPackage(AppSource.PLAY_STORE_PACKAGE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: return false
        return tryStart(context, main)
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
