package com.ikumemo.mobile.data.backup

import com.ikumemo.mobile.data.db.ProductEntity
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ReminderEntity
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.data.db.ShoppingTripEntity
import com.ikumemo.mobile.data.db.StoreEntity

// DB の行 → 書き出しファイルの行（F-72）。

fun StoreEntity.toBackup() =
    BackupStore(id, name, iconType, color, lastUsedAt?.toEpochMilli(), archived, createdAt.toEpochMilli())

fun ProductEntity.toBackup() =
    BackupProduct(
        id, name, normalizedName, defaultQuantity, defaultExpiryDays, purchaseCount,
        lastPurchasedAt?.toEpochDay(), avgIntervalDays, hidden, createdAt.toEpochMilli(),
    )

fun ShoppingItemEntity.toBackup() =
    BackupItem(
        id, productId, displayName, quantity, note, plannedStoreId, photoUri, sortOrder, createdAt.toEpochMilli(),
    )

fun ShoppingTripEntity.toBackup() = BackupTrip(id, date.toEpochDay(), storeId, note, photoUri)

fun PurchaseRecordEntity.toBackup() =
    BackupRecord(
        id, tripId, productId, displayName, quantity, note, photoUri,
        purchasedDate.toEpochDay(), recordedAt.toEpochMilli(), storeId,
    )

fun ReminderEntity.toBackup() =
    BackupReminder(id, purchaseRecordId, productId, dueDate.toEpochDay(), status.name, snoozedUntil?.toEpochDay())
