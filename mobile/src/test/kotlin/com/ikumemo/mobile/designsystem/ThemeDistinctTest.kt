package com.ikumemo.mobile.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** BL-175。4 種のプリセットが見分けられる（背景・アクセントの色相が重ならず、角の丸みも別）。 */
class ThemeDistinctTest {
    private val tinted = listOf(ThemePreset.POP, ThemePreset.GENTLE, ThemePreset.COOL)

    @Test
    fun backgroundsAndAccentsDifferInHueAndTheSimpleThemeStaysNeutral() {
        listOf(false, true).forEach { dark ->
            val pairs = tinted.flatMap { a -> tinted.filter { it.ordinal > a.ordinal }.map { a to it } }
            pairs.forEach { (a, b) ->
                val bg = hueGap(a.palette(dark).background, b.palette(dark).background)
                val accent = hueGap(a.palette(dark).primary, b.palette(dark).primary)
                assertTrue("${a.name}/${b.name} dark=$dark 背景の色相差 $bg", bg >= MIN_HUE_GAP)
                assertTrue("${a.name}/${b.name} dark=$dark アクセントの色相差 $accent", accent >= MIN_HUE_GAP)
            }
            val simple = ThemePreset.SIMPLE.palette(dark).background
            val (r, g, b) = rgb(simple)
            assertEquals("シンプルの背景は無彩色", 0.0, maxOf(r, g, b) - minOf(r, g, b), 0.02)
            tinted.forEach {
                val sat = saturation(it.palette(dark).background)
                assertTrue("${it.name} dark=$dark の背景は色付き $sat", sat > MIN_TINT)
            }
        }
    }

    @Test
    fun everyThemeHasItsOwnCorners() {
        val corners = ThemePreset.entries.map { it.corners }
        assertEquals(corners.size, corners.toSet().size)
    }

    private fun rgb(color: Long): Triple<Double, Double, Double> =
        Triple(
            (color shr 16 and 0xFF) / 255.0,
            (color shr 8 and 0xFF) / 255.0,
            (color and 0xFF) / 255.0,
        )

    private fun saturation(color: Long): Double {
        val (r, g, b) = rgb(color)
        val max = maxOf(r, g, b)
        return if (max == 0.0) 0.0 else (max - minOf(r, g, b)) / max
    }

    private fun hue(color: Long): Double {
        val (r, g, b) = rgb(color)
        val max = maxOf(r, g, b)
        val d = max - minOf(r, g, b)
        if (d == 0.0) return 0.0
        val h =
            when (max) {
                r -> ((g - b) / d) % 6
                g -> (b - r) / d + 2
                else -> (r - g) / d + 4
            }
        return (h * 60 + 360) % 360
    }

    private fun hueGap(
        a: Long,
        b: Long,
    ): Double = abs(hue(a) - hue(b)).let { minOf(it, 360 - it) }

    private companion object {
        const val MIN_HUE_GAP = 40.0
        const val MIN_TINT = 0.03
    }
}
