package com.ikumemo.core.suggest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PurchaseStatsTest {
    @Test
    fun noPurchasesIsEmpty() {
        assertEquals(PurchaseStats.EMPTY, PurchaseStats.from(emptyList()))
    }

    @Test
    fun singlePurchaseHasNoInterval() {
        val stats = PurchaseStats.from(listOf(LocalDate.of(2026, 10, 1)))

        assertEquals(1, stats.purchaseCount)
        assertEquals(LocalDate.of(2026, 10, 1), stats.lastPurchasedAt)
        assertNull(stats.averageIntervalDays)
    }

    @Test
    fun averageIntervalIsSpanDividedByGaps() {
        val dates = listOf(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7))

        val stats = PurchaseStats.from(dates)

        assertEquals(3, stats.purchaseCount)
        assertEquals(LocalDate.of(2026, 10, 19), stats.lastPurchasedAt)
        assertEquals(9.0, stats.averageIntervalDays ?: Double.NaN, 0.0001)
    }

    @Test
    fun sameDayPurchasesCountSeparately() {
        val day = LocalDate.of(2026, 10, 1)

        val stats = PurchaseStats.from(listOf(day, day))

        assertEquals(2, stats.purchaseCount)
        assertEquals(0.0, stats.averageIntervalDays ?: Double.NaN, 0.0001)
    }

    @Test
    fun intervalSpansYearEnd() {
        val stats = PurchaseStats.from(listOf(LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 4)))

        assertEquals(10.0, stats.averageIntervalDays ?: Double.NaN, 0.0001)
    }
}
