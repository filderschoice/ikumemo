package com.ikumemo.mobile.shoppinglist

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.ItemEdit
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.TURBINE_TIMEOUT
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import com.ikumemo.mobile.testing.testSettings
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShoppingListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var viewModel: ShoppingListViewModel

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val clock = fixedClock()
        val normalizer = ProductNameNormalizer()
        viewModel =
            ShoppingListViewModel(
                ShoppingRepository(db, normalizer, clock),
                StoreRepository(db, testSettings(), normalizer, clock),
            )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        db.close()
    }

    private suspend fun ReceiveTurbine<ShoppingListUiState>.awaitUntil(
        predicate: (ShoppingListUiState) -> Boolean,
    ): ShoppingListUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }

    @Test
    fun addByTextAppendsItemsInOrder() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.loaded }
                viewModel.onEvent(ShoppingListEvent.AddByText("牛乳"))
                viewModel.onEvent(ShoppingListEvent.AddByText(" たまご "))

                val state = awaitUntil { it.items.size == 2 }

                assertEquals(listOf("牛乳", "たまご"), state.items.map { it.displayName })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun addingSameProductHighlightsExistingItemAndShowsMessage() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.loaded }
                viewModel.onEvent(ShoppingListEvent.AddByText("卵"))
                val existing = awaitUntil { it.items.size == 1 }.items.single()

                viewModel.onEvent(ShoppingListEvent.AddByText("タマゴ"))
                val state = awaitUntil { it.message != null }

                assertEquals(1, state.items.size)
                assertEquals(existing.id, state.highlightedItemId)
                assertEquals(HomeMessage.AlreadyInMemo("卵"), state.message)

                viewModel.onEvent(ShoppingListEvent.MessageShown)
                viewModel.onEvent(ShoppingListEvent.HighlightShown)
                val cleared = awaitUntil { it.message == null && it.highlightedItemId == null }
                assertNull(cleared.message)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun blankTextIsIgnored() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.loaded }
                viewModel.onEvent(ShoppingListEvent.AddByText("   "))
                viewModel.onEvent(ShoppingListEvent.AddByText("牛乳"))

                assertEquals(listOf("牛乳"), awaitUntil { it.items.isNotEmpty() }.items.map { it.displayName })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun editSavesFieldsAndClosesSheet() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.loaded }
                viewModel.onEvent(ShoppingListEvent.AddByText("牛乳"))
                val item = awaitUntil { it.items.size == 1 }.items.single()
                viewModel.onEvent(ShoppingListEvent.OpenEditor(item.id))
                assertEquals(HomeSheet.Edit(item.id), awaitUntil { it.sheet != null }.sheet)

                viewModel.onEvent(ShoppingListEvent.SaveEdit(item.id, ItemEdit("低脂肪乳", " 2本 ", "")))

                val state = awaitUntil { it.sheet == null && it.items.single().quantity == "2本" }
                val edited = state.items.single()
                assertEquals("低脂肪乳", edited.displayName)
                assertNull(edited.note)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun renamingToProductAlreadyInMemoKeepsNameAndPointsToExisting() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.loaded }
                viewModel.onEvent(ShoppingListEvent.AddByText("牛乳"))
                viewModel.onEvent(ShoppingListEvent.AddByText("パン"))
                val items = awaitUntil { it.items.size == 2 }.items

                viewModel.onEvent(ShoppingListEvent.SaveEdit(items[1].id, ItemEdit("ぎゅうにゅう", "1斤", "")))

                val state = awaitUntil { it.message != null && it.items[1].quantity != null }
                assertEquals(listOf("牛乳", "パン"), state.items.map { it.displayName })
                assertEquals("1斤", state.items[1].quantity)
                assertEquals(items[0].id, state.highlightedItemId)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
