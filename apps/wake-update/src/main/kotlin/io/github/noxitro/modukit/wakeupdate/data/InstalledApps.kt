package io.github.noxitro.modukit.wakeupdate.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import io.github.noxitro.modukit.wakeupdate.domain.AppSource
import io.github.noxitro.modukit.wakeupdate.domain.AppState
import io.github.noxitro.modukit.wakeupdate.domain.SleepingApp
import java.text.Collator

/**
 * 端末のアプリ情報を読む。
 *
 * ほかのアプリは、マニフェストの <queries>（ランチャーに表示されるアプリ）の範囲で見える。
 */
class InstalledApps(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    val isGalaxy: Boolean
        get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    /**
     * ディープスリープ中のアプリ。
     *
     * Galaxy はディープスリープ中のアプリを無効化状態にする。ユーザーが入れたアプリは
     * 設定アプリから無効化できないので、無効化状態でも起動できるものをディープスリープ中とみなす。
     */
    fun sleepingApps(): List<SleepingApp> {
        val collator = Collator.getInstance()
        return installedApplications()
            .asSequence()
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .filter { !it.enabled && it.packageName != context.packageName }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .mapNotNull(::toSleepingApp)
            .sortedWith { x, y -> collator.compare(x.label, y.label) }
            .toList()
    }

    /** 指定したアプリの今の状態。アンインストールされたアプリは含まれない。 */
    fun states(packageNames: Collection<String>): Map<String, AppState> =
        packageNames.mapNotNull { name ->
            val info = packageInfo(name) ?: return@mapNotNull null
            name to AppState(
                enabled = info.applicationInfo?.enabled ?: false,
                versionName = info.versionName,
                versionCode = info.longVersionCode,
                lastUpdateTime = info.lastUpdateTime,
            )
        }.toMap()

    fun icon(packageName: String): Drawable? = try {
        pm.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun toSleepingApp(info: ApplicationInfo): SleepingApp? {
        val pkg = packageInfo(info.packageName) ?: return null
        return SleepingApp(
            packageName = info.packageName,
            label = info.loadLabel(pm).toString(),
            source = AppSource.fromInstaller(installerOf(info.packageName)),
            versionName = pkg.versionName,
            versionCode = pkg.longVersionCode,
            lastUpdateTime = pkg.lastUpdateTime,
        )
    }

    private fun installerOf(packageName: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(packageName)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    // QUERY_ALL_PACKAGES は使わず、マニフェストの <queries> でランチャーのアプリだけを見る
    @SuppressLint("QueryPermissionsNeeded")
    private fun installedApplications(): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }

    private fun packageInfo(packageName: String): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
