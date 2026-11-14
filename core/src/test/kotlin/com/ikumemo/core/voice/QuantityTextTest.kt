package com.ikumemo.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuantityTextTest {
    @Test
    fun convertsFullWidthDigitsToHalfWidth() {
        assertEquals("2本", QuantityText.normalize("２本"))
        assertEquals("500g", QuantityText.normalize("５００g"))
        assertEquals("12", QuantityText.normalize("１２"))
    }

    @Test
    fun convertsLeadingKanjiNumber() {
        assertEquals("2本", QuantityText.normalize("二本"))
        assertEquals("10個", QuantityText.normalize("十個"))
        assertEquals("3", QuantityText.normalize("三"))
        assertEquals("120g", QuantityText.normalize("百二十g"))
    }

    @Test
    fun keepsHalfWidthDigitsAndUnits() {
        assertEquals("1パック", QuantityText.normalize("1パック"))
        assertEquals("1.5L", QuantityText.normalize(" 1.5L "))
    }

    @Test
    fun keepsTextWithoutLeadingNumber() {
        assertEquals("少し", QuantityText.normalize("少し"))
        assertEquals("大きめ2個", QuantityText.normalize("大きめ２個"))
    }

    @Test
    fun returnsNullForBlank() {
        assertNull(QuantityText.normalize(null))
        assertNull(QuantityText.normalize("  "))
        assertNull(QuantityText.normalize("　"))
    }
}
