package com.ikumemo.mobile.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** 読み込むファイルの検査（F-72）。外から来た入力を DB に触る前に弾けること。 */
class BackupValidatorTest {
    private val store = BackupStore(id = 1, name = "店", createdAt = 0)
    private val product = BackupProduct(id = 1, name = "牛乳", normalizedName = "ぎゅうにゅう", createdAt = 0)
    private val trip = BackupTrip(id = 1, date = 20_000, storeId = 1)
    private val record =
        BackupRecord(
            id = 1,
            tripId = 1,
            productId = 1,
            displayName = "牛乳",
            purchasedDate = 20_000,
            recordedAt = 0,
            storeId = 1,
        )
    private val valid =
        BackupFile(
            exportedAt = 0,
            stores = listOf(store),
            products = listOf(product),
            items =
                listOf(
                    BackupItem(
                        id = 1,
                        productId = 1,
                        displayName = "牛乳",
                        sortOrder = 0,
                        createdAt = 0,
                        plannedStoreId = 1,
                    ),
                ),
            trips = listOf(trip),
            records = listOf(record),
            reminders = listOf(BackupReminder(id = 1, purchaseRecordId = 1, productId = 1, dueDate = 20_002)),
        )

    @Test
    fun acceptsConsistentFile() {
        assertNull(BackupValidator.validate(valid))
    }

    @Test
    fun rejectsOtherFormatAndVersion() {
        assertNotNull(BackupValidator.validate(valid.copy(format = "other")))
        assertNotNull(BackupValidator.validate(valid.copy(version = 2)))
    }

    @Test
    fun rejectsDuplicateIdsAndNames() {
        assertNotNull(BackupValidator.validate(valid.copy(stores = listOf(store, store))))
        assertNotNull(BackupValidator.validate(valid.copy(products = listOf(product, product.copy(id = 2)))))
        val second = valid.reminders.single().copy(id = 2)
        assertNotNull(BackupValidator.validate(valid.copy(reminders = valid.reminders + second)))
    }

    @Test
    fun rejectsMissingReferences() {
        assertNotNull(BackupValidator.validate(valid.copy(records = listOf(record.copy(tripId = 9)))))
        assertNotNull(BackupValidator.validate(valid.copy(trips = listOf(trip.copy(storeId = 9)))))
        val reminder = valid.reminders.single()
        assertNotNull(BackupValidator.validate(valid.copy(reminders = listOf(reminder.copy(purchaseRecordId = 9)))))
    }

    @Test
    fun rejectsUnsafePhotoPathsAndBadValues() {
        assertNotNull(BackupValidator.validate(valid.copy(records = listOf(record.copy(photoUri = "photos/../x.jpg")))))
        assertNotNull(BackupValidator.validate(valid.copy(records = listOf(record.copy(photoUri = "/data/x.jpg")))))
        assertNotNull(BackupValidator.validate(valid.copy(settings = BackupSettings(backgroundImage = "photos/a.jpg"))))
        val reminder = valid.reminders.single()
        assertNotNull(BackupValidator.validate(valid.copy(reminders = listOf(reminder.copy(status = "ZZZ")))))
        val farFuture = record.copy(purchasedDate = Long.MAX_VALUE)
        assertNotNull(BackupValidator.validate(valid.copy(records = listOf(farFuture))))
        assertNotNull(BackupValidator.validate(valid.copy(settings = BackupSettings(reminderTimeMinutes = 24 * 60))))
    }

    @Test
    fun acceptsSafePhotoPaths() {
        val withPhoto = valid.copy(records = listOf(record.copy(photoUri = "photos/0a1b-2c.jpg")))
        val withBackground = withPhoto.copy(settings = BackupSettings(backgroundImage = "background/bg.jpg"))
        assertNull(BackupValidator.validate(withBackground))
        assertEquals(true, BackupValidator.isPhotoPath("photos/a.jpg"))
        assertEquals(false, BackupValidator.isPhotoPath("photos/sub/a.jpg"))
    }
}
