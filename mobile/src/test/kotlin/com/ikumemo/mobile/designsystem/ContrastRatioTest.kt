package com.ikumemo.mobile.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class ContrastRatioTest {
    @Test
    fun blackOnWhiteIs21() {
        assertEquals(21.0, ContrastRatio.of(0xFF000000, 0xFFFFFFFF), 0.01)
    }

    @Test
    fun sameColorIs1() {
        assertEquals(1.0, ContrastRatio.of(0xFF777777, 0xFF777777), 0.0001)
    }

    @Test
    fun orderDoesNotMatter() {
        assertEquals(ContrastRatio.of(0xFF1F3A68, 0xFFFFFFFF), ContrastRatio.of(0xFFFFFFFF, 0xFF1F3A68), 0.0001)
    }

    @Test
    fun knownWcagValue() {
        // #767676 は白地で 4.54:1（WCAG AA を満たす最も明るい灰色として知られる値）
        assertEquals(4.54, ContrastRatio.of(0xFF767676, 0xFFFFFFFF), 0.01)
    }
}
