package com.ikumemo.mobile.calendar

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.RecordEdit
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 記録の編集シート（F-16）の閉じ方。閉じると保存し（メモの編集と同じ、BL-144）、消すときは保存しない。
 *
 * ボトムシートは別のウィンドウに描かれ、Robolectric では座標でのタップが届かないため、クリック処理を直接呼ぶ。
 */
@RunWith(RobolectricTestRunner::class)
class RecordEditSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableStateListOf<CalendarEvent>()
    private val record = CalendarSamples.row(id = 7, name = "牛乳").record

    /** 消したら ViewModel が editingId を外すのと同じく、DeleteRecord を受けたらシートを描かなくする。 */
    private fun show() {
        val photoStore = PhotoStore(context)
        composeRule.setContent {
            var open by remember { mutableStateOf(true) }
            IkumemoTheme {
                if (open) {
                    RecordEditSheet(record = record, stores = emptyList(), photoStore = photoStore, onEvent = {
                        events += it
                        if (it is CalendarEvent.DeleteRecord) open = false
                    })
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun closeButtonSavesEditedFields() {
        show()
        composeRule.onNodeWithTag("record_note").performTextReplacement("低脂肪")
        composeRule.onNodeWithTag("record_quantity").performTextReplacement("2本")

        composeRule.onNodeWithTag(CalendarTestTags.RECORD_CLOSE).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals(
            listOf(CalendarEvent.SaveRecord(record.id, RecordEdit(record.purchasedDate, null, "2本", "低脂肪", null))),
            events.toList(),
        )
    }

    @Test
    fun dismissingSheetSaves() {
        show()
        composeRule.onNodeWithTag("record_note").performTextReplacement("低脂肪")

        // 下へ払う・外を押すのと同じ、シートを閉じる操作（ドラッグハンドルの dismiss）
        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.Dismiss)
        composeRule.waitForIdle()

        assertEquals(
            listOf(CalendarEvent.SaveRecord(record.id, RecordEdit(record.purchasedDate, null, "", "低脂肪", null))),
            events.toList(),
        )
    }

    @Test
    fun deleteSendsOnlyDeleteRecord() {
        show()
        composeRule.onNodeWithTag("record_note").performTextReplacement("低脂肪")

        composeRule.onNodeWithTag(CalendarTestTags.RECORD_DELETE).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals(listOf(CalendarEvent.DeleteRecord(record.id)), events.toList())
    }

    @Test
    fun deleteSitsBelowCloseOnItsOwnRow() {
        show()

        // 押し間違えないよう、「この記録を消す」は「閉じる」と同じ行に並べず、その下へ離して置く（BL-135）
        // シートの下の方は画面の外にはみ出すことがあるため、切り取られない位置と大きさで比べる
        val close = composeRule.onNodeWithTag(CalendarTestTags.RECORD_CLOSE).fetchSemanticsNode()
        val delete = composeRule.onNodeWithTag(CalendarTestTags.RECORD_DELETE).fetchSemanticsNode()
        val closeBottom = close.positionInRoot.y + close.size.height
        assertTrue("消す（${delete.positionInRoot.y}）が閉じる（$closeBottom）の下に無い", delete.positionInRoot.y >= closeBottom)
    }
}
