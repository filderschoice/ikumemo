package com.ikumemo.mobile.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** BL-187。膜が明るい組み合わせでは状態バーのアイコンを暗く、膜が暗い組み合わせでは白のままにする。 */
class SystemBarIconsTest {
    private val darkThemeBackground = Color(0xFF1C1B1F)
    private val lightThemeBackground = Color(0xFFFFFBFF)

    @Test
    fun lightScrimGivesDarkIconsForEveryPenColorAndThemeDarkness() {
        listOf(InkColor.BLACK, InkColor.BLUE, InkColor.RED).forEach { ink ->
            listOf(false, true).forEach { dark ->
                val alpha = BackgroundScrim.effectiveAlpha(BackgroundScrim.DEFAULT, dark)
                assertTrue(
                    "${ink.storageKey} dark=$dark",
                    SystemBarIcons.useDarkIconsOnScrim(ink.scrimColor!!, alpha),
                )
            }
        }
    }

    @Test
    fun darkScrimKeepsLightIcons() {
        listOf(false, true).forEach { dark ->
            val alpha = BackgroundScrim.effectiveAlpha(BackgroundScrim.DEFAULT, dark)
            assertFalse("white dark=$dark", SystemBarIcons.useDarkIconsOnScrim(InkColor.WHITE.scrimColor!!, alpha))
        }
    }

    @Test
    fun autoInkFollowsThemeBackground() {
        val lightAlpha = BackgroundScrim.effectiveAlpha(BackgroundScrim.DEFAULT, false)
        val darkAlpha = BackgroundScrim.effectiveAlpha(BackgroundScrim.DEFAULT, true)
        assertTrue(SystemBarIcons.useDarkIconsOnScrim(lightThemeBackground, lightAlpha))
        assertFalse(SystemBarIcons.useDarkIconsOnScrim(darkThemeBackground, darkAlpha))
    }

    @Test
    fun darkScrimIsDarkEvenAtItsThinnestAndLightScrimIsLightAtItsThickest() {
        assertFalse(SystemBarIcons.useDarkIconsOnScrim(darkThemeBackground, BackgroundScrim.MIN))
        assertTrue(SystemBarIcons.useDarkIconsOnScrim(lightThemeBackground, BackgroundScrim.MIN))
    }

    @Test
    fun luminanceRisesWithThinnerLightScrimOverBrightPhotoAndFallsOverDarkPhoto() {
        val bright = SystemBarIcons.backgroundLuminance(darkThemeBackground, 0.3f, photoGray = 1f)
        val dim = SystemBarIcons.backgroundLuminance(darkThemeBackground, 0.3f, photoGray = 0f)
        assertTrue(bright > dim)
    }

    @Test
    fun withoutImageFollowsTheAppTheme() {
        assertTrue(SystemBarIcons.useDarkIconsWithoutImage(darkTheme = false))
        assertFalse(SystemBarIcons.useDarkIconsWithoutImage(darkTheme = true))
    }
}
