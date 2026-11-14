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
    fun keepsKanjiWhenNotFollowedByCounter() {
        assertEquals("十分", QuantityText.normalize("十分"))
        assertEquals("一部", QuantityText.normalize("一部"))
        assertEquals("一杯", QuantityText.normalize("一杯"))
        assertEquals("二本ずつ", QuantityText.normalize("二本ずつ"))
        assertEquals("〇", QuantityText.normalize("〇"))
        assertEquals("〇〇", QuantityText.normalize("〇〇"))
        assertEquals("〇分", QuantityText.normalize("〇分"))
        assertEquals("0個", QuantityText.normalize("〇個"))
        assertEquals("2本", QuantityText.normalize("２本"))
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
