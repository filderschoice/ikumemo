package com.ikumemo.mobile.reminder

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.ReminderRepository
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.UndoableAction
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.data.db.ReminderStatus
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.MutableClock
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

/** BL-051 の完了条件「期限の設定・使い切った・捨てたが DB に反映される」。 */
@RunWith(RobolectricTestRunner::class)
class ReminderViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var clock: MutableClock
    private lateinit var shopping: ShoppingRepository
    private lateinit var viewModel: ReminderViewModel
    private val today = LocalDate.of(2026, 10, 25)

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        clock = fixedClock("2026-10-25T01:00:00Z")
        shopping = ShoppingRepository(db, ProductNameNormalizer(), clock)
        viewModel = ReminderViewModel(ReminderRepository(db, clock), clock)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        db.close()
    }

    private suspend fun buy(
        name: String,
        date: LocalDate = today,
    ): Long {
        val now = clock.now
        clock.now = Instant.parse("${date}T01:00:00Z")
        val itemId = (shopping.addItem(name) as AddItemResult.Added).itemId
        val recordId = (shopping.markBought(itemId) as UndoableAction.Bought).recordId
        clock.now = now
        return recordId
    }

    private suspend fun ReceiveTurbine<ReminderUiState>.awaitUntil(
        predicate: (ReminderUiState) -> Boolean,
    ): ReminderUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }

    @Test
    fun recentShowsLastSevenDaysWithoutDue() =
        runTest {
            buy("牛乳")
            buy("パン", today.minusDays(6))
            buy("古い物", today.minusDays(7))

            viewModel.uiState.test {
                val state = awaitUntil { it.loaded && it.recent.size == 2 }
                assertEquals(listOf("牛乳", "パン"), state.recent.map { it.displayName })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun settingDueMovesItemFromRecentToPendingInDueOrder() =
        runTest {
            val milk = buy("牛乳")
            val bread = buy("パン")

            viewModel.uiState.test {
                awaitUntil { it.recent.size == 2 }
                viewModel.onEvent(ReminderEvent.OpenDue(DueTarget(milk, "牛乳")))
                assertEquals(milk, awaitUntil { it.dueTarget != null }.dueTarget?.recordId)

                viewModel.onEvent(ReminderEvent.SetDue(milk, DueChoice.ONE_WEEK.from(today)))
                viewModel.onEvent(ReminderEvent.SetDue(bread, DueChoice.THREE_DAYS.from(today)))

                val state = awaitUntil { it.pending.size == 2 && it.recent.isEmpty() }
                assertNull(state.dueTarget)
                assertEquals(
                    listOf("パン" to today.plusDays(3), "牛乳" to today.plusDays(7)),
                    state.pending.map {
                        it.displayName to it.reminder.dueDate
                    },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun usedUpAndDiscardedAreStoredAndLeaveList() =
        runTest {
            val milk = buy("牛乳")
            val bread = buy("パン")

            viewModel.uiState.test {
                viewModel.onEvent(ReminderEvent.SetDue(milk, today.minusDays(1)))
                viewModel.onEvent(ReminderEvent.SetDue(bread, today.plusDays(2)))
                val pending = awaitUntil { it.pending.size == 2 }.pending
                assertTrue(pending.first().reminder.dueDate < today)

                viewModel.onEvent(ReminderEvent.UsedUp(pending[0].reminder.id))
                viewModel.onEvent(ReminderEvent.Discarded(pending[1].reminder.id))

                awaitUntil { it.pending.isEmpty() }
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(ReminderStatus.USED_UP, db.reminderDao().findByRecord(milk)?.status)
            assertEquals(ReminderStatus.DISCARDED, db.reminderDao().findByRecord(bread)?.status)
        }

    @Test
    fun monthChoiceUsesCalendarMonth() {
        assertEquals(LocalDate.of(2026, 2, 28), DueChoice.ONE_MONTH.from(LocalDate.of(2026, 1, 31)))
        assertEquals(LocalDate.of(2026, 11, 8), DueChoice.TWO_WEEKS.from(today))
    }

    @Test
    fun notificationStateIsShown() =
        runTest {
            viewModel.uiState.test {
                viewModel.onEvent(ReminderEvent.NotificationStateChanged(false))
                assertEquals(false, awaitUntil { it.loaded && !it.notificationsEnabled }.notificationsEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
