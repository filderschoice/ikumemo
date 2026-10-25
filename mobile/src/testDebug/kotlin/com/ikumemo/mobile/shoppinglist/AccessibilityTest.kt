package com.ikumemo.mobile.shoppinglist

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.ikumemo.mobile.calendar.MonthHeader
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
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
}
