package com.ikumemo.mobile.shoppinglist

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.UndoableAction
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 「えらんで消す」モード（BL-194）。入る・チェックの入れ外し・まとめて消す・まとめて元に戻す。 */
@RunWith(RobolectricTestRunner::class)
class SelectModeViewModelTest {
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

    private suspend fun ReceiveTurbine<ShoppingListUiState>.addThree(): List<Long> {
        awaitUntil { it.loaded }
        listOf("牛乳", "たまご", "パン").forEach { viewModel.onEvent(ShoppingListEvent.AddByText(it)) }
        return awaitUntil { it.items.size == 3 }.items.map { it.id }
    }

    @Test
    fun startToggleAndStopSelecting() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                val ids = addThree()
                viewModel.onEvent(SelectionEvent.Start)
                assertEquals(emptySet<Long>(), awaitUntil { it.selection != null }.selection)

                viewModel.onEvent(SelectionEvent.Toggle(ids[0]))
                viewModel.onEvent(SelectionEvent.Toggle(ids[2]))
                assertEquals(setOf(ids[0], ids[2]), awaitUntil { it.selection?.size == 2 }.selection)

                viewModel.onEvent(SelectionEvent.Toggle(ids[0]))
                assertEquals(setOf(ids[2]), awaitUntil { it.selection?.size == 1 }.selection)

                viewModel.onEvent(SelectionEvent.Stop)
                val stopped = awaitUntil { it.selection == null }
                // やめても何も消えない
                assertEquals(3, stopped.items.size)
                assertNull(stopped.undo)
                cancelAndIgnoreRemainingEvents()
            }
        }

    /** モードに入っていないときのチェックの入れ外しは無視する。 */
    @Test
    fun toggleOutsideSelectingIsIgnored() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                val ids = addThree()
                viewModel.onEvent(SelectionEvent.Toggle(ids[0]))
                viewModel.onEvent(SelectionEvent.Start)
                assertEquals(emptySet<Long>(), awaitUntil { it.selection != null }.selection)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dismissSelectedRemovesOnlyCheckedAndUndoRestoresAll() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                val ids = addThree()
                viewModel.onEvent(SelectionEvent.Start)
                viewModel.onEvent(SelectionEvent.Toggle(ids[0]))
                viewModel.onEvent(SelectionEvent.Toggle(ids[2]))
                awaitUntil { it.selection?.size == 2 }

                viewModel.onEvent(ShoppingListEvent.DismissSelected)
                val removed = awaitUntil { it.undo != null && it.items.size == 1 && it.selection == null }
                assertEquals(listOf(ids[1]), removed.items.map { it.id })
                val offer = requireNotNull(removed.undo)
                assertEquals(2, offer.actions.size)
                assertTrue(offer.actions.all { it is UndoableAction.Dismissed })

                viewModel.onEvent(ShoppingListEvent.Undo(offer.serial))
                val restored = awaitUntil { it.items.size == 3 && it.undo == null }
                assertEquals(listOf("牛乳", "たまご", "パン"), restored.items.map { it.displayName })
                cancelAndIgnoreRemainingEvents()
            }
        }

    /** 1 件もえらばずに消そうとしても何も消えず、「元に戻す」も出ない。 */
    @Test
    fun dismissSelectedWithNothingCheckedDoesNothing() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                addThree()
                viewModel.onEvent(SelectionEvent.Start)
                awaitUntil { it.selection != null }

                viewModel.onEvent(ShoppingListEvent.DismissSelected)
                viewModel.onEvent(SelectionEvent.Stop)
                val state = awaitUntil { it.selection == null }
                assertEquals(3, state.items.size)
                assertNull(state.undo)
                cancelAndIgnoreRemainingEvents()
            }
        }

    /** チェックを入れた項目がほかの操作でなくなったら、チェックの数から外れる。 */
    @Test
    fun selectionDropsItemsThatDisappeared() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                val ids = addThree()
                viewModel.onEvent(SelectionEvent.Start)
                viewModel.onEvent(SelectionEvent.Toggle(ids[0]))
                viewModel.onEvent(SelectionEvent.Toggle(ids[1]))
                awaitUntil { it.selection?.size == 2 }

                viewModel.onEvent(ShoppingListEvent.Dismiss(ids[0]))
                val state = awaitUntil { it.items.size == 2 }
                assertEquals(setOf(ids[1]), state.selection)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
