package com.ikumemo.mobile.data.backup

import com.ikumemo.mobile.data.db.ProductEntity
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ReminderEntity
import com.ikumemo.mobile.data.db.ReminderStatus
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.data.db.ShoppingTripEntity
import com.ikumemo.mobile.data.db.StoreEntity
import java.time.Instant
import java.time.LocalDate

// 書き出しファイルの行 → DB の行（F-72）。検査（BackupValidator）を通ったファイルだけが渡される。

fun BackupStore.toEntity() =
    StoreEntity(
        id,
        name,
        iconType,
        color,
        lastUsedAt?.let(Instant::ofEpochMilli),
        archived,
        Instant.ofEpochMilli(createdAt),
    )

fun BackupProduct.toEntity() =
    ProductEntity(
        id, name, normalizedName, defaultQuantity, defaultExpiryDays, purchaseCount,
        lastPurchasedAt?.let(LocalDate::ofEpochDay), avgIntervalDays, hidden, Instant.ofEpochMilli(createdAt),
    )

fun BackupItem.toEntity() =
    ShoppingItemEntity(
        id, productId, displayName, quantity, note, plannedStoreId, photoUri, sortOrder,
        Instant.ofEpochMilli(
            createdAt,
        ),
    )

fun BackupTrip.toEntity() = ShoppingTripEntity(id, LocalDate.ofEpochDay(date), storeId, note, photoUri)

fun BackupRecord.toEntity() =
    PurchaseRecordEntity(
        id, tripId, productId, displayName, quantity, note, photoUri,
        LocalDate.ofEpochDay(purchasedDate), Instant.ofEpochMilli(recordedAt), storeId,
    )

fun BackupReminder.toEntity() =
    ReminderEntity(
        id,
        purchaseRecordId,
        productId,
        LocalDate.ofEpochDay(dueDate),
        ReminderStatus.entries.first { it.name == status },
        snoozedUntil?.let(LocalDate::ofEpochDay),
    )
