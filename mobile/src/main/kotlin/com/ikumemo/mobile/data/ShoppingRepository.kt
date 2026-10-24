package com.ikumemo.mobile.data

import androidx.room.withTransaction
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.data.db.ProductEntity
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.data.db.ShoppingTripEntity
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDate

/** メモへ追加した結果。 */
sealed interface AddItemResult {
    data class Added(
        val itemId: Long,
    ) : AddItemResult

    /** すでにメモにある（R-01）。新しく増やさず、既存の項目を示す。 */
    data class AlreadyInMemo(
        val itemId: Long,
        val displayName: String,
    ) : AddItemResult

    /** 品名が空。 */
    data object Blank : AddItemResult
}

/** 元に戻せる操作（R-04 元に戻せるのは最新の 1 件）。 */
sealed interface UndoableAction {
    val item: ShoppingItemEntity

    data class Bought(
        override val item: ShoppingItemEntity,
        val recordId: Long,
    ) : UndoableAction

    data class Dismissed(
        override val item: ShoppingItemEntity,
    ) : UndoableAction
}

/** メモ（これから買う物）の読み書き。品目（候補の元）の作成もここで行う。 */
class ShoppingRepository(
    private val db: IkumemoDatabase,
    private val normalizer: ProductNameNormalizer,
    private val clock: Clock,
) {
    private val itemDao = db.shoppingItemDao()
    private val productDao = db.productDao()
    private val tripDao = db.shoppingTripDao()
    private val recordDao = db.purchaseRecordDao()
    private val statsUpdater = ProductStatsUpdater(db)

    fun observeItems(): Flow<List<ShoppingItemEntity>> = itemDao.observeAll()

    suspend fun getItem(id: Long): ShoppingItemEntity? = itemDao.getById(id)

    /**
     * メモの末尾へ追加する。正規化した品名が同じ物がメモにあれば追加しない（R-01・R-02）。
     * 初めての品名は品目として登録し、その表記を代表名にする（PLAN §4.4）。
     */
    suspend fun addItem(
        name: String,
        quantity: String? = null,
    ): AddItemResult {
        val displayName = name.trim()
        if (displayName.isEmpty()) return AddItemResult.Blank
        return db.withTransaction {
            val product = findOrCreateProduct(displayName)
            val existing = itemDao.findByProductId(product.id)
            if (existing != null) {
                AddItemResult.AlreadyInMemo(existing.id, existing.displayName)
            } else {
                val id =
                    itemDao.insert(
                        ShoppingItemEntity(
                            productId = product.id,
                            displayName = displayName,
                            quantity = quantity?.trim()?.ifEmpty { null } ?: product.defaultQuantity,
                            sortOrder = itemDao.maxSortOrder() + 1,
                            createdAt = clock.instant(),
                        ),
                    )
                AddItemResult.Added(id)
            }
        }
    }

    /**
     * 編集シートの内容を保存する（F-04）。品名を変えた結果、メモにある別の項目と同じ物になる場合は
     * 品名だけ変えずに [AddItemResult.AlreadyInMemo] を返す（R-01）。
     */
    suspend fun updateItem(
        itemId: Long,
        name: String,
        quantity: String?,
        note: String?,
    ): AddItemResult =
        db.withTransaction {
            val item = itemDao.getById(itemId) ?: return@withTransaction AddItemResult.Blank
            val displayName = name.trim()
            var result: AddItemResult = AddItemResult.Added(itemId)
            var updated = item.copy(quantity = quantity.blankToNull(), note = note.blankToNull())
            if (displayName.isNotEmpty() && displayName != item.displayName) {
                val product = findOrCreateProduct(displayName)
                val other = itemDao.findByProductId(product.id)?.takeIf { it.id != itemId }
                if (other != null) {
                    result = AddItemResult.AlreadyInMemo(other.id, other.displayName)
                } else {
                    updated = updated.copy(productId = product.id, displayName = displayName)
                }
            }
            itemDao.update(updated)
            result
        }

    /**
     * 「買った」にする（F-10・F-13）。今日の日付（R-10）で記録し、メモから消す。同じ日に同じ物を買っても別の記録にする（R-11）。
     * 写真は同じファイルを記録から参照する（R-15）。[storeId] は選んでいるいつものお店（F-14）。
     */
    suspend fun markBought(
        itemId: Long,
        storeId: Long? = null,
    ): UndoableAction? =
        db.withTransaction {
            val item = itemDao.getById(itemId) ?: return@withTransaction null
            val today = LocalDate.now(clock)
            val tripId =
                tripDao.find(today, storeId)?.id
                    ?: tripDao.insert(ShoppingTripEntity(date = today, storeId = storeId))
            val recordId =
                recordDao.insert(
                    PurchaseRecordEntity(
                        tripId = tripId,
                        productId = item.productId,
                        displayName = item.displayName,
                        quantity = item.quantity,
                        note = item.note,
                        photoUri = item.photoUri,
                        purchasedDate = today,
                        recordedAt = clock.instant(),
                        storeId = storeId,
                    ),
                )
            itemDao.delete(item)
            statsUpdater.recalculate(item.productId)
            UndoableAction.Bought(item, recordId)
        }

    /** 「やめた」にする（F-11）。記録には残さず、品目（候補）は残す（R-03）。 */
    suspend fun dismiss(itemId: Long): UndoableAction? =
        db.withTransaction {
            val item = itemDao.getById(itemId) ?: return@withTransaction null
            itemDao.delete(item)
            UndoableAction.Dismissed(item)
        }

    /** 直前の「買った」「やめた」を元に戻す（F-12）。メモの同じ位置へ戻し、「買った」の記録は消す。 */
    suspend fun undo(action: UndoableAction) {
        db.withTransaction {
            if (action is UndoableAction.Bought) {
                recordDao.getById(action.recordId)?.let { recordDao.delete(it) }
                tripDao.deleteUnused()
                statsUpdater.recalculate(action.item.productId)
            }
            if (itemDao.getById(action.item.id) == null && itemDao.findByProductId(action.item.productId) == null) {
                itemDao.insert(action.item)
            }
        }
    }

    suspend fun findOrCreateProduct(displayName: String): ProductEntity {
        val normalized = normalizer.normalize(displayName)
        productDao.findByNormalizedName(normalized)?.let { return it }
        val product = ProductEntity(name = displayName, normalizedName = normalized, createdAt = clock.instant())
        return product.copy(id = productDao.insert(product))
    }

    private fun String?.blankToNull(): String? = this?.trim()?.ifEmpty { null }
}
