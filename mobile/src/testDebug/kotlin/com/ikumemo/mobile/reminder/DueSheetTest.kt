package com.ikumemo.mobile.reminder

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.R
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * 期限を決めるシート（F-50、BL-132）。候補のチップは「1週間後」と日付（曜日付き）の 2 行、「日付を選ぶ」は 1 行。
 *
 * ボトムシートは別のウィンドウに描かれ、Robolectric では座標でのタップが届かないため、クリック処理を直接呼ぶ。
 */
@RunWith(RobolectricTestRunner::class)
class DueSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableStateListOf<ReminderEvent>()
    private val today = LocalDate.of(2026, 11, 13)
    private val target = DueTarget(recordId = 7, name = "牛乳")

    private fun show() {
        composeRule.setContent {
            IkumemoTheme {
                DueSheet(target = target, today = today, onEvent = { events += it })
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun choicesShowLabelAndDateWithWeekday() {
        show()
        val expected =
            mapOf(
                DueChoice.THREE_DAYS to (R.string.reminder_due_3days to "11月16日（月）"),
                DueChoice.ONE_WEEK to (R.string.reminder_due_1week to "11月20日（金）"),
                DueChoice.TWO_WEEKS to (R.string.reminder_due_2weeks to "11月27日（金）"),
                DueChoice.ONE_MONTH to (R.string.reminder_due_1month to "12月13日（日）"),
            )
        expected.forEach { (choice, texts) ->
            composeRule
                .onNodeWithTag(ReminderTestTags.choice(choice))
                .assert(hasText(context.getString(texts.first)))
                .assert(hasText(texts.second))
                .assertHeightIsAtLeast(48.dp)
        }
        // 「日付を選ぶ」は日付を添えない 1 行のまま（チップの文字はこれだけ）
        val pick = context.getString(R.string.reminder_due_pick)
        composeRule.onNodeWithText(pick).assertTextEquals(pick)
    }

    @Test
    fun choiceSetsDueCountedFromToday() {
        show()
        composeRule
            .onNodeWithTag(ReminderTestTags.choice(DueChoice.ONE_MONTH))
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals(listOf(ReminderEvent.SetDue(7, LocalDate.of(2026, 12, 13))), events.toList())
    }
}
