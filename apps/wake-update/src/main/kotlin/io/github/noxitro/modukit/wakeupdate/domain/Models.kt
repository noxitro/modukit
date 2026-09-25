package io.github.noxitro.modukit.wakeupdate.domain

/** アプリを入れたストア。Play ストア以外のアプリは Play ストアでは更新されない。 */
enum class AppSource {
    PLAY_STORE,
    GALAXY_STORE,
    OTHER;

    companion object {
        const val PLAY_STORE_PACKAGE = "com.android.vending"
        const val GALAXY_STORE_PACKAGE = "com.sec.android.app.samsungapps"

        fun fromInstaller(installer: String?): AppSource = when (installer) {
            PLAY_STORE_PACKAGE -> PLAY_STORE
            GALAXY_STORE_PACKAGE -> GALAXY_STORE
            else -> OTHER
        }
    }
}

/**
 * ディープスリープ中のアプリ。
 *
 * Galaxy はディープスリープ中のアプリを無効化状態（ApplicationInfo.enabled == false）にするので、
 * Play ストアからは見えなくなり、アプリを起動するまで更新されない。
 */
data class SleepingApp(
    val packageName: String,
    val label: String,
    val source: AppSource,
    val versionName: String?,
    val versionCode: Long,
    val lastUpdateTime: Long,
)

/** アプリの現在の状態。セッション中にアプリが起きたか・更新されたかの判定に使う。 */
data class AppState(
    val enabled: Boolean,
    val versionName: String?,
    val versionCode: Long,
    val lastUpdateTime: Long,
)
