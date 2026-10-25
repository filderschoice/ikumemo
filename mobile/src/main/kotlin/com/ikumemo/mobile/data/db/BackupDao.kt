package com.ikumemo.mobile.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * 書き出し・読み込み（F-72）用の DAO。すべての表を ID の順に読み、ID を保ったまま入れ直す。
 * detekt の関数数の上限に収めるため、読む・入れる・消すの 3 つに分けている。
 */
@Dao
interface BackupReadDao {
    @Query("SELECT * FROM store ORDER BY id")
    suspend fun stores(): List<StoreEntity>

    @Query("SELECT * FROM product ORDER BY id")
    suspend fun products(): List<ProductEntity>

    @Query("SELECT * FROM shopping_item ORDER BY id")
    suspend fun items(): List<ShoppingItemEntity>

    @Query("SELECT * FROM shopping_trip ORDER BY id")
    suspend fun trips(): List<ShoppingTripEntity>

    @Query("SELECT * FROM purchase_record ORDER BY id")
    suspend fun records(): List<PurchaseRecordEntity>

    @Query("SELECT * FROM reminder ORDER BY id")
    suspend fun reminders(): List<ReminderEntity>
}

/** 読み込みで入れる。親（お店・品目）を先に、子（記録・期限）を後に入れる。 */
@Dao
interface BackupInsertDao {
    @Insert
    suspend fun insertStores(stores: List<StoreEntity>)

    @Insert
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert
    suspend fun insertItems(items: List<ShoppingItemEntity>)

    @Insert
    suspend fun insertTrips(trips: List<ShoppingTripEntity>)

    @Insert
    suspend fun insertRecords(records: List<PurchaseRecordEntity>)

    @Insert
    suspend fun insertReminders(reminders: List<ReminderEntity>)
}

/** 読み込みの前に全部消す。 */
@Dao
interface BackupDeleteDao {
    @Query("DELETE FROM reminder")
    suspend fun deleteReminders()

    @Query("DELETE FROM purchase_record")
    suspend fun deleteRecords()

    @Query("DELETE FROM shopping_trip")
    suspend fun deleteTrips()

    @Query("DELETE FROM shopping_item")
    suspend fun deleteItems()

    @Query("DELETE FROM product")
    suspend fun deleteProducts()

    @Query("DELETE FROM store")
    suspend fun deleteStores()

    /** 子から先に消す（外部キーの CASCADE に頼らず、消す順をここで決める）。 */
    suspend fun deleteAll() {
        deleteReminders()
        deleteRecords()
        deleteTrips()
        deleteItems()
        deleteProducts()
        deleteStores()
    }
}
