package com.ikumemo.mobile.data

import androidx.room.withTransaction
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.data.db.StoreEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.Clock

/** お店の読み書きと、選んでいるいつものお店（F-14・F-30・F-31・R-06・R-16）。 */
class StoreRepository(
    private val db: IkumemoDatabase,
    private val settings: AppSettings,
    private val normalizer: ProductNameNormalizer,
    private val clock: Clock,
) {
    private val storeDao = db.storeDao()
    private val tripDao = db.shoppingTripDao()
    private val recordDao = db.purchaseRecordDao()
    private val itemDao = db.shoppingItemDao()

    /** 使わない印の無いお店（最近使った順）。お店を選ぶシートは先頭 [TOP_STORE_COUNT] 件をチップで出す（F-31）。 */
    fun observeActiveStores(): Flow<List<StoreEntity>> = storeDao.observeActive()

    /**
     * 選んでいるお店。解除の条件（R-06）は時間の経過でも満たされるため、1 分ごとに見直す。
     */
    fun observeSelectedStore(): Flow<StoreEntity?> =
        combine(settings.storeSelection, storeDao.observeAll(), minuteTicks()) { selection, stores, _ ->
            selection
                ?.takeUnless { it.isExpired(clock.instant(), clock.zone) }
                ?.let { s -> stores.firstOrNull { it.id == s.storeId && !it.archived } }
        }.distinctUntilChanged()

    /** 「買った」に付けるお店。解除の条件を満たしていれば解除して null を返す（R-06）。 */
    suspend fun currentStoreIdForPurchase(): Long? {
        val selection = settings.storeSelection.first()
        val expired = selection?.isExpired(clock.instant(), clock.zone) == true
        if (expired) settings.clearStoreSelection()
        return selection
            ?.takeUnless { expired }
            ?.let { storeDao.getById(it.storeId) }
            ?.takeUnless { it.archived }
            ?.id
    }

    suspend fun select(storeId: Long) {
        settings.selectStore(storeId, clock.instant())
    }

    suspend fun clearSelection() {
        settings.clearStoreSelection()
    }

    /** 選んでいるお店で「買った」にした。お店の最近使った日時と、解除までの 3 時間の起点を更新する。 */
    suspend fun onBought(storeId: Long) {
        val now = clock.instant()
        storeDao.getById(storeId)?.let { storeDao.update(it.copy(lastUsedAt = now)) }
        settings.recordBoughtAtSelectedStore(now)
    }

    /**
     * お店を登録する（F-30、名前だけ）。同じ名前（表記ゆれを含む）のお店があればそれを返し、使わない印が付いていれば外す。
     */
    suspend fun addStore(name: String): StoreEntity? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        return db.withTransaction {
            val normalized = normalizer.normalize(trimmed)
            val existing = storeDao.getAll().firstOrNull { normalizer.normalize(it.name) == normalized }
            when {
                existing == null -> {
                    val store = StoreEntity(name = trimmed, createdAt = clock.instant())
                    store.copy(id = storeDao.insert(store))
                }
                existing.archived -> existing.copy(archived = false).also { storeDao.update(it) }
                else -> existing
            }
        }
    }

    /** お店の管理で消したお店。候補からは消すが、過去の記録のお店名を残すため行は消さない（R-16）。 */
    suspend fun archive(storeId: Long) {
        db.withTransaction {
            storeDao.getById(storeId)?.let { storeDao.update(it.copy(archived = true)) }
        }
        val selection = settings.storeSelection.first()
        if (selection?.storeId == storeId) settings.clearStoreSelection()
    }

    /**
     * 同じお店が 2 つできたときに 1 つにまとめる（R-16）。[from] の記録・メモ・お買い物を [into] へ移し、[from] に使わない印を付ける。
     * 同じ日に両方のお店のお買い物があれば 1 つにまとめる。
     */
    suspend fun merge(
        from: Long,
        into: Long,
    ) {
        if (from == into) return
        db.withTransaction {
            tripDao.getByStore(from).forEach { trip ->
                val target = tripDao.find(trip.date, into)
                if (target == null) {
                    tripDao.updateStore(trip.id, into)
                } else {
                    recordDao.moveTrip(trip.id, target.id)
                }
            }
            tripDao.deleteUnused()
            recordDao.moveStore(from, into)
            itemDao.movePlannedStore(from, into)
            storeDao.getById(from)?.let { storeDao.update(it.copy(archived = true)) }
        }
        if (settings.storeSelection.first()?.storeId == from) select(into)
    }

    private fun minuteTicks(): Flow<Unit> =
        flow {
            while (true) {
                emit(Unit)
                delay(TICK_MILLIS)
            }
        }

    companion object {
        const val TOP_STORE_COUNT = 5
        private const val TICK_MILLIS = 60_000L
    }
}
