package com.ikumemo.mobile.designsystem

import android.content.Context
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.data.Appearance
import com.ikumemo.mobile.widget.WidgetColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * BL-176「おまかせ（壁紙の色）」の配色の仕組み。壁紙の色は実行時に決まるため、ここでは Robolectric の既定の壁紙の色で、
 * Dynamic Color の配色がそのまま使われること・文字と面の色の比・ウィジェットが同じ配色に従うことを確かめる
 * （実機の壁紙での見た目は人手検証）。
 */
@RunWith(RobolectricTestRunner::class)
class WallpaperPaletteTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun wallpaperUsesDynamicColorOnAndroid12AndLater() {
        val wallpaper = ThemeChoice.Wallpaper
        assertEquals(dynamicLightColorScheme(context).toPalette(), wallpaper.palette(context, darkTheme = false))
        assertEquals(dynamicDarkColorScheme(context).toPalette(), wallpaper.palette(context, darkTheme = true))
    }

    @Test
    fun wallpaperPaletteMeetsTextContrast() {
        val failures =
            listOf(false, true).flatMap { dark ->
                ThemeChoice.Wallpaper.palette(context, dark).textPairs().mapNotNull { (name, pair) ->
                    val ratio = ContrastRatio.of(pair.first, pair.second)
                    val mode = if (dark) "dark" else "light"
                    if (ratio < ContrastRatio.MIN_TEXT_RATIO) "$mode $name = %.2f".format(ratio) else null
                }
            }
        assertTrue("コントラスト比 4.5:1 未満の組み合わせ: $failures", failures.isEmpty())
    }

    @Test
    @Config(sdk = [30])
    fun wallpaperFallsBackToDefaultBeforeAndroid12() {
        listOf(false, true).forEach { dark ->
            assertEquals(ThemePreset.DEFAULT.palette(dark), ThemeChoice.Wallpaper.palette(context, dark))
        }
    }

    @Test
    fun presetPaletteIsUnchangedThroughChoice() {
        ThemePreset.entries.forEach { preset ->
            assertEquals(preset.light, preset.palette(context, darkTheme = false))
            assertEquals(preset.dark, preset.palette(context, darkTheme = true))
        }
    }

    @Test
    fun widgetFollowsWallpaperPalette() {
        val colors = WidgetColors.from(context, Appearance(theme = ThemeChoice.Wallpaper, darkMode = DarkMode.SYSTEM))
        assertEquals(ThemeChoice.Wallpaper.palette(context, darkTheme = false), colors.day)
        assertEquals(ThemeChoice.Wallpaper.palette(context, darkTheme = true), colors.night)
        assertEquals(ThemeChoice.Wallpaper.corners, colors.corners)
    }
}
