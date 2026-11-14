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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        records = RecordRepository(db, ProductNameNormalizer(), clock)
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
                RecordEdit(LocalDate.of(2026, 9, 21), storeId = null, quantity = " ２本 ", note = "特売", photoUri = null),
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
    fun undoingDeleteRestoresRecordAndReminderWithSameIdsAndStats() =
        runTest {
            // BL-135。消して元に戻すと、記録とおしらせが同じ id で戻り、集計も消す前と同じになる
            var rescheduled = 0
            records = RecordRepository(db, ProductNameNormalizer(), clock) { rescheduled += 1 }
            val (_, second) = buyMilkOn(0, 6)
            val before = milk()
            val recordBefore = requireNotNull(records.getRecord(second))
            val reminderId =
                db.reminderDao().insert(
                    ReminderEntity(
                        purchaseRecordId = second,
                        productId = before.id,
                        dueDate = LocalDate.of(2026, 10, 14),
                        snoozedUntil = LocalDate.of(2026, 10, 15),
                    ),
                )
            val reminderBefore = db.reminderDao().getById(reminderId)

            val deleted = requireNotNull(records.deleteRecord(second))
            assertNull(db.shoppingTripDao().find(LocalDate.of(2026, 10, 7), null))
            assertEquals(1, rescheduled)

            assertTrue(records.restoreRecord(deleted))

            assertEquals(recordBefore, records.getRecord(second))
            assertEquals(reminderBefore, db.reminderDao().findByRecord(second))
            assertEquals(recordBefore.tripId, db.shoppingTripDao().find(LocalDate.of(2026, 10, 7), null)?.id)
            assertEquals(before, milk())
            assertEquals(2, rescheduled)
            // 2 回目は戻すものが無いので何もしない
            assertEquals(false, records.restoreRecord(deleted))
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

    @Test
    fun addingRecordOnAnyDateUpdatesPurchaseCountWithoutTouchingMemo() =
        runTest {
            // F-17（BL-043 の完了条件）。メモに入れていない物を過去の日付で記録する
            buyMilkOn(0)
            val store = db.storeDao().insert(StoreEntity(name = "○○スーパー", createdAt = clock.instant()))
            val memoItem = (shopping.addItem("パン") as AddItemResult.Added).itemId

            val recordId =
                records.addRecord(
                    NewRecord(" ぎゅうにゅう ", LocalDate.of(2026, 9, 21), store, " 二本 ", "", photoUri = null),
                )

            val record = requireNotNull(records.getRecord(requireNotNull(recordId)))
            assertEquals(milk().id, record.productId)
            assertEquals("ぎゅうにゅう", record.displayName)
            assertEquals("2本", record.quantity)
            assertNull(record.note)
            assertEquals(store, record.storeId)
            assertEquals(record.tripId, db.shoppingTripDao().find(LocalDate.of(2026, 9, 21), store)?.id)
            assertEquals(2, milk().purchaseCount)
            assertEquals(LocalDate.of(2026, 10, 1), milk().lastPurchasedAt)
            assertEquals(10.0, milk().avgIntervalDays ?: Double.NaN, 0.0001)
            // メモは変わらない
            assertNotNull(db.shoppingItemDao().getById(memoItem))
            assertNull(db.shoppingItemDao().findByProductId(milk().id))
        }

    @Test
    fun addingRecordOfNewNameRegistersProduct() =
        runTest {
            val recordId = records.addRecord(NewRecord("アイス", LocalDate.of(2026, 10, 3), null, null, null, null))

            val product = requireNotNull(db.productDao().findByNormalizedName("あいす"))
            assertEquals(product.id, records.getRecord(requireNotNull(recordId))?.productId)
            assertEquals(1, product.purchaseCount)
            assertEquals(LocalDate.of(2026, 10, 3), product.lastPurchasedAt)
        }

    @Test
    fun blankNameIsNotRecorded() =
        runTest {
            assertNull(records.addRecord(NewRecord("  ", LocalDate.of(2026, 10, 3), null, null, null, null)))
            assertTrue(db.productDao().getAllNames().isEmpty())
        }
}
