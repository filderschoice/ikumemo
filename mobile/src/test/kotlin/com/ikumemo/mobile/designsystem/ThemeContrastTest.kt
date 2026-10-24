package com.ikumemo.mobile.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** PLAN §1.3-7・§2.7「各テーマはライト／ダーク両方を用意し、コントラスト比 4.5:1 を自動テストで確認する」。 */
class ThemeContrastTest {
    @Test
    fun allPresetsMeetTextContrastInLightAndDark() {
        val failures =
            ThemePreset.entries.flatMap { preset ->
                listOf(false, true).flatMap { dark ->
                    preset.palette(dark).textPairs().mapNotNull { (name, pair) ->
                        val ratio = ContrastRatio.of(pair.first, pair.second)
                        if (ratio < ContrastRatio.MIN_TEXT_RATIO) {
                            "${preset.name}/${if (dark) "dark" else "light"} $name = %.2f".format(ratio)
                        } else {
                            null
                        }
                    }
                }
            }
        assertTrue("コントラスト比 4.5:1 未満の組み合わせ: $failures", failures.isEmpty())
    }

    @Test
    fun presetsAreFourAsDecided() {
        // PLAN D-8: MVP のプリセットは 4 種
        assertEquals(4, ThemePreset.entries.size)
    }

    @Test
    fun storageKeysAreUniqueAndRoundTrip() {
        val keys = ThemePreset.entries.map { it.storageKey }
        assertEquals(keys.size, keys.toSet().size)
        ThemePreset.entries.forEach { assertEquals(it, ThemePreset.fromStorageKey(it.storageKey)) }
    }

    @Test
    fun unknownStorageKeyFallsBackToDefault() {
        assertEquals(ThemePreset.DEFAULT, ThemePreset.fromStorageKey(null))
        assertEquals(ThemePreset.DEFAULT, ThemePreset.fromStorageKey("removed-theme"))
    }
}
