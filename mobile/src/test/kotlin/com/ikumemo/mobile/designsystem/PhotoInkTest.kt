package com.ikumemo.mobile.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** BL-174。写真の上に直接置く文字（副題・見出し・チップ・ボタン）は、ペンの色があればそれにし、膜との比が 4.5:1 以上になる。 */
class PhotoInkTest {
    @Test
    fun noPenColorKeepsTheThemeColor() {
        ThemePreset.entries.forEach { preset ->
            listOf(false, true).forEach { dark ->
                val theme = Color(preset.palette(dark).onSurfaceVariant)
                assertEquals(theme, resolvePhotoInk(null, theme))
            }
        }
    }

    /** BL-204。おまかせの写真の上の文字（LocalContentColor）は onBackground になり、テーマの背景（膜）の上で読める。 */
    @Test
    fun autoInkFallsBackToOnBackgroundAndIsReadableOnTheThemeBackground() {
        ThemePreset.entries.forEach { preset ->
            listOf(false, true).forEach { dark ->
                val palette = preset.palette(dark)
                val onBackground = Color(palette.onBackground)
                assertEquals(onBackground, resolvePhotoInk(InkColor.AUTO.textColor, onBackground))
                val ratio =
                    ContrastRatio.of(
                        onBackground.toArgb().toLong() and 0xFFFFFFFFL,
                        Color(palette.background).toArgb().toLong() and 0xFFFFFFFFL,
                    )
                val label = "${preset.storageKey} dark=$dark は $ratio"
                assertTrue(label, ratio >= ContrastRatio.MIN_TEXT_RATIO)
            }
        }
    }

    @Test
    fun penColorReplacesTheThemeColorAndIsReadableOnItsScrim() {
        InkColor.entries.filter { it != InkColor.AUTO }.forEach { ink ->
            ThemePreset.entries.forEach { preset ->
                listOf(false, true).forEach { dark ->
                    val theme = Color(preset.palette(dark).primary)
                    val resolved = resolvePhotoInk(ink.textColor, theme)
                    assertEquals(ink.textColor, resolved)
                    val ratio =
                        ContrastRatio.of(
                            resolved.toArgb().toLong() and 0xFFFFFFFFL,
                            ink.scrimColor!!.toArgb().toLong() and 0xFFFFFFFFL,
                        )
                    val label = "${ink.storageKey} ${preset.storageKey} dark=$dark は $ratio"
                    assertTrue(label, ratio >= ContrastRatio.MIN_TEXT_RATIO)
                }
            }
        }
    }
}
