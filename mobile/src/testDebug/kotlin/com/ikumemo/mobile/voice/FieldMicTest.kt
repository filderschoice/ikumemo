package com.ikumemo.mobile.voice

import android.Manifest
import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.data.ItemEdit
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.shoppinglist.EditItemSheet
import com.ikumemo.mobile.shoppinglist.TestTags
import com.ikumemo.mobile.testing.FakeSpeechEngine
import com.ikumemo.mobile.testing.awaitTag
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Instant

/** BL-022 の完了条件「各欄のマイクから入力でき、品名欄では分割されない」。 */
@RunWith(RobolectricTestRunner::class)
class FieldMicTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val engine = FakeSpeechEngine()
    private val saved = mutableListOf<ItemEdit>()

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
    }

    private fun show(voiceAvailable: Boolean = true) {
        engine.isAvailable = voiceAvailable
        val item =
            ShoppingItemEntity(
                id = 1,
                productId = 1,
                displayName = "牛乳",
                note = "低脂肪",
                sortOrder = 0,
                createdAt = Instant.EPOCH,
            )
        val dictation = FieldDictationViewModel(engine)
        composeRule.setContent {
            IkumemoTheme {
                CompositionLocalProvider(LocalFieldDictation provides dictation) {
                    EditItemSheet(
                        item = item,
                        stores = emptyList(),
                        photoStore = PhotoStore(app),
                        onSave = { saved += it },
                        onRemove = {},
                    )
                }
            }
        }
    }

    /** ボトムシートの中は座標のタップが届かないため、クリック処理を直接呼ぶ。 */
    private fun click(tag: String) {
        composeRule.awaitTag(tag)
        composeRule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
    }

    @Test
    fun eachFieldTakesSpokenTextAndNameIsNotSplit() {
        show()
        engine.autoResults = listOf("牛乳とたまご")
        click(fieldMicTag(TestTags.EDIT_NAME))
        engine.autoResults = listOf("2本")
        click(fieldMicTag(TestTags.EDIT_QUANTITY))
        engine.autoResults = listOf("特売の日に")
        click(fieldMicTag(TestTags.EDIT_NOTE))

        click(TestTags.EDIT_CLOSE)

        // 品名は 1 つのまま、メモは改行して足す
        assertEquals(ItemEdit("牛乳とたまご", "2本", "低脂肪\n特売の日に", null, null), saved.last())
    }

    @Test
    fun noMicWhenRecognitionIsUnavailable() {
        show(voiceAvailable = false)
        composeRule.awaitTag(TestTags.EDIT_NAME)

        composeRule.onNodeWithTag(fieldMicTag(TestTags.EDIT_NAME), useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(fieldMicTag(TestTags.EDIT_NOTE), useUnmergedTree = true).assertDoesNotExist()
    }
}
