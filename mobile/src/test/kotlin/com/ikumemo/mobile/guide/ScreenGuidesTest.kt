package com.ikumemo.mobile.guide

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.calendar.CalendarEvent
import com.ikumemo.mobile.calendar.CalendarViewModel
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.GuidePrefs
import com.ikumemo.mobile.data.RecordRepository
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.data.ReminderRepository
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.UndoableAction
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.reminder.DueTarget
import com.ikumemo.mobile.reminder.ReminderEvent
import com.ikumemo.mobile.reminder.ReminderViewModel
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.TURBINE_TIMEOUT
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import com.ikumemo.mobile.testing.testDataStore
import com.ikumemo.mobile.testing.testSettings
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
import java.time.LocalDate

/** カレンダーとおしらせのその場のガイド（G-07・G-08、BL-097 の完了条件）。 */
@RunWith(RobolectricTestRunner::class)
class ScreenGuidesTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var shopping: ShoppingRepository
    private lateinit var guidePrefs: GuidePrefs
    private val clock = fixedClock("2026-10-25T01:00:00Z")
    private val normalizer = ProductNameNormalizer()
    private var calendar: CalendarViewModel? = null
    private var reminder: ReminderViewModel? = null

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        shopping = ShoppingRepository(db, normalizer, clock)
        guidePrefs = GuidePrefs(testDataStore())
        runBlocking { guidePrefs.finishOnboarding() }
    }

    @After
    fun tearDown() {
        calendar?.viewModelScope?.cancel()
        reminder?.viewModelScope?.cancel()
        db.close()
    }

    private fun calendar() =
        CalendarViewModel(
            RecordRepository(db, normalizer, clock),
            StoreRepository(db, testSettings(), normalizer, clock),
            clock,
            guidePrefs,
        ).also { calendar = it }

    private fun reminder() =
        ReminderViewModel(ReminderRepository(db, clock), ReminderPrefs(testDataStore()), clock, guidePrefs)
            .also { reminder = it }

    private suspend fun buy(name: String): Long {
        val itemId = (shopping.addItem(name) as AddItemResult.Added).itemId
        return (shopping.markBought(itemId) as UndoableAction.Bought).recordId
    }

    private suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }

    private suspend fun shown() = guidePrefs.state.first().shown

    @Test
    fun calendarGuideAppearsOnlyWhenShoppingDayIsVisibleAndOnce() =
        runTest {
            val viewModel = calendar()
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                // 買い物のあった日が無ければ照らす物が無いので出さない
                assertNull(awaitItem().guide)
                buy("牛乳")
                assertEquals(GuideId.G07_CALENDAR_DAY, awaitUntil { it.guide != null }.guide)

                // R-52: 検索を開いている間は出さない
                viewModel.onEvent(CalendarEvent.OpenSearch)
                assertNull(awaitUntil { it.searchOpen }.guide)
                viewModel.onEvent(CalendarEvent.CloseSearch)
                awaitUntil { !it.searchOpen && it.guide != null }

                viewModel.onEvent(CalendarEvent.GuideDismissed(GuideId.G07_CALENDAR_DAY))
                awaitUntil { it.guide == null }
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(GuideId.G07_CALENDAR_DAY in shown())
        }

    @Test
    fun selectingDateFirstMeansCalendarGuideIsNotNeeded() =
        runTest {
            buy("牛乳")
            val viewModel = calendar()
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.guide == GuideId.G07_CALENDAR_DAY }
                // R-51: 自分で日付を押して中身を見た
                viewModel.onEvent(CalendarEvent.SelectDate(LocalDate.of(2026, 10, 25)))
                awaitUntil { it.guide == null }
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(GuideId.G07_CALENDAR_DAY in shown())
        }

    @Test
    fun reminderGuideAppearsAfterFirstPurchaseAndStopsWithDisableAll() =
        runTest {
            val viewModel = reminder()
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                // まだ何も買っていなければ出さない
                assertNull(awaitUntil { it.loaded }.guide)
                val recordId = buy("牛乳")
                assertEquals(GuideId.G08_DUE_DATE, awaitUntil { it.recent.isNotEmpty() }.guide)

                // R-52: 期限の設定シートを出している間は出さない
                viewModel.onEvent(ReminderEvent.OpenDue(DueTarget(recordId, "牛乳")))
                assertNull(awaitUntil { it.dueTarget != null }.guide)
                viewModel.onEvent(ReminderEvent.CloseDue)
                // R-51: 自分で「期限を決める」を押したので、閉じても出さない
                assertNull(awaitUntil { it.dueTarget == null && it.guide == null }.guide)
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(GuideId.G08_DUE_DATE in shown())
        }

    @Test
    fun disableAllStopsReminderGuide() =
        runTest {
            buy("牛乳")
            val viewModel = reminder()
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.guide == GuideId.G08_DUE_DATE }
                viewModel.onEvent(ReminderEvent.GuideDisableAll(GuideId.G08_DUE_DATE))
                // 「出さない」と表示済みは 1 回で保存するため、ガイドが消えた時には止める設定も保存済み
                awaitUntil { it.guide == null }
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(guidePrefs.state.first().disabled)
        }
}
