package com.ikumemo.mobile.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
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

/** BL-103 のデータ画面。確認を 1 回出してから消し、やめれば何も起きない。 */
@RunWith(RobolectricTestRunner::class)
class DataContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableStateListOf<DataEvent>()

    private fun show(state: DataUiState = DataUiState()) {
        composeRule.setContent { IkumemoTheme { DataContent(state, onEvent = { events += it }) } }
    }

    @Test
    fun erasesOnlyAfterConfirmation() {
        show()

        composeRule.onNodeWithTag(DataTestTags.ERASE).performClick()
        assertEquals(emptyList<DataEvent>(), events.toList())
        composeRule.onNodeWithText("データをすべて消しますか？").assertIsDisplayed()
        composeRule.onNodeWithTag(DataTestTags.ERASE_CONFIRM).performClick()

        assertEquals(listOf(DataEvent.EraseAll), events.toList())
        composeRule.onNodeWithText("データをすべて消しますか？").assertDoesNotExist()
    }

    @Test
    fun cancelDoesNothing() {
        show()

        composeRule.onNodeWithTag(DataTestTags.ERASE).performClick()
        composeRule.onNodeWithText("やめる").performClick()

        assertEquals(emptyList<DataEvent>(), events.toList())
        composeRule.onNodeWithText("データをすべて消しますか？").assertDoesNotExist()
    }

    @Test
    fun showsNoticeAfterErasing() {
        show(DataUiState(erased = true))

        composeRule.onNodeWithTag(DataTestTags.ERASED).assertIsDisplayed()
    }
}
