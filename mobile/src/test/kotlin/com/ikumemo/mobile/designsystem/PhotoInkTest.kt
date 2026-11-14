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
