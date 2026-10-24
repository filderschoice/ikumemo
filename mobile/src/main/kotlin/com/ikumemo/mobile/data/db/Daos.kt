package com.ikumemo.mobile.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface StoreDao {
    @Insert
    suspend fun insert(store: StoreEntity): Long

    @Update
    suspend fun update(store: StoreEntity)

    @Query("SELECT * FROM store WHERE id = :id")
    suspend fun getById(id: Long): StoreEntity?

    @Query("SELECT * FROM store WHERE name = :name AND archived = 0 LIMIT 1")
    suspend fun findActiveByName(name: String): StoreEntity?

    /** いつものお店の候補（F-31）。使わない印のお店を除き、最近使った順に並べる。 */
    @Query(
        "SELECT * FROM store WHERE archived = 0 " +
            "ORDER BY lastUsedAt IS NULL, lastUsedAt DESC, createdAt DESC",
    )
    fun observeActive(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM store")
    fun observeAll(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM store")
    suspend fun getAll(): List<StoreEntity>
}

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM product WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): ProductEntity?

    /** 品目の表示名（音声の分割で品名の中の「と」を守るのに使う）。 */
    @Query("SELECT name FROM product")
    suspend fun getAllNames(): List<String>

    /** 候補に出す品目（非表示を除く）。 */
    @Query("SELECT * FROM product WHERE hidden = 0")
    fun observeVisible(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product")
    fun observeAll(): Flow<List<ProductEntity>>
}

@Dao
interface ShoppingItemDao {
    @Insert
    suspend fun insert(item: ShoppingItemEntity): Long

    @Update
    suspend fun update(item: ShoppingItemEntity)

    @Delete
    suspend fun delete(item: ShoppingItemEntity)

    @Query("SELECT * FROM shopping_item WHERE id = :id")
    suspend fun getById(id: Long): ShoppingItemEntity?

    @Query("SELECT * FROM shopping_item WHERE productId = :productId LIMIT 1")
    suspend fun findByProductId(productId: Long): ShoppingItemEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM shopping_item")
    suspend fun maxSortOrder(): Int

    /** メモの並び順（ウィジェットも同じ順で表示する、PLAN §2.11）。 */
    @Query("SELECT * FROM shopping_item ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_item ORDER BY sortOrder, id")
    suspend fun getAll(): List<ShoppingItemEntity>

    @Query("UPDATE shopping_item SET plannedStoreId = :into WHERE plannedStoreId = :from")
    suspend fun movePlannedStore(
        from: Long,
        into: Long,
    )
}

@Dao
interface ShoppingTripDao {
    @Insert
    suspend fun insert(trip: ShoppingTripEntity): Long

    /** 同じ日・同じお店のお買い物（お店なし同士も同じとみなすため `IS` で比べる）。 */
    @Query("SELECT * FROM shopping_trip WHERE date = :date AND storeId IS :storeId LIMIT 1")
    suspend fun find(
        date: LocalDate,
        storeId: Long?,
    ): ShoppingTripEntity?

    @Query("SELECT * FROM shopping_trip WHERE storeId = :storeId")
    suspend fun getByStore(storeId: Long): List<ShoppingTripEntity>

    @Query("UPDATE shopping_trip SET storeId = :storeId WHERE id = :tripId")
    suspend fun updateStore(
        tripId: Long,
        storeId: Long,
    )

    /** どの記録からも使われなくなったお買い物を消す。 */
    @Query("DELETE FROM shopping_trip WHERE id NOT IN (SELECT tripId FROM purchase_record)")
    suspend fun deleteUnused()
}

@Dao
interface PurchaseRecordDao {
    @Insert
    suspend fun insert(record: PurchaseRecordEntity): Long

    @Update
    suspend fun update(record: PurchaseRecordEntity)

    @Delete
    suspend fun delete(record: PurchaseRecordEntity)

    @Query("SELECT * FROM purchase_record WHERE id = :id")
    suspend fun getById(id: Long): PurchaseRecordEntity?

    @Query("UPDATE purchase_record SET storeId = :into WHERE storeId = :from")
    suspend fun moveStore(
        from: Long,
        into: Long,
    )

    @Query("UPDATE purchase_record SET tripId = :into WHERE tripId = :from")
    suspend fun moveTrip(
        from: Long,
        into: Long,
    )

    /** 品目の購入日（古い順）。購入回数・最後に買った日・平均間隔の計算に使う（R-12）。 */
    @Query("SELECT purchasedDate FROM purchase_record WHERE productId = :productId ORDER BY purchasedDate")
    suspend fun purchaseDatesOf(productId: Long): List<LocalDate>

    @Query(
        "SELECT * FROM purchase_record WHERE purchasedDate BETWEEN :from AND :to " +
            "ORDER BY purchasedDate, recordedAt",
    )
    fun observeBetween(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<PurchaseRecordEntity>>
}

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("SELECT * FROM reminder WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminder WHERE purchaseRecordId = :purchaseRecordId")
    suspend fun findByRecord(purchaseRecordId: Long): ReminderEntity?

    /** 未対応のおしらせ（期限が近い順、F-53）。 */
    @Query("SELECT * FROM reminder WHERE status = 'PENDING' ORDER BY dueDate, id")
    fun observePending(): Flow<List<ReminderEntity>>
}
