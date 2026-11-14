package com.ikumemo.mobile.designsystem

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.data.Appearance
import com.ikumemo.mobile.widget.WidgetColors
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * BL-177: 見本の色で作った配色に、アプリ本体（[palette]。写真の上のペンの色「おまかせ」もこの配色から取る）と
 * ウィジェットが同じく従うこと。おまかせと違い端末の機能に頼らないため、Android 11 以前でも同じ配色になる。
 */
@RunWith(RobolectricTestRunner::class)
class SeedThemeFollowTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val seed = SeedSwatch.SKY.choice

    @Test
    fun appPaletteIsTheSeedPalette() {
        assertEquals(seed.palette(darkTheme = false), seed.palette(context, darkTheme = false))
        assertEquals(seed.palette(darkTheme = true), seed.palette(context, darkTheme = true))
    }

    @Test
    @Config(sdk = [30])
    fun seedPaletteIsAvailableBeforeAndroid12() {
        assertEquals(seed.palette(darkTheme = false), seed.palette(context, darkTheme = false))
        assertEquals(seed, seed.availableOr(ThemeChoice.DEFAULT, wallpaperAvailable = false))
    }

    @Test
    fun widgetFollowsSeedPalette() {
        val system = WidgetColors.from(context, Appearance(theme = seed, darkMode = DarkMode.SYSTEM))
        assertEquals(seed.palette(darkTheme = false), system.day)
        assertEquals(seed.palette(darkTheme = true), system.night)
        assertEquals(seed.corners, system.corners)

        val dark = WidgetColors.from(context, Appearance(theme = seed, darkMode = DarkMode.DARK))
        assertEquals(seed.palette(darkTheme = true), dark.day)
        assertEquals(seed.palette(darkTheme = true), dark.night)
    }
}
