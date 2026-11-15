package com.ikumemo.mobile.shoppinglist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.testing.FakeSpeechEngine
import com.ikumemo.mobile.testing.awaitCondition
import com.ikumemo.mobile.testing.awaitTag
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import com.ikumemo.mobile.testing.testSettings
import com.ikumemo.mobile.voice.VoiceInputViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 「えらんで消す」モードを画面の操作で確かめる（BL-194）。普段の左スワイプ（やめた）が残ることも確かめる。 */
@RunWith(RobolectricTestRunner::class)
class SelectModeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var repository: ShoppingRepository
    private lateinit var ids: List<Long>

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val clock = fixedClock()
        repository = ShoppingRepository(db, ProductNameNormalizer(), clock)
        ids =
            runBlocking {
                listOf("牛乳", "たまご", "パン").map { (repository.addItem(it) as AddItemResult.Added).itemId }
            }
        val stores = StoreRepository(db, testSettings(), ProductNameNormalizer(), clock)
        val viewModel = ShoppingListViewModel(repository, stores)
        val voiceViewModel = VoiceInputViewModel(repository, FakeSpeechEngine())
        composeRule.setContent { IkumemoTheme { ShoppingListScreen(viewModel, voiceViewModel) } }
        composeRule.awaitTag(TestTags.item(ids[0]))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun items() = runBlocking { repository.observeItems().first() }

    private fun startSelecting() {
        composeRule.onNodeWithTag(TestTags.SELECT_START).performClick()
        composeRule.awaitTag(TestTags.SELECT_DELETE)
    }

    @Test
    fun checkedItemsAreDismissedTogetherThenUndoneTogether() {
        startSelecting()
        composeRule.onNodeWithTag(TestTags.SELECT_DELETE).assertIsNotEnabled()
        composeRule.onNodeWithTag(TestTags.item(ids[0])).performClick()
        composeRule.onNodeWithTag(TestTags.item(ids[2])).performClick()
        composeRule.awaitCondition {
            composeRule.onAllNodesWithText("2件えらんでいます").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TestTags.SELECT_DELETE).assertIsEnabled()

        composeRule.onNodeWithTag(TestTags.SELECT_DELETE).performClick()

        composeRule.awaitCondition { items().map { it.displayName } == listOf("たまご") }
        composeRule.onNodeWithText("やめた：2件").assertIsDisplayed()
        // えらぶモードは終わり、普段の追加ボタンに戻る
        composeRule.onNodeWithTag(TestTags.SELECT_DELETE).assertDoesNotExist()

        composeRule.onNodeWithText("元に戻す").performClick()
        composeRule.awaitCondition { items().map { it.displayName } == listOf("牛乳", "たまご", "パン") }
    }

    @Test
    fun cancelLeavesEverythingAsIs() {
        startSelecting()
        composeRule.onNodeWithTag(TestTags.item(ids[1])).performClick()

        composeRule.onNodeWithTag(TestTags.SELECT_CANCEL).performClick()

        composeRule.awaitTag(TestTags.ADD_BY_TEXT)
        check(items().size == 3)
        composeRule.onNodeWithTag(TestTags.SELECT_START).assertIsDisplayed()
    }

    /** えらぶモードでは行を押してもチェックが入るだけで、編集シートは開かず、スワイプでも消えない。 */
    @Test
    fun rowTapAndSwipeDoNotActInSelectMode() {
        startSelecting()

        composeRule.onNodeWithTag(TestTags.item(ids[0])).performClick()
        composeRule.onNodeWithTag(TestTags.item(ids[1])).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        check(items().size == 3)
        composeRule.onNodeWithTag(TestTags.EDIT_NAME).assertDoesNotExist()
    }

    /** 普段のモードの左スワイプ（やめた）は残る。 */
    @Test
    fun normalSwipeLeftStillDismisses() {
        composeRule.onNodeWithTag(TestTags.item(ids[0])).performTouchInput { swipeLeft() }

        composeRule.awaitCondition { items().size == 2 }
        composeRule.onNodeWithText("やめた：牛乳").assertIsDisplayed()
    }
}
