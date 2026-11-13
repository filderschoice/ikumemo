package com.ikumemo.mobile.shoppinglist

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.ikumemo.mobile.calendar.CalendarTestTags
import com.ikumemo.mobile.calendar.MonthGrid
import com.ikumemo.mobile.calendar.MonthHeader
import com.ikumemo.mobile.data.db.ReminderEntity
import com.ikumemo.mobile.data.db.ReminderRow
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.reminder.ReminderContent
import com.ikumemo.mobile.reminder.ReminderTestTags
import com.ikumemo.mobile.reminder.ReminderUiState
import com.ikumemo.mobile.settings.FeedbackContent
import com.ikumemo.mobile.settings.FeedbackTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** アクセシビリティの仕上げ（BL-085 の完了条件）。読み上げで主要な操作ができ、アイコンに説明がある。 */
@RunWith(RobolectricTestRunner::class)
class AccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val milk =
        ShoppingItemEntity(
            id = 1,
            productId = 1,
            displayName = "牛乳",
            photoUri = "photos/milk.jpg",
            sortOrder = 0,
            createdAt = Instant.EPOCH,
        )

    @Test
    fun swipeRowOffersBoughtAndDismissedActions() {
        val events = mutableStateListOf<String>()
        composeRule.setContent {
            IkumemoTheme {
                SwipeableItemRow(
                    item = milk,
                    highlighted = false,
                    onClick = {},
                    onBought = { events += "bought" },
                    onDismissed = { events += "dismissed" },
                )
            }
        }

        // スワイプの代わりに、読み上げの操作の一覧から「買った」「やめた」を選べる
        val actions =
            composeRule
                .onNodeWithTag(TestTags.item(milk.id))
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
        assertEquals(listOf("買った", "やめた"), actions.map { it.label })
        actions.forEach { it.action() }
        composeRule.waitForIdle()

        assertEquals(listOf("bought", "dismissed"), events.toList())
    }

    @Test
    fun rowIconsHaveDescriptions() {
        composeRule.setContent {
            IkumemoTheme {
                SwipeableItemRow(
                    item = milk,
                    highlighted = false,
                    onClick = {},
                    onBought = {},
                    onDismissed = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("牛乳を買った").assertExists()
        composeRule.onNodeWithContentDescription("写真あり").assertExists()
    }

    @Test
    fun calendarMonthButtonsHaveDescriptions() {
        composeRule.setContent {
            IkumemoTheme {
                MonthHeader(
                    month = YearMonth.of(2026, 10),
                    onPrevious = {},
                    onNext = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("前の月").assertExists()
        composeRule.onNodeWithContentDescription("次の月").assertExists()
    }

    @Test
    fun calendarSelectedDayIsAnnouncedAsSelected() {
        val selected = LocalDate.of(2026, 10, 15)
        val other = LocalDate.of(2026, 10, 16)
        composeRule.setContent {
            IkumemoTheme {
                MonthGrid(
                    month = YearMonth.of(2026, 10),
                    selectedDate = selected,
                    today = LocalDate.of(2026, 10, 1),
                    shoppingDays = emptyMap(),
                    onSelect = {},
                )
            }
        }

        // 選ばれている日は背景色だけでなく、読み上げでも選択中と分かる（BL-115）
        composeRule.onNodeWithTag(CalendarTestTags.day(selected)).assertIsSelected()
        composeRule.onNodeWithTag(CalendarTestTags.day(other)).assertIsNotSelected()
    }

    @Test
    fun feedbackQuestionAnnouncesOpenStateAndAction() {
        composeRule.setContent {
            IkumemoTheme {
                FeedbackContent(deviceInfo = "端末", onCopyDeviceInfo = {}, onOpenGitHub = {})
            }
        }
        val question = composeRule.onNodeWithTag(FeedbackTestTags.faq(0))

        // 閉じているときは「閉じている」状態と「答えを開く」操作が読まれる（BL-115）
        assertEquals("答えは閉じています", question.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertEquals("答えを開く", question.fetchSemanticsNode().config[SemanticsActions.OnClick].label)

        question.performClick()
        composeRule.waitForIdle()

        assertEquals("答えを表示中", question.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertEquals("答えを閉じる", question.fetchSemanticsNode().config[SemanticsActions.OnClick].label)
    }

    @Test
    fun reminderPendingRowAnnouncesRedoDueAction() {
        val today = LocalDate.of(2026, 10, 15)
        val row =
            ReminderRow(
                ReminderEntity(id = 1, purchaseRecordId = 3, productId = 3, dueDate = today.plusDays(1)),
                "ヨーグルト",
            )
        composeRule.setContent {
            IkumemoTheme {
                ReminderContent(
                    state = ReminderUiState(today = today, pending = listOf(row), loaded = true),
                    onEvent = {},
                    onEnableNotifications = {},
                    showGuide = false,
                )
            }
        }

        // 行を押す操作が「期限を決め直す」と読まれる（BL-115）
        val label =
            composeRule
                .onNodeWithTag(ReminderTestTags.pending(row.reminder.id))
                .fetchSemanticsNode()
                .config[SemanticsActions.OnClick]
                .label
        assertEquals("期限を決め直す", label)
    }
}
