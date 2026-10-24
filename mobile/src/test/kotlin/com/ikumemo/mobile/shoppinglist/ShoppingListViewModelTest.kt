package com.ikumemo.mobile.shoppinglist

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
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
        viewModel = ShoppingListViewModel(ShoppingRepository(db, ProductNameNormalizer(), fixedClock()))
    }

    @After
    fun tearDown() {
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
            viewModel.uiState.test {
                awaitUntil { it.loaded }
                viewModel.addByText("牛乳")
                viewModel.addByText(" たまご ")

                val state = awaitUntil { it.items.size == 2 }

                assertEquals(listOf("牛乳", "たまご"), state.items.map { it.displayName })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun addingSameProductHighlightsExistingItemAndShowsMessage() =
        runTest {
            viewModel.uiState.test {
                awaitUntil { it.loaded }
                viewModel.addByText("卵")
                val existing = awaitUntil { it.items.size == 1 }.items.single()

                viewModel.addByText("タマゴ")
                val state = awaitUntil { it.message != null }

                assertEquals(1, state.items.size)
                assertEquals(existing.id, state.highlightedItemId)
                assertEquals(HomeMessage.AlreadyInMemo("卵"), state.message)

                viewModel.messageShown()
                viewModel.highlightShown()
                val cleared = awaitUntil { it.message == null && it.highlightedItemId == null }
                assertNull(cleared.message)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun blankTextIsIgnored() =
        runTest {
            viewModel.uiState.test {
                awaitUntil { it.loaded }
                viewModel.addByText("   ")
                viewModel.addByText("牛乳")

                assertEquals(listOf("牛乳"), awaitUntil { it.items.isNotEmpty() }.items.map { it.displayName })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun editSavesFieldsAndClosesSheet() =
        runTest {
            viewModel.uiState.test {
                awaitUntil { it.loaded }
                viewModel.addByText("牛乳")
                val item = awaitUntil { it.items.size == 1 }.items.single()
                viewModel.openEditor(item.id)
                assertEquals(HomeSheet.Edit(item.id), awaitUntil { it.sheet != null }.sheet)

                viewModel.saveEdit(item.id, "低脂肪乳", " 2本 ", "")

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
            viewModel.uiState.test {
                awaitUntil { it.loaded }
                viewModel.addByText("牛乳")
                viewModel.addByText("パン")
                val items = awaitUntil { it.items.size == 2 }.items

                viewModel.saveEdit(items[1].id, "ぎゅうにゅう", "1斤", "")

                val state = awaitUntil { it.message != null && it.items[1].quantity != null }
                assertEquals(listOf("牛乳", "パン"), state.items.map { it.displayName })
                assertEquals("1斤", state.items[1].quantity)
                assertEquals(items[0].id, state.highlightedItemId)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
