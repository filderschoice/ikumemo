package com.ikumemo.mobile.data.backup

import com.ikumemo.mobile.data.db.ReminderStatus
import kotlinx.serialization.Serializable

/**
 * 書き出しファイルの中身（F-72）。DB の各表を ID ごと写す。日付は epoch day、日時は epoch ミリ秒で持つ。
 * 写真は `photos/<ファイル名>`、背景画像は `background/<ファイル名>` の相対パスで、ZIP の同じパスにファイルを入れる。
 * JSON だけで書き出したとき（写真なし）は写真のパスをすべて空にする。
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val stores: List<BackupStore> = emptyList(),
    val products: List<BackupProduct> = emptyList(),
    val items: List<BackupItem> = emptyList(),
    val trips: List<BackupTrip> = emptyList(),
    val records: List<BackupRecord> = emptyList(),
    val reminders: List<BackupReminder> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
) {
    companion object {
        const val FORMAT = "ikumemo-backup"
        const val VERSION = 1
    }
}

@Serializable
data class BackupStore(
    val id: Long,
    val name: String,
    val iconType: String? = null,
    val color: Long? = null,
    val lastUsedAt: Long? = null,
    val archived: Boolean = false,
    val createdAt: Long,
)

@Serializable
data class BackupProduct(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val defaultQuantity: String? = null,
    val defaultExpiryDays: Int? = null,
    val purchaseCount: Int = 0,
    val lastPurchasedAt: Long? = null,
    val avgIntervalDays: Double? = null,
    val hidden: Boolean = false,
    val createdAt: Long,
)

@Serializable
data class BackupItem(
    val id: Long,
    val productId: Long,
    val displayName: String,
    val quantity: String? = null,
    val note: String? = null,
    val plannedStoreId: Long? = null,
    val photoUri: String? = null,
    val sortOrder: Int,
    val createdAt: Long,
)

@Serializable
data class BackupTrip(
    val id: Long,
    val date: Long,
    val storeId: Long? = null,
    val note: String? = null,
    val photoUri: String? = null,
)

@Serializable
data class BackupRecord(
    val id: Long,
    val tripId: Long,
    val productId: Long,
    val displayName: String,
    val quantity: String? = null,
    val note: String? = null,
    val photoUri: String? = null,
    val purchasedDate: Long,
    val recordedAt: Long,
    val storeId: Long? = null,
)

@Serializable
data class BackupReminder(
    val id: Long,
    val purchaseRecordId: Long,
    val productId: Long,
    val dueDate: Long,
    val status: String = ReminderStatus.PENDING.name,
    val snoozedUntil: Long? = null,
)

/** 見た目・操作・おしらせの設定。ガイドの表示済みやお店の選択は端末ごとの状態なので入れない。 */
@Serializable
data class BackupSettings(
    val theme: String? = null,
    val darkMode: String? = null,
    val textSize: String? = null,
    val scrimLevel: Float? = null,
    val backgroundImage: String? = null,
    val showBoughtButton: Boolean? = null,
    val vibration: Boolean? = null,
    val reminderTimeMinutes: Int? = null,
    val notifyOnDueDate: Boolean? = null,
)
