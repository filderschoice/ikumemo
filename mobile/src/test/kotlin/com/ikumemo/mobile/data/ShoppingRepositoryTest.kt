package com.ikumemo.mobile.data

import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ShoppingRepositoryTest {
    private lateinit var db: IkumemoDatabase
    private val clock = fixedClock("2026-10-24T15:30:00Z") // 日本時間 10 月 25 日 0:30
    private lateinit var repository: ShoppingRepository
    private val today = LocalDate.of(2026, 10, 25)

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = ShoppingRepository(db, ProductNameNormalizer(), clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun add(name: String): Long = (repository.addItem(name) as AddItemResult.Added).itemId

    private suspend fun recordsToday() = db.purchaseRecordDao().observeBetween(today, today).first()

    @Test
    fun markBoughtRecordsTodayAndRemovesFromMemo() =
        runTest {
            val id = add("牛乳")

            val action = repository.markBought(id)

            assertNotNull(action)
            assertEquals(emptyList<Any>(), repository.observeItems().first())
            val record = recordsToday().single()
            assertEquals("牛乳", record.displayName)
            assertEquals(today, record.purchasedDate)
            val product = db.productDao().getById(record.productId)
            assertEquals(1, product?.purchaseCount)
            assertEquals(today, product?.lastPurchasedAt)
        }

    @Test
    fun boughtTwiceSameDayMakesTwoRecordsInOneTrip() =
        runTest {
            repository.markBought(add("牛乳"))
            repository.markBought(add("牛乳"))

            val records = recordsToday()
            assertEquals(2, records.size)
            assertEquals(1, records.map { it.tripId }.distinct().size)
            assertEquals(2, db.productDao().getById(records[0].productId)?.purchaseCount)
        }

    @Test
    fun dismissKeepsProductButMakesNoRecord() =
        runTest {
            val id = add("洗剤")

            repository.dismiss(id)

            assertEquals(emptyList<Any>(), repository.observeItems().first())
            assertEquals(emptyList<Any>(), recordsToday())
            assertNotNull(db.productDao().findByNormalizedName("せんざい"))
        }

    @Test
    fun undoBoughtRestoresItemAtSamePositionAndRemovesRecord() =
        runTest {
            add("牛乳")
            val egg = add("たまご")
            add("パン")
            val action = requireNotNull(repository.markBought(egg))

            repository.undo(action)

            assertEquals(listOf("牛乳", "たまご", "パン"), repository.observeItems().first().map { it.displayName })
            assertEquals(emptyList<Any>(), recordsToday())
            val product = db.productDao().findByNormalizedName("たまご")
            assertEquals(0, product?.purchaseCount)
            assertNull(product?.lastPurchasedAt)
        }

    @Test
    fun undoDismissedRestoresItem() =
        runTest {
            val id = add("洗剤")
            val action = requireNotNull(repository.dismiss(id))

            repository.undo(action)

            assertEquals(listOf(id), repository.observeItems().first().map { it.id })
        }

    @Test
    fun undoDoesNotDuplicateWhenSameProductWasAddedAgain() =
        runTest {
            val action = requireNotNull(repository.dismiss(add("洗剤")))
            add("せんざい")

            repository.undo(action)

            assertEquals(1, repository.observeItems().first().size)
        }
}
