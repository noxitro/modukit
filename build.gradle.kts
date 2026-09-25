plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 の組み込み Kotlin で使う Kotlin Gradle Plugin のバージョンを固定する
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.roborazzi) apply false
}
