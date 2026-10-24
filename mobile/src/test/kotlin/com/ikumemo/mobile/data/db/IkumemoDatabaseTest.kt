package com.ikumemo.mobile.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class IkumemoDatabaseTest {
    private lateinit var db: IkumemoDatabase
    private val now = Instant.parse("2026-10-25T01:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, IkumemoDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun product(
        name: String,
        normalizedName: String,
    ): Long = db.productDao().insert(ProductEntity(name = name, normalizedName = normalizedName, createdAt = now))

    private suspend fun record(
        productId: Long,
        date: LocalDate,
    ): Long {
        val trip = db.shoppingTripDao().insert(ShoppingTripEntity(date = date))
        return db.purchaseRecordDao().insert(
            PurchaseRecordEntity(
                tripId = trip,
                productId = productId,
                displayName = "牛乳",
                purchasedDate = date,
                recordedAt = now,
            ),
        )
    }

    @Test
    fun shoppingItemsAreObservedInSortOrder() =
        runTest {
            val milk = product("牛乳", "ぎゅうにゅう")
            val egg = product("たまご", "たまご")
            val dao = db.shoppingItemDao()
            dao.insert(ShoppingItemEntity(productId = egg, displayName = "たまご", sortOrder = 1, createdAt = now))
            dao.insert(
                ShoppingItemEntity(
                    productId = milk,
                    displayName = "牛乳",
                    quantity = "2本",
                    sortOrder = 0,
                    createdAt = now,
                ),
            )

            val items = db.shoppingItemDao().observeAll().first()

            assertEquals(listOf("牛乳", "たまご"), items.map { it.displayName })
            assertEquals(1, db.shoppingItemDao().maxSortOrder())
        }

    @Test
    fun tripIsFoundBySameDateAndStoreIncludingNoStore() =
        runTest {
            val date = LocalDate.of(2026, 10, 25)
            val store = db.storeDao().insert(StoreEntity(name = "○○スーパー", createdAt = now))
            val noStoreTrip = db.shoppingTripDao().insert(ShoppingTripEntity(date = date))
            val storeTrip = db.shoppingTripDao().insert(ShoppingTripEntity(date = date, storeId = store))

            assertEquals(noStoreTrip, db.shoppingTripDao().find(date, null)?.id)
            assertEquals(storeTrip, db.shoppingTripDao().find(date, store)?.id)
            assertNull(db.shoppingTripDao().find(date.plusDays(1), store))
        }

    @Test
    fun deletingRecordAlsoDeletesItsReminder() =
        runTest {
            val date = LocalDate.of(2026, 10, 25)
            val product = product("牛乳", "ぎゅうにゅう")
            val recordId = record(product, date)
            db.reminderDao().insert(
                ReminderEntity(purchaseRecordId = recordId, productId = product, dueDate = date.plusDays(7)),
            )
            assertEquals(1, db.reminderDao().observePending().first().size)

            db.purchaseRecordDao().delete(requireNotNull(db.purchaseRecordDao().getById(recordId)))

            assertEquals(0, db.reminderDao().observePending().first().size)
        }

    @Test
    fun archivedStoreIsHiddenFromCandidatesButKeptForRecords() =
        runTest {
            val used = db.storeDao().insert(StoreEntity(name = "A", lastUsedAt = now, createdAt = now))
            val later = now.plusSeconds(60)
            val recent = db.storeDao().insert(StoreEntity(name = "B", lastUsedAt = later, createdAt = now))
            val never = db.storeDao().insert(StoreEntity(name = "C", createdAt = now))
            val archived = db.storeDao().insert(StoreEntity(name = "D", archived = true, createdAt = now))

            assertEquals(listOf(recent, used, never), db.storeDao().observeActive().first().map { it.id })
            assertEquals("D", db.storeDao().getById(archived)?.name)
        }

    @Test
    fun purchaseDatesAreReturnedOldestFirst() =
        runTest {
            val product = product("牛乳", "ぎゅうにゅう")
            val dates = listOf(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10))
            dates.forEach { record(product, it) }

            assertEquals(dates.sorted(), db.purchaseRecordDao().purchaseDatesOf(product))
        }
}
