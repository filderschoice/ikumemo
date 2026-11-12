package com.ikumemo.mobile.calendar

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * カレンダーの画面（F-40〜F-42）の操作。[CalendarContent] に状態を渡して描き、押したときに出るイベントを確かめる。
 *
 * 文言やレイアウトが変わっても壊れにくいよう、要素はテストタグ（[CalendarTestTags]）で探し、
 * 文言で確かめるときは文字列リソース（[text]）から引く。テストを足すときは [CalendarSamples] で状態を組み立て、[show] で描く。
 */
@RunWith(RobolectricTestRunner::class)
class CalendarContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableStateListOf<CalendarEvent>()

    private fun show(state: CalendarUiState = CalendarSamples.state()) {
        val photoStore = PhotoStore(context)
        composeRule.setContent {
            IkumemoTheme {
                CalendarContent(state = state, photoStore = photoStore, onEvent = { events += it }, showGuide = false)
            }
        }
    }

    private fun text(
        @StringRes id: Int,
        vararg args: Any,
    ): String = context.getString(id, *args)

    @Test
    fun tappingADaySelectsIt() {
        show()
        val day = CalendarSamples.TODAY.plusDays(2)

        composeRule.onNodeWithTag(CalendarTestTags.day(day)).performClick()

        assertEquals(listOf<CalendarEvent>(CalendarEvent.SelectDate(day)), events.toList())
    }

    @Test
    fun dayWithoutShoppingSaysSo() {
        show(CalendarSamples.state(selectedRecords = emptyList()))

        composeRule.onNodeWithText(text(R.string.calendar_no_shopping)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dayWithShoppingListsTheRecordsAndOpensTheEditor() {
        val row = CalendarSamples.row(id = 7, name = "牛乳", storeName = "駅前スーパー")
        show(CalendarSamples.state(selectedRecords = listOf(row)))

        composeRule.onNodeWithText(text(R.string.calendar_no_shopping)).assertDoesNotExist()
        composeRule.onNodeWithTag(CalendarTestTags.record(7)).performScrollTo().performClick()

        assertEquals(listOf<CalendarEvent>(CalendarEvent.EditRecord(7)), events.toList())
    }

    @Test
    fun tappingASearchResultJumpsToItsDay() {
        val date = CalendarSamples.TODAY.minusMonths(2)
        val row = CalendarSamples.row(id = 3, name = "卵", date = date)
        show(CalendarSamples.state(searchOpen = true, query = "卵", searchResults = listOf(row)))

        composeRule.onNodeWithTag(CalendarTestTags.result(3)).performClick()

        assertEquals(listOf<CalendarEvent>(CalendarEvent.JumpTo(date)), events.toList())
    }

    @Test
    fun searchWithoutResultsSaysSo() {
        show(CalendarSamples.state(searchOpen = true, query = "見つからない品"))

        composeRule.onNodeWithText(text(R.string.calendar_search_empty)).assertIsDisplayed()
    }
}

/** カレンダーの画面のテスト用の状態。既定は「今日を選び、記録なし、検索は閉じている」。 */
internal object CalendarSamples {
    val TODAY: LocalDate = LocalDate.of(2026, 11, 13)
    private val RECORDED_AT: Instant = Instant.parse("2026-11-13T01:00:00Z")

    fun state(
        selectedDate: LocalDate = TODAY,
        selectedRecords: List<RecordRow> = emptyList(),
        shoppingDays: Map<LocalDate, List<Long?>> = emptyMap(),
        searchOpen: Boolean = false,
        query: String = "",
        searchResults: List<RecordRow> = emptyList(),
    ): CalendarUiState =
        CalendarUiState(
            month = YearMonth.from(selectedDate),
            selectedDate = selectedDate,
            today = TODAY,
            shoppingDays = shoppingDays,
            selectedRecords = selectedRecords,
            searchOpen = searchOpen,
            query = query,
            searchResults = searchResults,
        )

    fun row(
        id: Long,
        name: String,
        date: LocalDate = TODAY,
        storeName: String? = null,
    ): RecordRow =
        RecordRow(
            record =
                PurchaseRecordEntity(
                    id = id,
                    tripId = 1,
                    productId = id,
                    displayName = name,
                    purchasedDate = date,
                    recordedAt = RECORDED_AT,
                ),
            storeName = storeName,
        )
}
