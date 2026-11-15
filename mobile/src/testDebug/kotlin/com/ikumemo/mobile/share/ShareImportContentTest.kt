package com.ikumemo.mobile.share

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** BL-197 の完了条件「共有で受け取った文がチェック付きの一覧になり、既にある物は最初から外れている」（Compose UI）。 */
@RunWith(RobolectricTestRunner::class)
class ShareImportContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableStateListOf<ShareImportEvent>()
    private var state by mutableStateOf<ShareImportState>(ShareImportState.Loading)

    private fun show(initial: ShareImportState) {
        state = initial
        composeRule.setContent {
            IkumemoTheme {
                ShareImportContent(
                    state = state,
                    onEvent = { event ->
                        events += event
                        val review = state as? ShareImportState.Review
                        if (event is ShareImportEvent.Toggle && review != null) state = review.toggled(event.key)
                    },
                    onCancel = {},
                )
            }
        }
    }

    private val rows =
        listOf(
            ShareImportRow(0, "牛乳", "2本", alreadyInMemo = false, included = true),
            ShareImportRow(1, "たまご", null, alreadyInMemo = true, included = false),
        )

    @Test
    fun rowsShowCheckStateAndAlreadyInMemo() {
        show(ShareImportState.Review(rows))

        composeRule.onNodeWithTag(ShareImportTestTags.row(0)).assertIsOn()
        composeRule.onNodeWithTag(ShareImportTestTags.row(1)).assertIsOff()
        composeRule.onNodeWithText("メモにあります").assertExists()
        composeRule.onNodeWithText("2本").assertExists()
        composeRule.onNodeWithText("1つ追加する").assertExists()
    }

    @Test
    fun tappingRowTogglesAndAddSendsEvent() {
        show(ShareImportState.Review(rows))

        composeRule.onNodeWithTag(ShareImportTestTags.row(0)).performClick()
        composeRule.onNodeWithTag(ShareImportTestTags.row(0)).assertIsOff()
        composeRule.onNodeWithTag(ShareImportTestTags.ADD).assertIsNotEnabled()

        composeRule.onNodeWithTag(ShareImportTestTags.row(1)).performClick()
        composeRule.onNodeWithTag(ShareImportTestTags.ADD).assertIsEnabled().performClick()

        assertEquals(
            listOf(ShareImportEvent.Toggle(0), ShareImportEvent.Toggle(1), ShareImportEvent.AddSelected),
            events.toList(),
        )
    }

    @Test
    fun emptyStateShowsMessage() {
        show(ShareImportState.Empty)

        composeRule.onNodeWithTag(ShareImportTestTags.EMPTY).assertExists()
        composeRule.onNodeWithText("取り込める品物がありませんでした").assertExists()
    }
}
