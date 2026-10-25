package com.ikumemo.mobile.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ikumemo.mobile.data.backup.ExportKind
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** データ画面（F-72・F-76）。書き出しは押すだけ、読み込みと消去は確認を 1 回出してから進み、やめれば何も起きない。 */
@RunWith(RobolectricTestRunner::class)
class DataContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableStateListOf<DataEvent>()
    private val exports = mutableStateListOf<ExportKind>()
    private var pickedImport = 0

    private fun show(state: DataUiState = DataUiState()) {
        composeRule.setContent {
            IkumemoTheme {
                DataContent(
                    state,
                    onEvent = { events += it },
                    onExport = { exports += it },
                    onPickImportFile = { pickedImport++ },
                )
            }
        }
    }

    @Test
    fun exportRowsAskForEachKind() {
        show()

        composeRule.onNodeWithTag(DataTestTags.EXPORT_ZIP).performClick()
        composeRule.onNodeWithTag(DataTestTags.EXPORT_JSON).performClick()
        composeRule.onNodeWithTag(DataTestTags.EXPORT_CSV).performClick()

        assertEquals(listOf(ExportKind.ZIP, ExportKind.JSON, ExportKind.CSV), exports.toList())
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
    fun opensFilePickerForImportOnlyAfterConfirmation() {
        show()

        composeRule.onNodeWithTag(DataTestTags.IMPORT).performClick()
        assertEquals(0, pickedImport)
        composeRule.onNodeWithText("データを読み込みますか？").assertIsDisplayed()
        composeRule.onNodeWithTag(DataTestTags.IMPORT_CONFIRM).performClick()

        assertEquals(1, pickedImport)
    }

    @Test
    fun cancelDoesNothing() {
        show()

        composeRule.onNodeWithTag(DataTestTags.ERASE).performClick()
        composeRule.onNodeWithText("やめる").performClick()
        composeRule.onNodeWithTag(DataTestTags.IMPORT).performClick()
        composeRule.onNodeWithText("やめる").performClick()

        assertEquals(emptyList<DataEvent>(), events.toList())
        assertEquals(0, pickedImport)
    }

    @Test
    fun showsTheResult() {
        show(DataUiState(result = DataResult.IMPORT_INVALID))

        composeRule.onNodeWithTag(DataTestTags.RESULT).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("このファイルは読み込めません", substring = true).assertIsDisplayed()
    }
}
