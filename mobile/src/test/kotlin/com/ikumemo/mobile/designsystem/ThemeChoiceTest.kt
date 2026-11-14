package com.ikumemo.mobile.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** BL-176 の完了条件のうち、保存値の読み方と「おまかせ」を選べる条件（Android 12 以降）。 */
class ThemeChoiceTest {
    @Test
    fun presetKeysStayAsBefore() {
        // 保存済みのプリセットの選択が変わらないよう、従来の値をそのまま読む
        mapOf(
            "pop" to ThemePreset.POP,
            "gentle" to ThemePreset.GENTLE,
            "cool" to ThemePreset.COOL,
            "simple" to ThemePreset.SIMPLE,
        ).forEach { (key, preset) -> assertEquals(preset, ThemeChoice.fromStorageKey(key)) }
    }

    @Test
    fun wallpaperRoundTripsAndKeysAreUnique() {
        assertEquals(ThemeChoice.Wallpaper, ThemeChoice.fromStorageKey(ThemeChoice.Wallpaper.storageKey))
        val keys = (ThemePreset.entries + ThemeChoice.Wallpaper).map { it.storageKey }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun unknownKeyFallsBackToDefault() {
        assertEquals(ThemeChoice.DEFAULT, ThemeChoice.fromStorageKey(null))
        assertEquals(ThemeChoice.DEFAULT, ThemeChoice.fromStorageKey("seed-from-a-newer-version"))
        assertEquals(ThemePreset.DEFAULT, ThemeChoice.DEFAULT)
    }

    @Test
    fun dynamicColorNeedsAndroid12() {
        assertFalse(DynamicColor.isAvailable(sdkInt = 30))
        assertTrue(DynamicColor.isAvailable(sdkInt = 31))
        assertTrue(DynamicColor.isAvailable(sdkInt = 35))
    }

    @Test
    fun wallpaperIsReplacedOnlyWhenUnavailable() {
        val fallback = ThemeChoice.DEFAULT
        assertEquals(fallback, ThemeChoice.Wallpaper.availableOr(fallback, wallpaperAvailable = false))
        assertEquals(ThemeChoice.Wallpaper, ThemeChoice.Wallpaper.availableOr(fallback, wallpaperAvailable = true))
        ThemePreset.entries.forEach { preset ->
            assertEquals(preset, preset.availableOr(fallback, wallpaperAvailable = false))
        }
    }
}
