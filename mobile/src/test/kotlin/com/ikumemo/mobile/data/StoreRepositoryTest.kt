package com.ikumemo.mobile.data

import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.testing.MutableClock
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import com.ikumemo.mobile.testing.testSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class StoreRepositoryTest {
    private lateinit var db: IkumemoDatabase
    private lateinit var clock: MutableClock
    private lateinit var stores: StoreRepository
    private lateinit var shopping: ShoppingRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        clock = fixedClock("2026-10-25T01:00:00Z") // 日本時間 10:00
        val normalizer = ProductNameNormalizer()
        stores = StoreRepository(db, testSettings(), normalizer, clock)
        shopping = ShoppingRepository(db, normalizer, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun buy(name: String): Long {
        val itemId = (shopping.addItem(name) as AddItemResult.Added).itemId
        val storeId = stores.currentStoreIdForPurchase()
        val action = requireNotNull(shopping.markBought(itemId, storeId)) as UndoableAction.Bought
        storeId?.let { stores.onBought(it) }
        return action.recordId
    }

    @Test
    fun selectedStoreIsRecordedOnBought() =
        runTest {
            val store = requireNotNull(stores.addStore("○○スーパー"))
            stores.select(store.id)

            val record = requireNotNull(db.purchaseRecordDao().getById(buy("牛乳")))

            assertEquals(store.id, record.storeId)
            assertEquals(store.id, stores.observeSelectedStore().first()?.id)
            assertEquals(clock.instant(), db.storeDao().getById(store.id)?.lastUsedAt)
        }

    @Test
    fun selectionIsReleasedThreeHoursAfterSelecting() =
        runTest {
            val store = requireNotNull(stores.addStore("○○スーパー"))
            stores.select(store.id)

            clock.advanceSeconds(3 * 3600 - 60)
            assertEquals(store.id, stores.currentStoreIdForPurchase())
            clock.advanceSeconds(60)

            assertNull(stores.currentStoreIdForPurchase())
            assertNull(db.purchaseRecordDao().getById(buy("牛乳"))?.storeId)
            assertNull(stores.observeSelectedStore().first())
        }

    @Test
    fun boughtRestartsThreeHourWindow() =
        runTest {
            val store = requireNotNull(stores.addStore("○○スーパー"))
            stores.select(store.id)
            clock.advanceSeconds(2 * 3600)
            buy("牛乳")

            clock.advanceSeconds(2 * 3600)

            assertEquals(store.id, stores.currentStoreIdForPurchase())
        }

    @Test
    fun selectionIsReleasedWhenDateChanges() =
        runTest {
            clock.now = java.time.Instant.parse("2026-10-25T14:30:00Z") // 日本時間 23:30
            val store = requireNotNull(stores.addStore("コンビニ"))
            stores.select(store.id)

            clock.advanceSeconds(60 * 60) // 日本時間 翌 0:30

            assertNull(stores.currentStoreIdForPurchase())
        }

    @Test
    fun addStoreReusesSameNameAndRestoresArchived() =
        runTest {
            val first = requireNotNull(stores.addStore("ドラッグＡ"))
            assertEquals(first.id, stores.addStore(" ドラッグA ")?.id)

            stores.archive(first.id)
            assertTrue(stores.observeActiveStores().first().isEmpty())

            val restored = requireNotNull(stores.addStore("ドラッグA"))
            assertEquals(first.id, restored.id)
            assertEquals(false, restored.archived)
            assertNull(stores.addStore("  "))
        }

    @Test
    fun activeStoresAreOrderedByRecentUse() =
        runTest {
            val a = requireNotNull(stores.addStore("A"))
            val b = requireNotNull(stores.addStore("B"))
            stores.select(a.id)
            buy("牛乳")
            clock.advanceSeconds(60)
            stores.select(b.id)
            buy("パン")

            assertEquals(listOf(b.id, a.id), stores.observeActiveStores().first().map { it.id })
        }

    @Test
    fun archivedStoreKeepsNameForPastRecords() =
        runTest {
            val store = requireNotNull(stores.addStore("○○スーパー"))
            stores.select(store.id)
            val record = buy("牛乳")

            stores.archive(store.id)

            assertNull(stores.observeSelectedStore().first())
            val storeId = requireNotNull(db.purchaseRecordDao().getById(record)?.storeId)
            assertEquals("○○スーパー", db.storeDao().getById(storeId)?.name)
        }

    @Test
    fun mergeMovesRecordsAndTripsIntoOneStore() =
        runTest {
            val a = requireNotNull(stores.addStore("スーパーA"))
            val a2 = requireNotNull(stores.addStore("Aスーパー"))
            stores.select(a.id)
            val r1 = buy("牛乳")
            stores.select(a2.id)
            val r2 = buy("パン")

            stores.merge(from = a2.id, into = a.id)

            val today = LocalDate.of(2026, 10, 25)
            val trip = requireNotNull(db.shoppingTripDao().find(today, a.id))
            assertEquals(trip.id, db.purchaseRecordDao().getById(r1)?.tripId)
            assertEquals(trip.id, db.purchaseRecordDao().getById(r2)?.tripId)
            assertEquals(a.id, db.purchaseRecordDao().getById(r2)?.storeId)
            assertNull(db.shoppingTripDao().find(today, a2.id))
            assertEquals(listOf(a.id), stores.observeActiveStores().first().map { it.id })
            assertEquals(a.id, stores.currentStoreIdForPurchase())
        }
}
