package com.ikumemo.mobile.data

import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.data.db.ReminderEntity
import com.ikumemo.mobile.data.db.StoreEntity
import com.ikumemo.mobile.testing.MutableClock
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class RecordRepositoryTest {
    private lateinit var db: IkumemoDatabase
    private lateinit var clock: MutableClock
    private lateinit var shopping: ShoppingRepository
    private lateinit var records: RecordRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        clock = fixedClock("2026-10-01T01:00:00Z")
        shopping = ShoppingRepository(db, ProductNameNormalizer(), clock)
        records = RecordRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** [days] 日目ごとに「牛乳」を買った記録を作り、記録 ID を返す。 */
    private suspend fun buyMilkOn(vararg days: Long): List<Long> =
        days.map { day ->
            clock.now = java.time.Instant.parse("2026-10-01T01:00:00Z").plusSeconds(day * 86_400)
            val itemId = (shopping.addItem("牛乳") as AddItemResult.Added).itemId
            (requireNotNull(shopping.markBought(itemId)) as UndoableAction.Bought).recordId
        }

    private suspend fun milk() = requireNotNull(db.productDao().findByNormalizedName("ぎゅうにゅう"))

    @Test
    fun editingDateQuantityAndNoteRecalculatesStats() =
        runTest {
            val (first, _, last) = buyMilkOn(0, 10, 20)
            assertEquals(10.0, milk().avgIntervalDays ?: Double.NaN, 0.0001)

            records.updateRecord(
                first,
                RecordEdit(LocalDate.of(2026, 9, 21), storeId = null, quantity = " 2本 ", note = "特売", photoUri = null),
            )

            val edited = requireNotNull(records.getRecord(first))
            assertEquals(LocalDate.of(2026, 9, 21), edited.purchasedDate)
            assertEquals("2本", edited.quantity)
            assertEquals("特売", edited.note)
            assertEquals(LocalDate.of(2026, 9, 21), db.shoppingTripDao().find(LocalDate.of(2026, 9, 21), null)?.date)
            assertEquals(15.0, milk().avgIntervalDays ?: Double.NaN, 0.0001)
            assertEquals(LocalDate.of(2026, 10, 21), milk().lastPurchasedAt)
            assertEquals(3, milk().purchaseCount)
            assertEquals(
                last,
                records.observeBetween(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 21)).first().single().id,
            )
        }

    @Test
    fun deletingRecordRecalculatesStatsAndRemovesReminderAndEmptyTrip() =
        runTest {
            val (first, second) = buyMilkOn(0, 6)
            val product = milk()
            db.reminderDao().insert(
                ReminderEntity(purchaseRecordId = second, productId = product.id, dueDate = LocalDate.of(2026, 10, 14)),
            )

            records.deleteRecord(second)

            assertNull(records.getRecord(second))
            assertNull(db.reminderDao().findByRecord(second))
            assertNull(db.shoppingTripDao().find(LocalDate.of(2026, 10, 7), null))
            assertEquals(1, milk().purchaseCount)
            assertEquals(LocalDate.of(2026, 10, 1), milk().lastPurchasedAt)
            assertNull(milk().avgIntervalDays)

            records.deleteRecord(first)
            assertEquals(0, milk().purchaseCount)
            assertNull(milk().lastPurchasedAt)
        }

    @Test
    fun changingStoreMovesRecordToThatStoresTrip() =
        runTest {
            val (record) = buyMilkOn(0)
            val store = db.storeDao().insert(StoreEntity(name = "○○スーパー", createdAt = clock.instant()))

            records.updateRecord(record, RecordEdit(LocalDate.of(2026, 10, 1), store, null, null, null))

            val trip = requireNotNull(db.shoppingTripDao().find(LocalDate.of(2026, 10, 1), store))
            assertEquals(trip.id, records.getRecord(record)?.tripId)
            assertNull(db.shoppingTripDao().find(LocalDate.of(2026, 10, 1), null))
        }
}
