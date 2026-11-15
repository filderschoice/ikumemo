package com.ikumemo.mobile.testing

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule

private const val MAX_WAIT_ROUNDS = 250
private const val WAIT_MILLIS = 20L

/**
 * 条件を満たすまで待つ。ViewModel の処理は Room の実行スレッドから戻るときにメインスレッドへ積まれるが、
 * Robolectric では `waitUntil` が待つ間にメインスレッドの処理を進めないため、`waitForIdle` で進めながら待つ。
 */
fun ComposeTestRule.awaitCondition(condition: () -> Boolean) {
    repeat(MAX_WAIT_ROUNDS) {
        waitForIdle()
        if (condition()) return
        Thread.sleep(WAIT_MILLIS)
    }
    error("条件を満たさないまま時間切れになった")
}

fun ComposeTestRule.awaitTag(tag: String) =
    awaitCondition {
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }
