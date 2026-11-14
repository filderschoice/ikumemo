package com.ikumemo.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Baseline Profile を作るテスト（BL-100）。起動してメモ一覧を表示し、下のタブ（記録・期限）を巡って戻る。
 * 実機で `./gradlew :mobile:generateBaselineProfile` から流す（手順は docs/BASELINE_PROFILE.md）。
 */
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.text("メモ")), UI_TIMEOUT_MS)
            for (tab in listOf("記録", "期限", "メモ")) {
                device.findObject(By.text(tab))?.click()
                device.waitForIdle()
            }
        }

    private companion object {
        const val TARGET_PACKAGE = "com.ikumemo.mobile"
        const val UI_TIMEOUT_MS = 5_000L
    }
}
