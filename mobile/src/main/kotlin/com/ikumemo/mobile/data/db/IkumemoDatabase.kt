package com.ikumemo.mobile.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * いくメモの DB（PLAN §4.3 の 6 テーブル）。スキーマ JSON は mobile/schemas に出力して Git 管理し、
 * 版を上げるときは AutoMigration とマイグレーションテストで記録が消えないことを確かめる。
 */
@Database(
    entities = [
        StoreEntity::class,
        ProductEntity::class,
        ShoppingItemEntity::class,
        ShoppingTripEntity::class,
        PurchaseRecordEntity::class,
        ReminderEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class IkumemoDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao

    abstract fun productDao(): ProductDao

    abstract fun shoppingItemDao(): ShoppingItemDao

    abstract fun shoppingTripDao(): ShoppingTripDao

    abstract fun purchaseRecordDao(): PurchaseRecordDao

    abstract fun reminderDao(): ReminderDao

    abstract fun photoDao(): PhotoDao

    abstract fun backupReadDao(): BackupReadDao

    abstract fun backupInsertDao(): BackupInsertDao

    abstract fun backupDeleteDao(): BackupDeleteDao

    companion object {
        const val FILE_NAME = "ikumemo.db"

        fun create(context: Context): IkumemoDatabase =
            Room
                .databaseBuilder(context.applicationContext, IkumemoDatabase::class.java, FILE_NAME)
                .build()
    }
}
