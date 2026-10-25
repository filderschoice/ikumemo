package com.ikumemo.mobile.data

import androidx.room.withTransaction
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ShoppingTripEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** 記録を修正するときの内容（F-16）。 */
data class RecordEdit(
    val purchasedDate: LocalDate,
    val storeId: Long?,
    val quantity: String?,
    val note: String?,
    val photoUri: String?,
)

/** 買った記録の読み書き（カレンダー・おしらせから使う）。 */
class RecordRepository(
    private val db: IkumemoDatabase,
    private val normalizer: ProductNameNormalizer = ProductNameNormalizer(),
) {
    private val recordDao = db.purchaseRecordDao()
    private val tripDao = db.shoppingTripDao()
    private val statsUpdater = ProductStatsUpdater(db)

    fun observeBetween(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<PurchaseRecordEntity>> = recordDao.observeBetween(from, to)

    suspend fun getRecord(id: Long): PurchaseRecordEntity? = recordDao.getById(id)

    /** 「牛乳 いつ買った？」に答える品名検索（F-42）。空の入力では何も返さない。 */
    suspend fun search(query: String): List<PurchaseRecordEntity> {
        val raw = query.trim().filterNot { it == '%' || it == '_' }
        val normalized = normalizer.normalize(raw).filterNot { it == '%' || it == '_' }
        if (normalized.isEmpty()) return emptyList()
        return recordDao.search(normalized, raw, SEARCH_LIMIT)
    }

    /**
     * 記録を修正する（F-16）。購入日・お店が変わったら、その日・お店のお買い物へ付け替える。
     * 品目の購入回数・最後に買った日・平均間隔を計算し直す（R-12）。
     */
    suspend fun updateRecord(
        recordId: Long,
        edit: RecordEdit,
    ) {
        db.withTransaction {
            val record = recordDao.getById(recordId) ?: return@withTransaction
            val tripId =
                if (record.purchasedDate == edit.purchasedDate && record.storeId == edit.storeId) {
                    record.tripId
                } else {
                    findOrCreateTrip(edit.purchasedDate, edit.storeId)
                }
            recordDao.update(
                record.copy(
                    tripId = tripId,
                    purchasedDate = edit.purchasedDate,
                    storeId = edit.storeId,
                    quantity = edit.quantity?.trim()?.ifEmpty { null },
                    note = edit.note?.trim()?.ifEmpty { null },
                    photoUri = edit.photoUri,
                ),
            )
            tripDao.deleteUnused()
            statsUpdater.recalculate(record.productId)
        }
    }

    /** 記録を消す。紐づくおしらせも消し（外部キーの CASCADE）、品目の集計を計算し直す（R-12）。 */
    suspend fun deleteRecord(recordId: Long) {
        db.withTransaction {
            val record = recordDao.getById(recordId) ?: return@withTransaction
            recordDao.delete(record)
            tripDao.deleteUnused()
            statsUpdater.recalculate(record.productId)
        }
    }

    suspend fun findOrCreateTrip(
        date: LocalDate,
        storeId: Long?,
    ): Long = tripDao.find(date, storeId)?.id ?: tripDao.insert(ShoppingTripEntity(date = date, storeId = storeId))

    companion object {
        const val SEARCH_LIMIT = 200
    }
}
