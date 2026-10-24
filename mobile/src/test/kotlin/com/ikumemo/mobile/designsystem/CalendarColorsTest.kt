package com.ikumemo.mobile.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarColorsTest {
    private fun failures(
        minRatio: Double,
        colors: (Boolean) -> List<Long>,
    ): List<String> =
        ThemePreset.entries.flatMap { preset ->
            listOf(false, true).flatMap { dark ->
                val palette = preset.palette(dark)
                colors(dark).flatMap { color ->
                    listOf(palette.surface, palette.background).mapNotNull { background ->
                        val ratio = ContrastRatio.of(color, background)
                        if (ratio < minRatio) "${preset.name}/$dark %08X = %.2f".format(color, ratio) else null
                    }
                }
            }
        }

    @Test
    fun weekendTextMeetsTextContrast() {
        val result =
            failures(ContrastRatio.MIN_TEXT_RATIO) { dark ->
                listOf(CalendarColors.sunday(dark), CalendarColors.saturday(dark))
            }
        assertTrue("4.5:1 未満: $result", result.isEmpty())
    }

    @Test
    fun storeDotsMeetGraphicContrast() {
        val result = failures(MIN_GRAPHIC_RATIO) { dark -> CalendarColors.allDots(dark) }
        assertTrue("3:1 未満: $result", result.isEmpty())
    }

    private companion object {
        /** 文字以外の図形（WCAG 1.4.11）。 */
        const val MIN_GRAPHIC_RATIO = 3.0
    }
}
