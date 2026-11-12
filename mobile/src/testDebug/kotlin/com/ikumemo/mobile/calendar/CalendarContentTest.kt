package com.ikumemo.mobile.calendar

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.designsystem.LocalHasBackgroundImage
import com.ikumemo.mobile.designsystem.ReadablePanelTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun show(
        state: CalendarUiState = CalendarSamples.state(),
        hasBackgroundImage: Boolean = false,
    ) {
        val photoStore = PhotoStore(context)
        composeRule.setContent {
            IkumemoTheme {
                CompositionLocalProvider(LocalHasBackgroundImage provides hasBackgroundImage) {
                    CalendarContent(
                        state = state,
                        photoStore = photoStore,
                        onEvent = { events += it },
                        showGuide = false,
                    )
                }
            }
        }
    }

    private fun panelCount(): Int =
        composeRule.onAllNodesWithTag(ReadablePanelTestTags.PANEL, useUnmergedTree = true).fetchSemanticsNodes().size

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

    @Test
    fun backgroundImageLaysPanelsUnderTheMonthAndTheDay() {
        val row = CalendarSamples.row(id = 7, name = "牛乳")
        show(CalendarSamples.state(selectedRecords = listOf(row)), hasBackgroundImage = true)

        // 月の表の日付・記録の行・「買った物を記録する」が面の上に描かれる（BL-130）
        assertOnPanel(CalendarTestTags.day(CalendarSamples.TODAY))
        assertOnPanel(CalendarTestTags.record(7))
        assertOnPanel(CalendarTestTags.ADD_RECORD)
    }

    @Test
    fun backgroundImageLaysPanelsUnderTheSearch() {
        val rows = listOf(CalendarSamples.row(id = 3, name = "卵"), CalendarSamples.row(id = 4, name = "卵焼き"))
        show(CalendarSamples.state(searchOpen = true, query = "卵", searchResults = rows), hasBackgroundImage = true)

        // 検索の欄と結果の 2 行（BL-130）
        assertEquals(3, panelCount())
        assertOnPanel(CalendarTestTags.SEARCH_INPUT, scroll = false)
        assertOnPanel(CalendarTestTags.result(4), scroll = false)
    }

    /** [tag] の要素まで一覧を送り（[scroll]）、その全体がどれかの面の内側にあることを確かめる。 */
    private fun assertOnPanel(
        tag: String,
        scroll: Boolean = true,
    ) {
        // 一覧は LazyColumn のため、画面外の行は作られない。送ってから探す
        if (scroll) composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val panels =
            composeRule
                .onAllNodesWithTag(ReadablePanelTestTags.PANEL, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .map { it.boundsInRoot }
        assertTrue("$tag が面の上に無い", panels.any { it.intersect(bounds) == bounds })
    }

    @Test
    fun noPanelsWithoutBackgroundImage() {
        show(CalendarSamples.state(selectedRecords = listOf(CalendarSamples.row(id = 7, name = "牛乳"))))

        assertEquals(0, panelCount())
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
