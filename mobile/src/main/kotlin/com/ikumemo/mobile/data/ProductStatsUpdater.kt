package com.ikumemo.mobile.data

import com.ikumemo.core.suggest.PurchaseStats
import com.ikumemo.mobile.data.db.IkumemoDatabase

/** 品目の購入回数・最後に買った日・平均間隔を、残っている記録から計算し直す（R-12）。トランザクション内で呼ぶ。 */
class ProductStatsUpdater(
    db: IkumemoDatabase,
) {
    private val productDao = db.productDao()
    private val recordDao = db.purchaseRecordDao()

    suspend fun recalculate(productId: Long) {
        val product = productDao.getById(productId) ?: return
        val stats = PurchaseStats.from(recordDao.purchaseDatesOf(productId))
        productDao.update(
            product.copy(
                purchaseCount = stats.purchaseCount,
                lastPurchasedAt = stats.lastPurchasedAt,
                avgIntervalDays = stats.averageIntervalDays,
            ),
        )
    }
}
