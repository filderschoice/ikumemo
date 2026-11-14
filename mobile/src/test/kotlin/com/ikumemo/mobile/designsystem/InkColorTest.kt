package com.ikumemo.mobile.designsystem

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** BL-108 の完了条件「ペンの色ごとに文字と膜の組み合わせが読める（4.5:1 以上）」「未知の保存値は おまかせ に戻る」。 */
class InkColorTest {
    @Test
    fun everyPenColorIsReadableOnItsOwnScrim() {
        InkColor.entries.filter { it != InkColor.AUTO }.forEach { ink ->
            val text = ink.textColor!!.toArgb().toLong() and 0xFFFFFFFFL
            val scrim = ink.scrimColor!!.toArgb().toLong() and 0xFFFFFFFFL
            val ratio = ContrastRatio.of(text, scrim)
            assertTrue("${ink.storageKey} は $ratio", ratio >= ContrastRatio.MIN_TEXT_RATIO)
        }
    }

    /**
     * BL-170。面の中の文字は onSurface、写真の上に直接置く文字はペンの色で、それぞれの背景（面・膜）の上で 4.5:1 以上。
     * 面の文字がペンの色にならないのは、surface と background が同じ色のテーマで Surface の既定の文字色が
     * onBackground（ペンの色）に当たらないよう onSurface を明示しているため。この前提（同じ色）が崩れても明示で守られる。
     */
    @Test
    fun panelTextAndDirectInkAreReadableForEveryThemeAndInk() {
        ThemePreset.entries.forEach { preset ->
            listOf(false, true).forEach { dark ->
                val palette = preset.palette(dark)
                val panel = ContrastRatio.of(palette.onSurface, palette.surface)
                assertTrue("${preset.storageKey} dark=$dark の面の文字は $panel", panel >= ContrastRatio.MIN_TEXT_RATIO)
                InkColor.entries.filter { it != InkColor.AUTO }.forEach { ink ->
                    val text = ink.textColor!!.toArgb().toLong() and 0xFFFFFFFFL
                    val scrim = ink.scrimColor!!.toArgb().toLong() and 0xFFFFFFFFL
                    val direct = ContrastRatio.of(text, scrim)
                    assertTrue("${ink.storageKey} の写真の上の文字は $direct", direct >= ContrastRatio.MIN_TEXT_RATIO)
                }
            }
        }
    }

    @Test
    fun autoKeepsTheThemeColors() {
        assertNull(InkColor.AUTO.textColor)
        assertNull(InkColor.AUTO.scrimColor)
    }

    @Test
    fun unknownStorageKeyFallsBackToAuto() {
        assertEquals(InkColor.AUTO, InkColor.fromStorageKey(null))
        assertEquals(InkColor.AUTO, InkColor.fromStorageKey("gold"))
        InkColor.entries.forEach { assertEquals(it, InkColor.fromStorageKey(it.storageKey)) }
    }
}
