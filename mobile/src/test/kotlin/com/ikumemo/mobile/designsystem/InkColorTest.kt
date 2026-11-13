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
