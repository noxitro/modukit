package io.github.noxitro.modukit.wakeupdate.wake

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import io.github.noxitro.modukit.wakeupdate.R
import io.github.noxitro.modukit.wakeupdate.reminder.Reminders
import io.github.noxitro.modukit.wakeupdate.ui.MainActivity
import io.github.noxitro.modukit.wakeupdate.wakeUpdateApp

/**
 * 画面を持たない Activity。アプリ内のボタン、クイック設定のタイル、ショートカット、通知から呼ばれる。
 *
 * 前面にいる間にしかほかのアプリを起動できないので、onCreate の中で全部起動してすぐ閉じる。
 */
class WakeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationManagerCompat.from(this).cancel(Reminders.NOTIFICATION_ID)

        val app = wakeUpdateApp
        val only = intent.getStringArrayExtra(EXTRA_PACKAGES)?.toSet()
        val targets = app.wakeTargets(only)
        if (targets.isEmpty()) {
            Toast.makeText(this, R.string.toast_nothing_to_wake, Toast.LENGTH_SHORT).show()
            finishQuietly()
            return
        }

        app.beginSession(targets)
        WakeLauncher.launchAll(this, targets.map { it.packageName })
        // Play ストアの下にこのアプリを置いて、「戻る」で起こしたアプリではなくこのアプリに戻るようにする
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION,
            ),
        )
        WakeLauncher.openPlayStoreUpdates(this)
        app.checkSoon()
        finishQuietly()
    }

    private fun finishQuietly() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        private const val EXTRA_PACKAGES = "packages"

        /** @param only 指定したアプリだけを起こす（「もう一度起こす」用） */
        fun intent(context: Context, only: Collection<String>? = null): Intent =
            Intent(context, WakeActivity::class.java).apply {
                if (only != null) putExtra(EXTRA_PACKAGES, only.toTypedArray())
            }
    }
}
