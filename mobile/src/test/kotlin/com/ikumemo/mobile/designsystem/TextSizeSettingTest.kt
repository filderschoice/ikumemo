package com.ikumemo.mobile.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class TextSizeSettingTest {
    @Test
    fun normalKeepsDeviceScale() {
        assertEquals(1.3f, TextSizeSetting.effectiveFontScale(1.3f, TextSizeSetting.NORMAL), 0.0001f)
    }

    @Test
    fun appSettingMultipliesDeviceScale() {
        assertEquals(1.15f, TextSizeSetting.effectiveFontScale(1.0f, TextSizeSetting.LARGE), 0.0001f)
    }

    @Test
    fun combinedScaleIsCappedAt200Percent() {
        // 端末 2.0 × とても大きい 1.3 = 2.6 → 2.0 で頭打ち（PLAN F-65）
        assertEquals(2.0f, TextSizeSetting.effectiveFontScale(2.0f, TextSizeSetting.EXTRA_LARGE), 0.0001f)
    }

    @Test
    fun unknownStorageKeyFallsBackToNormal() {
        assertEquals(TextSizeSetting.NORMAL, TextSizeSetting.fromStorageKey("huge"))
        TextSizeSetting.entries.forEach { assertEquals(it, TextSizeSetting.fromStorageKey(it.storageKey)) }
    }
}
