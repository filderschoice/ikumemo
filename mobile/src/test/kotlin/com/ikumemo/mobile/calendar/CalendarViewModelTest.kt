package com.ikumemo.mobile.calendar

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.RecordEdit
import com.ikumemo.mobile.data.RecordRepository
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.MutableClock
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import com.ikumemo.mobile.testing.testSettings
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class CalendarViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var clock: MutableClock
    private lateinit var shopping: ShoppingRepository
    private lateinit var stores: StoreRepository
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        clock = fixedClock("2026-10-25T01:00:00Z")
        val normalizer = ProductNameNormalizer()
        shopping = ShoppingRepository(db, normalizer, clock)
        stores = StoreRepository(db, testSettings(), normalizer, clock)
        viewModel = CalendarViewModel(RecordRepository(db, normalizer), stores, clock)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        db.close()
    }

    /** 日本時間の [date] に [name] を買った記録を作る。 */
    private suspend fun buy(
        name: String,
        date: LocalDate,
        storeId: Long? = null,
    ): Long {
        val now = clock.now
        clock.now = Instant.parse("${date}T01:00:00Z")
        val itemId = (shopping.addItem(name) as AddItemResult.Added).itemId
        val recordId = (shopping.markBought(itemId, storeId) as com.ikumemo.mobile.data.UndoableAction.Bought).recordId
        clock.now = now
        return recordId
    }

    private suspend fun ReceiveTurbine<CalendarUiState>.awaitUntil(
        predicate: (CalendarUiState) -> Boolean,
    ): CalendarUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }

    @Test
    fun selectingDateListsThatDaysRecordsWithStoreNames() =
        runTest {
            val store = requireNotNull(stores.addStore("○○スーパー"))
            buy("牛乳", LocalDate.of(2026, 10, 8), store.id)
            buy("たまご", LocalDate.of(2026, 10, 8))
            buy("パン", LocalDate.of(2026, 10, 12))

            viewModel.uiState.test {
                val month = awaitUntil { it.shoppingDays.size == 2 }
                assertEquals(YearMonth.of(2026, 10), month.month)
                assertEquals(LocalDate.of(2026, 10, 25), month.selectedDate)
                assertEquals(listOf(store.id, null), month.shoppingDays[LocalDate.of(2026, 10, 8)])

                viewModel.onEvent(CalendarEvent.SelectDate(LocalDate.of(2026, 10, 8)))

                val day = awaitUntil { it.selectedRecords.size == 2 }
                assertEquals(
                    listOf("牛乳" to "○○スーパー", "たまご" to null),
                    day.selectedRecords.map { it.record.displayName to it.storeName },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun searchResultJumpsToThatDayInOtherMonth() =
        runTest {
            buy("卵", LocalDate.of(2026, 8, 3))
            buy("牛乳", LocalDate.of(2026, 10, 1))

            viewModel.uiState.test {
                viewModel.onEvent(CalendarEvent.OpenSearch)
                viewModel.onEvent(CalendarEvent.QueryChanged("タマゴ"))
                val found = awaitUntil { it.searchResults.isNotEmpty() }
                assertEquals(listOf("卵"), found.searchResults.map { it.record.displayName })

                viewModel.onEvent(CalendarEvent.JumpTo(found.searchResults.single().record.purchasedDate))

                val jumped = awaitUntil { !it.searchOpen && it.selectedRecords.isNotEmpty() }
                assertEquals(YearMonth.of(2026, 8), jumped.month)
                assertEquals(LocalDate.of(2026, 8, 3), jumped.selectedDate)
                assertEquals("卵", jumped.selectedRecords.single().record.displayName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun monthNavigationSelectsFirstDayExceptCurrentMonth() =
        runTest {
            viewModel.uiState.test {
                viewModel.onEvent(CalendarEvent.PreviousMonth)
                assertEquals(LocalDate.of(2026, 9, 1), awaitUntil { it.month == YearMonth.of(2026, 9) }.selectedDate)
                viewModel.onEvent(CalendarEvent.NextMonth)
                assertEquals(LocalDate.of(2026, 10, 25), awaitUntil { it.month == YearMonth.of(2026, 10) }.selectedDate)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun editingAndDeletingRecordFromDayDetail() =
        runTest {
            val record = buy("牛乳", LocalDate.of(2026, 10, 25))

            viewModel.uiState.test {
                awaitUntil { it.selectedRecords.size == 1 }
                viewModel.onEvent(CalendarEvent.EditRecord(record))
                assertEquals(record, awaitUntil { it.editing != null }.editing?.id)

                viewModel.onEvent(
                    CalendarEvent.SaveRecord(record, RecordEdit(LocalDate.of(2026, 10, 24), null, "2本", "", null)),
                )
                val yesterday = LocalDate.of(2026, 10, 24)
                val moved =
                    awaitUntil { it.editing == null && it.selectedDate == yesterday && it.selectedRecords.isNotEmpty() }
                assertEquals("2本", moved.selectedRecords.single().record.quantity)

                viewModel.onEvent(CalendarEvent.DeleteRecord(record))
                val deleted = awaitUntil { it.selectedRecords.isEmpty() }
                assertNull(deleted.editing)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
