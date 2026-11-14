package com.ikumemo.mobile.designsystem

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** BL-176 の完了条件のうち、保存値の読み方と「おまかせ」を選べる条件（Android 12 以降）。 */
@RunWith(RobolectricTestRunner::class)
class ThemeChoiceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

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
    fun seedKeyAcceptsOnlyHalfWidthHex() {
        val expected = ThemeChoice.seed(0xAB12EFL)
        assertEquals(expected, ThemeChoice.fromStorageKey("seed:#AB12EF"))
        assertEquals(expected, ThemeChoice.fromStorageKey("seed:#ab12ef"))
        // 全角の数字・英字は不正値として既定に戻す
        assertEquals(ThemeChoice.DEFAULT, ThemeChoice.fromStorageKey("seed:#１２３４５６"))
        assertEquals(ThemeChoice.DEFAULT, ThemeChoice.fromStorageKey("seed:#ＡB12EF"))
        assertEquals(ThemeChoice.DEFAULT, ThemeChoice.fromStorageKey("seed:#12345G"))
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

    @Test
    fun wallpaperPaletteUsesDefaultBeforeAndroid12() {
        var called = false
        val palette =
            ThemeChoice.Wallpaper.palette(context, darkTheme = false, sdkInt = 30) { _, _ ->
                called = true
                WALLPAPER_MARK
            }
        assertFalse(called)
        assertEquals(ThemePreset.DEFAULT.palette(false), palette)
    }

    @Test
    fun wallpaperPaletteUsesWallpaperColorsFromAndroid12() {
        listOf(false, true).forEach { dark ->
            val palette =
                ThemeChoice.Wallpaper.palette(context, darkTheme = dark, sdkInt = 31) { _, isDark ->
                    assertEquals(dark, isDark)
                    WALLPAPER_MARK
                }
            assertEquals(WALLPAPER_MARK, palette)
        }
    }

    private companion object {
        val WALLPAPER_MARK = ThemePreset.DEFAULT.palette(false).copy(primary = 0x12345678L)
    }
}
