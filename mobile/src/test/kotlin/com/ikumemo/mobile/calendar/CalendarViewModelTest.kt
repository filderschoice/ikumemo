package com.ikumemo.mobile.calendar

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.NewRecord
import com.ikumemo.mobile.data.RecordEdit
import com.ikumemo.mobile.data.RecordRepository
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.MutableClock
import com.ikumemo.mobile.testing.TURBINE_TIMEOUT
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
        viewModel = CalendarViewModel(RecordRepository(db, normalizer, clock), stores, clock)
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

            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
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

            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
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
    fun addingRecordClosesSheetAndShowsThatDay() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                viewModel.onEvent(CalendarEvent.OpenAddRecord)
                awaitUntil { it.adding }

                viewModel.onEvent(
                    CalendarEvent.AddRecord(NewRecord("アイス", LocalDate.of(2026, 9, 30), null, null, null, null)),
                )

                val added = awaitUntil { !it.adding && it.selectedRecords.isNotEmpty() }
                assertEquals(YearMonth.of(2026, 9), added.month)
                assertEquals(LocalDate.of(2026, 9, 30), added.selectedDate)
                assertEquals(listOf("アイス"), added.selectedRecords.map { it.record.displayName })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun monthNavigationSelectsFirstDayExceptCurrentMonth() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
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

            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
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

    @Test
    fun deletingOffersUndoThatRestoresRecord() =
        runTest {
            // BL-135。消すと元に戻すが出て、押すと同じ記録が戻る
            val record = buy("牛乳", LocalDate.of(2026, 10, 25))

            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                val before = awaitUntil { it.selectedRecords.size == 1 }.selectedRecords.single().record
                viewModel.onEvent(CalendarEvent.EditRecord(record))
                awaitUntil { it.editing != null }

                viewModel.onEvent(CalendarEvent.DeleteRecord(record))
                val deleted = awaitUntil { it.selectedRecords.isEmpty() && it.undo != null }
                assertNull(deleted.editing)
                assertEquals("牛乳", deleted.undo?.deleted?.record?.displayName)

                viewModel.onEvent(CalendarEvent.UndoDelete(requireNotNull(deleted.undo).serial))
                val restored = awaitUntil { it.selectedRecords.isNotEmpty() }
                assertNull(restored.undo)
                assertEquals(before, restored.selectedRecords.single().record)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun undoOfferDisappearsWhenExpired() =
        runTest {
            // BL-135。時間が過ぎると元に戻すは消え、記録は消えたまま。古い番号の元に戻すは効かない
            val record = buy("牛乳", LocalDate.of(2026, 10, 25))

            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.selectedRecords.size == 1 }
                viewModel.onEvent(CalendarEvent.DeleteRecord(record))
                val serial = requireNotNull(awaitUntil { it.undo != null && it.selectedRecords.isEmpty() }.undo).serial

                viewModel.onEvent(CalendarEvent.UndoExpired(serial))
                val expired = awaitUntil { it.undo == null }
                assertEquals(emptyList<RecordRow>(), expired.selectedRecords)

                viewModel.onEvent(CalendarEvent.UndoDelete(serial))
                assertNull(db.purchaseRecordDao().getById(record))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun closingEditorWithoutChangesKeepsRecord() =
        runTest {
            val store = requireNotNull(stores.addStore("○○スーパー"))
            val recordId = buy("牛乳", LocalDate.of(2026, 10, 20), store.id)

            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                viewModel.onEvent(CalendarEvent.SelectDate(LocalDate.of(2026, 10, 20)))
                viewModel.onEvent(CalendarEvent.EditRecord(recordId))
                val before = requireNotNull(awaitUntil { it.editing != null }.editing)

                // シートを何も変えずに閉じたときに送られる内容（RecordEditSheet は null を空文字で持つ）
                viewModel.onEvent(
                    CalendarEvent.SaveRecord(
                        recordId,
                        RecordEdit(
                            before.purchasedDate,
                            before.storeId,
                            before.quantity.orEmpty(),
                            before.note.orEmpty(),
                            before.photoUri,
                        ),
                    ),
                )

                val closed = awaitUntil { it.editing == null && it.selectedRecords.isNotEmpty() }
                assertEquals(LocalDate.of(2026, 10, 20), closed.selectedDate)
                assertEquals(before, closed.selectedRecords.single().record)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
