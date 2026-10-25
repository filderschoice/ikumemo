package com.ikumemo.core.suggest

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 品目ごとの購入の集計（R-12）。記録を追加・修正・削除するたびに、残っている購入日から計算し直す。
 *
 * @property averageIntervalDays 最初と最後の購入日の間隔を「回数 - 1」で割った平均。2 回未満なら null
 */
data class PurchaseStats(
    val purchaseCount: Int,
    val lastPurchasedAt: LocalDate?,
    val averageIntervalDays: Double?,
) {
    companion object {
        val EMPTY = PurchaseStats(purchaseCount = 0, lastPurchasedAt = null, averageIntervalDays = null)

        fun from(purchaseDates: List<LocalDate>): PurchaseStats {
            if (purchaseDates.isEmpty()) return EMPTY
            val first = purchaseDates.min()
            val last = purchaseDates.max()
            val average =
                if (purchaseDates.size >= 2) {
                    ChronoUnit.DAYS.between(first, last).toDouble() / (purchaseDates.size - 1)
                } else {
                    null
                }
            return PurchaseStats(purchaseDates.size, last, average)
        }
    }
}
