package io.github.noxitro.modukit.sleeper

import android.app.Activity
import android.os.Bundle

/** 起動されたらすぐ閉じる。 */
class SleeperActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
