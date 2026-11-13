package com.ikumemo.mobile.data

import androidx.room.withTransaction
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ReminderEntity
import com.ikumemo.mobile.data.db.ShoppingTripEntity
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDate

/** 記録を修正するときの内容（F-16）。 */
data class RecordEdit(
    val purchasedDate: LocalDate,
    val storeId: Long?,
    val quantity: String?,
    val note: String?,
    val photoUri: String?,
)

/** あとから記録するときの内容（F-17）。 */
data class NewRecord(
    val name: String,
    val purchasedDate: LocalDate,
    val storeId: Long?,
    val quantity: String?,
    val note: String?,
    val photoUri: String?,
)

/** 消した記録と、一緒に消えたおしらせ・使われなくなったお買い物（元に戻すため、F-16）。 */
data class DeletedRecord(
    val record: PurchaseRecordEntity,
    val reminder: ReminderEntity?,
    val trip: ShoppingTripEntity?,
)

/**
 * 買った記録の読み書き（カレンダー・おしらせから使う）。
 * 記録の削除・元に戻すでおしらせが消えた・戻ったら [onRemindersChanged] を呼ぶ（次の通知の予約をし直すため、R-24）。
 */
class RecordRepository(
    private val db: IkumemoDatabase,
    private val normalizer: ProductNameNormalizer = ProductNameNormalizer(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val onRemindersChanged: suspend () -> Unit = {},
) {
    private val recordDao = db.purchaseRecordDao()
    private val tripDao = db.shoppingTripDao()
    private val reminderDao = db.reminderDao()
    private val productDao = db.productDao()
    private val products = ProductRepository(db, normalizer, clock)
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

    /**
     * メモに入れていなかった物を、選んだ日に買った物として記録する（F-17）。メモは変えない。
     * 初めての品名は品目として登録し、品目の購入回数などを計算し直す（R-12）。品名が空なら何もせず null を返す。
     */
    suspend fun addRecord(record: NewRecord): Long? {
        val displayName = record.name.trim()
        if (displayName.isEmpty()) return null
        return db.withTransaction {
            val product = products.findOrCreate(displayName)
            val recordId =
                recordDao.insert(
                    PurchaseRecordEntity(
                        tripId = findOrCreateTrip(record.purchasedDate, record.storeId),
                        productId = product.id,
                        displayName = displayName,
                        quantity = record.quantity?.trim()?.ifEmpty { null },
                        note = record.note?.trim()?.ifEmpty { null },
                        photoUri = record.photoUri,
                        purchasedDate = record.purchasedDate,
                        recordedAt = clock.instant(),
                        storeId = record.storeId,
                    ),
                )
            statsUpdater.recalculate(product.id)
            recordId
        }
    }

    /**
     * 記録を消す。紐づくおしらせも消し（外部キーの CASCADE）、品目の集計を計算し直す（R-12）。
     * 元に戻すため、消した記録・おしらせ・お買い物を返す（F-16）。記録が無ければ null。写真のファイルは消さない（R-14 の掃除に任せる）。
     */
    suspend fun deleteRecord(recordId: Long): DeletedRecord? {
        val deleted =
            db.withTransaction {
                val record = recordDao.getById(recordId) ?: return@withTransaction null
                val reminder = reminderDao.findByRecord(recordId)
                val snapshot = DeletedRecord(record, reminder, tripDao.getById(record.tripId))
                recordDao.delete(record)
                tripDao.deleteUnused()
                statsUpdater.recalculate(record.productId)
                snapshot
            }
        if (deleted?.reminder != null) onRemindersChanged()
        return deleted
    }

    /**
     * 消した記録を元に戻す（F-16）。記録・おしらせを同じ id で戻し、品目の集計を計算し直し、次の通知を予約し直す。
     * 品目が無くなっていたり、同じ id の記録がもうあれば何もせず false を返す。
     */
    suspend fun restoreRecord(deleted: DeletedRecord): Boolean {
        val record = deleted.record
        val restored =
            db.withTransaction {
                if (recordDao.getById(record.id) != null || productDao.getById(record.productId) == null) {
                    return@withTransaction false
                }
                recordDao.insert(record.copy(tripId = restoreTrip(deleted)))
                deleted.reminder?.let { reminder ->
                    if (reminderDao.findByRecord(record.id) == null) reminderDao.insert(reminder)
                }
                statsUpdater.recalculate(record.productId)
                true
            }
        if (restored && deleted.reminder != null) onRemindersChanged()
        return restored
    }

    /** 元のお買い物が残っていればそれを、無ければ同じ日・お店のもの、それも無ければ元のお買い物を同じ id で戻す。 */
    private suspend fun restoreTrip(deleted: DeletedRecord): Long {
        val record = deleted.record
        return tripDao.getById(record.tripId)?.id
            ?: tripDao.find(record.purchasedDate, record.storeId)?.id
            ?: tripDao.insert(
                deleted.trip ?: ShoppingTripEntity(date = record.purchasedDate, storeId = record.storeId),
            )
    }

    suspend fun findOrCreateTrip(
        date: LocalDate,
        storeId: Long?,
    ): Long = tripDao.find(date, storeId)?.id ?: tripDao.insert(ShoppingTripEntity(date = date, storeId = storeId))

    companion object {
        const val SEARCH_LIMIT = 200
    }
}
