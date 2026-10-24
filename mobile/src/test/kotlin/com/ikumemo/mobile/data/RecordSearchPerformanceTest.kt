package com.ikumemo.mobile.data

import androidx.room.withTransaction
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.data.db.ProductEntity
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ShoppingTripEntity
import com.ikumemo.mobile.testing.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

/** PLAN §8「5 年分（購入記録 2 万件）でもカレンダー・検索が 1 秒以内」（BL-042 の完了条件）。 */
@RunWith(RobolectricTestRunner::class)
class RecordSearchPerformanceTest {
    private lateinit var db: IkumemoDatabase
    private val normalizer = ProductNameNormalizer()
    private val names = listOf("牛乳", "卵", "食パン", "納豆", "豆腐", "バナナ", "洗剤", "トイレットペーパー", "人参", "玉ねぎ")

    @Before
    fun setUp() {
        db = inMemoryDatabase()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertFiveYears() {
        val now = Instant.parse("2026-10-25T01:00:00Z")
        val start = LocalDate.of(2021, 10, 26)
        db.withTransaction {
            val products =
                (0 until PRODUCT_COUNT).map { index ->
                    val name = names[index % names.size] + if (index < names.size) "" else index.toString()
                    db.productDao().insert(
                        ProductEntity(name = name, normalizedName = normalizer.normalize(name), createdAt = now),
                    )
                }
            var day = 0L
            repeat(RECORD_COUNT / RECORDS_PER_DAY) {
                val date = start.plusDays(day++)
                val trip = db.shoppingTripDao().insert(ShoppingTripEntity(date = date))
                repeat(RECORDS_PER_DAY) { i ->
                    val productIndex = (day.toInt() * RECORDS_PER_DAY + i) % products.size
                    db.purchaseRecordDao().insert(
                        PurchaseRecordEntity(
                            tripId = trip,
                            productId = products[productIndex],
                            displayName = "x",
                            purchasedDate = date,
                            recordedAt = now,
                        ),
                    )
                }
            }
        }
    }

    @Test
    fun searchAndMonthQueryFinishWithinOneSecondFor20000Records() =
        runTest {
            insertFiveYears()
            val repository = RecordRepository(db, normalizer)

            val searchMillis = measure { repository.search("ぎゅうにゅう") }
            val monthMillis =
                measure { repository.observeBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).first() }

            assertEquals(RecordRepository.SEARCH_LIMIT, repository.search("ぎゅうにゅう").size)
            assertTrue("検索 ${searchMillis}ms", searchMillis < LIMIT_MILLIS)
            assertTrue("月の記録 ${monthMillis}ms", monthMillis < LIMIT_MILLIS)
        }

    private suspend fun measure(block: suspend () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / NANOS_PER_MILLI
    }

    private companion object {
        const val RECORD_COUNT = 20_000
        const val RECORDS_PER_DAY = 11
        const val PRODUCT_COUNT = 300
        const val LIMIT_MILLIS = 1_000L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
