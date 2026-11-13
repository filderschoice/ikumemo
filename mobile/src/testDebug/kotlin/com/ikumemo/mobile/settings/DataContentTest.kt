package com.ikumemo.mobile.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import com.ikumemo.mobile.data.backup.ExportKind
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

        composeRule.onNodeWithTag(DataTestTags.EXPORT_ZIP).performScrollTo().performClick()
        composeRule.onNodeWithTag(DataTestTags.EXPORT_OTHER).performScrollTo().performClick()
        composeRule.onNodeWithTag(DataTestTags.EXPORT_JSON).performScrollTo().performClick()
        composeRule.onNodeWithTag(DataTestTags.EXPORT_CSV).performScrollTo().performClick()

        assertEquals(listOf(ExportKind.ZIP, ExportKind.JSON, ExportKind.CSV), exports.toList())
    }

    /** BL-145。最初は写真も含む書き出しだけが見え、「ほかの形式で書き出す」を押すと写真なしと表計算用が出る。 */
    @Test
    fun otherExportsAreFoldedAtFirst() {
        show()

        composeRule.onNodeWithTag(DataTestTags.EXPORT_ZIP).assertExists()
        composeRule.onNodeWithText("データを書き出す（写真も含む）").assertExists()
        composeRule.onNodeWithTag(DataTestTags.EXPORT_JSON).assertDoesNotExist()
        composeRule.onNodeWithTag(DataTestTags.EXPORT_CSV).assertDoesNotExist()

        composeRule.onNodeWithText("ほかの形式で書き出す").performScrollTo().performClick()

        composeRule.onNodeWithTag(DataTestTags.EXPORT_JSON).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(DataTestTags.EXPORT_CSV).performScrollTo().performClick()
        assertEquals(listOf(ExportKind.CSV), exports.toList())
        assertTrue(top(DataTestTags.EXPORT_ZIP) < top(DataTestTags.EXPORT_OTHER))
        assertTrue(top(DataTestTags.EXPORT_CSV) < top(DataTestTags.HEADING_IMPORT))
    }

    /** BL-145。もう一度押すと閉じる。 */
    @Test
    fun otherExportsCloseAgain() {
        show()

        composeRule.onNodeWithTag(DataTestTags.EXPORT_OTHER).performScrollTo().performClick()
        composeRule.onNodeWithTag(DataTestTags.EXPORT_OTHER).performScrollTo().performClick()

        composeRule.onNodeWithTag(DataTestTags.EXPORT_JSON).assertDoesNotExist()
    }

    /** BL-145。説明には ZIP・JSON・CSV の語を出さない。 */
    @Test
    fun detailsAvoidFileFormatNames() {
        show()
        composeRule.onNodeWithTag(DataTestTags.EXPORT_OTHER).performScrollTo().performClick()

        listOf("ZIP", "JSON", "CSV").forEach {
            composeRule.onNodeWithText(it, substring = true).assertDoesNotExist()
        }
        composeRule.onNodeWithText("表計算ソフト（Excel など）で開けるファイル", substring = true).assertExists()
    }

    @Test
    fun erasesOnlyAfterConfirmation() {
        show()

        composeRule.onNodeWithTag(DataTestTags.ERASE).performScrollTo().performClick()
        assertEquals(emptyList<DataEvent>(), events.toList())
        composeRule.onNodeWithText("データをすべて消しますか？").assertIsDisplayed()
        composeRule.onNodeWithTag(DataTestTags.ERASE_CONFIRM).performClick()

        assertEquals(listOf(DataEvent.EraseAll), events.toList())
        composeRule.onNodeWithText("データをすべて消しますか？").assertDoesNotExist()
    }

    @Test
    fun opensFilePickerForImportOnlyAfterConfirmation() {
        show()

        composeRule.onNodeWithTag(DataTestTags.IMPORT).performScrollTo().performClick()
        assertEquals(0, pickedImport)
        composeRule.onNodeWithText("データを読み込みますか？").assertIsDisplayed()
        composeRule.onNodeWithTag(DataTestTags.IMPORT_CONFIRM).performClick()

        assertEquals(1, pickedImport)
    }

    @Test
    fun cancelDoesNothing() {
        show()

        composeRule.onNodeWithTag(DataTestTags.ERASE).performScrollTo().performClick()
        composeRule.onNodeWithText("戻る").performClick()
        composeRule.onNodeWithTag(DataTestTags.IMPORT).performScrollTo().performClick()
        composeRule.onNodeWithText("戻る").performClick()

        assertEquals(emptyList<DataEvent>(), events.toList())
        assertEquals(0, pickedImport)
    }

    /** BL-133。確認ダイアログの取り消しは「戻る」（メモの「やめた」と紛らわしい「やめる」は使わない）。 */
    @Test
    fun confirmDialogCancelSaysBack() {
        show()

        composeRule.onNodeWithTag(DataTestTags.ERASE).performScrollTo().performClick()

        composeRule.onNodeWithText("戻る").assertIsDisplayed()
        composeRule.onNodeWithText("やめる").assertDoesNotExist()
    }

    @Test
    fun showsTheResult() {
        show(DataUiState(result = DataResult.IMPORT_INVALID))

        composeRule.onNodeWithTag(DataTestTags.RESULT).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("このファイルは読み込めません", substring = true).assertIsDisplayed()
    }

    /** BL-134。見出しは「書き出す」「読み込む」「消す」の順で、写真も含む書き出しが一番上、すべて消すは一番下。 */
    @Test
    fun sectionsAreInOrder() {
        show()

        val tops =
            listOf(
                DataTestTags.HEADING_EXPORT,
                DataTestTags.EXPORT_ZIP,
                DataTestTags.HEADING_IMPORT,
                DataTestTags.IMPORT,
                DataTestTags.HEADING_ERASE,
                DataTestTags.ERASE,
            ).map { top(it) }

        assertEquals(tops.sorted(), tops)
        composeRule.onNodeWithText("書き出す").assertExists()
        composeRule.onNodeWithText("読み込む").assertExists()
        composeRule.onNodeWithText("消す").assertExists()
    }

    /** BL-134。結果の知らせは最初の見出しより上に出る。 */
    @Test
    fun resultIsAboveTheFirstSection() {
        show(DataUiState(result = DataResult.EXPORTED))

        assertTrue(top(DataTestTags.RESULT) < top(DataTestTags.HEADING_EXPORT))
    }

    /** BL-134。処理中は押した行にだけ進み具合と文字が出て、ほかの行は押せない。 */
    @Test
    fun showsProgressOnTheRunningRow() {
        show(DataUiState(running = DataTask.EXPORT_CSV))

        // 進み具合は押せる行の中にまとめられるため、まとめる前の木で探す
        composeRule.onNodeWithTag(DataTestTags.PROGRESS, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("書き出しています…").assertIsDisplayed()
        val progress = top(DataTestTags.PROGRESS)
        assertTrue(top(DataTestTags.EXPORT_CSV) < progress && progress < top(DataTestTags.HEADING_IMPORT))

        composeRule.onNodeWithTag(DataTestTags.EXPORT_ZIP).performScrollTo().performClick()
        assertEquals(emptyList<ExportKind>(), exports.toList())
    }

    @Test
    fun noProgressWhenIdle() {
        show()

        composeRule.onNodeWithTag(DataTestTags.PROGRESS, useUnmergedTree = true).assertDoesNotExist()
    }

    /** 画面の外にある行も比べられるよう、切り取られる前の位置を使う。 */
    private fun top(tag: String): Dp =
        composeRule
            .onNodeWithTag(tag, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .top
}
