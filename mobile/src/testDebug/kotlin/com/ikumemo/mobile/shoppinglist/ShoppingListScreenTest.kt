package com.ikumemo.mobile.shoppinglist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * M1 の完了条件「手入力で追加 → スワイプで記録 → 元に戻す」を画面の操作で確かめる。
 * Compose のテスト用 Activity（ui-test-manifest）は debug にだけ入れるため、画面のテストは testDebug に置く。
 */
@RunWith(RobolectricTestRunner::class)
class ShoppingListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var repository: ShoppingRepository
    private val today = LocalDate.of(2026, 10, 25)

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = ShoppingRepository(db, ProductNameNormalizer(), fixedClock())
        val viewModel = ShoppingListViewModel(repository)
        composeRule.setContent { IkumemoTheme { ShoppingListScreen(viewModel) } }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun items() = runBlocking { repository.observeItems().first() }

    private fun records() = runBlocking { db.purchaseRecordDao().observeBetween(today, today).first() }

    private fun waitUntil(condition: () -> Boolean) = composeRule.waitUntil(timeoutMillis = 5_000, condition)

    private fun addByText(name: String) {
        composeRule.onNodeWithTag(TestTags.ADD_BY_TEXT).performClick()
        waitUntil { composeRule.onAllNodesWithTagExists(TestTags.ADD_SHEET_INPUT) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TestTags.ADD_SHEET_INPUT).performTextInput(name)
        // シートが動いている間に座標でボタンを押すと外れるため、キーボードの「完了」で追加する
        composeRule.onNodeWithTag(TestTags.ADD_SHEET_INPUT).performImeAction()
        waitUntil { items().any { it.displayName == name } }
        composeRule.onNodeWithText("閉じる").performClick()
    }

    @Test
    fun addSwipeRightToRecordThenUndo() {
        addByText("牛乳")
        val id = items().single().id
        composeRule.onNodeWithText("牛乳").assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.item(id)).performTouchInput { swipeRight() }
        waitUntil { items().isEmpty() && records().size == 1 }
        composeRule.onNodeWithText("買った：牛乳").assertIsDisplayed()

        composeRule.onNodeWithText("元に戻す").performClick()
        waitUntil { items().size == 1 && records().isEmpty() }
        composeRule.onNodeWithText("牛乳").assertIsDisplayed()
    }

    @Test
    fun swipeLeftDismissesWithoutRecord() {
        val id = runBlocking { (repository.addItem("洗剤") as AddItemResult.Added).itemId }
        waitUntil { composeRule.onAllNodesWithTagExists(TestTags.item(id)) }

        composeRule.onNodeWithTag(TestTags.item(id)).performTouchInput { swipeLeft() }

        waitUntil { items().isEmpty() }
        composeRule.onNodeWithText("やめた：洗剤").assertIsDisplayed()
        check(records().isEmpty())
    }

    @Test
    fun tapCircleMarksBought() {
        val id = runBlocking { (repository.addItem("たまご") as AddItemResult.Added).itemId }
        waitUntil { composeRule.onAllNodesWithTagExists(TestTags.boughtButton(id)) }

        composeRule.onNodeWithTag(TestTags.boughtButton(id)).performClick()

        waitUntil { items().isEmpty() && records().size == 1 }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagExists(tag: String): Boolean =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
}
