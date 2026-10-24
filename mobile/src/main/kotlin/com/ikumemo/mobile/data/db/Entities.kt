package com.ikumemo.mobile.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * お店（PLAN §4.3）。消すときは行を消さず [archived] を立てる（R-16）。過去の記録のお店名を残すため。
 */
@Entity(tableName = "store")
data class StoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconType: String? = null,
    val color: Long? = null,
    val lastUsedAt: Instant? = null,
    val archived: Boolean = false,
    val createdAt: Instant,
)

/**
 * 品目（候補の元）。[normalizedName] は core/text の正規化名で、同じ物の判定に使う（R-02）。
 * 購入回数・最後に買った日・平均間隔は記録から計算し直す値（R-12）。
 */
@Entity(
    tableName = "product",
    indices = [Index(value = ["normalizedName"], unique = true)],
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val defaultQuantity: String? = null,
    val defaultExpiryDays: Int? = null,
    val purchaseCount: Int = 0,
    val lastPurchasedAt: LocalDate? = null,
    val avgIntervalDays: Double? = null,
    val hidden: Boolean = false,
    val createdAt: Instant,
)

/** これから買う物（メモの 1 行）。 */
@Entity(
    tableName = "shopping_item",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["plannedStoreId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("productId"), Index("plannedStoreId")],
)
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val displayName: String,
    val quantity: String? = null,
    val note: String? = null,
    val plannedStoreId: Long? = null,
    val photoUri: String? = null,
    val sortOrder: Int,
    val createdAt: Instant,
)

/**
 * お買い物 1 回（同じ日・同じお店）。F-15 は v1.x だが、後からの移行を避けるため MVP から記録時に自動で作る。
 */
@Entity(
    tableName = "shopping_trip",
    foreignKeys = [
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["storeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["date", "storeId"])],
)
data class ShoppingTripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val storeId: Long? = null,
    val note: String? = null,
    val photoUri: String? = null,
)

/** 買った記録。[purchasedDate] は「買った」にした時点の端末のローカル日付（R-10）。 */
@Entity(
    tableName = "purchase_record",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingTripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["storeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("tripId"),
        Index("productId"),
        Index("storeId"),
        Index("purchasedDate"),
    ],
)
data class PurchaseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val productId: Long,
    val displayName: String,
    val quantity: String? = null,
    val note: String? = null,
    val photoUri: String? = null,
    val purchasedDate: LocalDate,
    val recordedAt: Instant,
    val storeId: Long? = null,
)

/** おしらせの状態。DB には [name] を文字列で保存する。 */
enum class ReminderStatus {
    /** 未対応 */
    PENDING,

    /** 使い切った */
    USED_UP,

    /** 捨てた */
    DISCARDED,
}

/** おしらせ（期限）。記録を消すと紐づくおしらせも消える（R-12、外部キーの CASCADE）。 */
@Entity(
    tableName = "reminder",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseRecordId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["purchaseRecordId"], unique = true),
        Index("productId"),
        Index("dueDate"),
    ],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseRecordId: Long,
    val productId: Long,
    val dueDate: LocalDate,
    @ColumnInfo(defaultValue = "PENDING") val status: ReminderStatus = ReminderStatus.PENDING,
    val snoozedUntil: LocalDate? = null,
)
