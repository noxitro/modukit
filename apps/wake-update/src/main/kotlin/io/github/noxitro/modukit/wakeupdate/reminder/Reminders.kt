package io.github.noxitro.modukit.wakeupdate.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.noxitro.modukit.wakeupdate.R
import io.github.noxitro.modukit.wakeupdate.ui.MainActivity
import io.github.noxitro.modukit.wakeupdate.wake.WakeActivity
import io.github.noxitro.modukit.wakeupdate.wakeUpdateApp
import java.util.concurrent.TimeUnit

/**
 * スリープ中のアプリの更新を定期的に知らせる。
 *
 * バックグラウンドからはほかのアプリを起動できないので、自動では起こさず、通知のボタンから起こしてもらう。
 */
object Reminders {
    const val NOTIFICATION_ID = 1
    private const val CHANNEL_ID = "reminder"
    private const val WORK_NAME = "reminder"

    /** 選べる間隔（日）。0 はオフ。 */
    val INTERVALS = listOf(0, 7, 14, 30)

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, days: Int) {
        val work = WorkManager.getInstance(context)
        if (days <= 0) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(days.toLong(), TimeUnit.DAYS)
            .setInitialDelay(days.toLong(), TimeUnit.DAYS)
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun show(context: Context, count: Int) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val wake = PendingIntent.getActivity(
            context,
            1,
            WakeActivity.intent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wake)
            .setColor(ContextCompat.getColor(context, R.color.dawn))
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.resources.getQuantityString(R.plurals.reminder_text, count, count))
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.reminder_action), wake)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // 通知の権限が直前に取り消された
        }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val count = applicationContext.wakeUpdateApp.wakeTargets().size
        if (count > 0) Reminders.show(applicationContext, count)
        return Result.success()
    }
}
