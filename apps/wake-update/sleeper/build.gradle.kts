// エミュレータテスト（:apps:wake-update の androidTest）で起こして更新するアプリ。
// 同じパッケージの v1 と v2 を作り、テストで v1 から v2 に更新する。配布はしない
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.noxitro.modukit.sleeper"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "io.github.noxitro.modukit.sleeper"
        minSdk = 29
        targetSdk = 36
    }

    flavorDimensions += "version"
    productFlavors {
        create("v1") {
            dimension = "version"
            versionCode = 1
            versionName = "1.0"
        }
        create("v2") {
            dimension = "version"
            versionCode = 2
            versionName = "2.0"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
