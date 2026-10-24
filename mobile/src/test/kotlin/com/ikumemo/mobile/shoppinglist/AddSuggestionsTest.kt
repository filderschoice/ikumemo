package com.ikumemo.mobile.shoppinglist

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ikumemo.core.suggest.Suggestions
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import com.ikumemo.mobile.testing.testSettings
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** BL-032 の完了条件「表記ゆれで同じ候補が出て、いつものから複数件を追加できる」を ViewModel で確かめる。 */
@RunWith(RobolectricTestRunner::class)
class AddSuggestionsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var repository: ShoppingRepository
    private lateinit var viewModel: ShoppingListViewModel

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val clock = fixedClock()
        val normalizer = ProductNameNormalizer()
        repository = ShoppingRepository(db, normalizer, clock)
        viewModel =
            ShoppingListViewModel(
                repository,
                StoreRepository(db, testSettings(), normalizer, clock),
                Suggestions(normalizer),
            )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        db.close()
    }

    private suspend fun boughtBefore(
        name: String,
        times: Int,
    ) {
        repeat(times) {
            val id = (repository.addItem(name) as AddItemResult.Added).itemId
            repository.markBought(id)
        }
    }

    private suspend fun ReceiveTurbine<AddSuggestions>.awaitUntil(
        predicate: (AddSuggestions) -> Boolean,
    ): AddSuggestions {
        while (true) {
            val value = awaitItem()
            if (predicate(value)) return value
        }
    }

    @Test
    fun spellingVariantsShowSameCandidate() =
        runTest {
            boughtBefore("卵", 2)
            boughtBefore("玉ねぎ", 1)

            viewModel.addSuggestions.test {
                listOf("たまご", "タマゴ", "卵").forEach { query ->
                    viewModel.onEvent(ShoppingListEvent.AddQueryChanged(query))
                    val suggestions = awaitUntil { it.query == query && it.candidates.isNotEmpty() }
                    assertEquals(query, listOf("卵"), suggestions.candidates.map { it.name })
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun severalUsualItemsCanBeAdded() =
        runTest {
            boughtBefore("牛乳", 3)
            boughtBefore("パン", 2)
            boughtBefore("洗剤", 1)

            viewModel.addSuggestions.test {
                val usual = awaitUntil { it.usual.size == 3 }.usual
                assertEquals(listOf("牛乳", "パン", "洗剤"), usual.map { it.name })

                viewModel.onEvent(ShoppingListEvent.AddProduct("牛乳"))
                viewModel.onEvent(ShoppingListEvent.AddProduct("パン"))

                // メモに入った物は「いつもの」から外れる
                assertEquals(listOf("洗剤"), awaitUntil { it.usual.size == 1 }.usual.map { it.name })
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf("牛乳", "パン"), repository.observeItems().first().map { it.displayName })
        }

    @Test
    fun addingByTextClearsQuery() =
        runTest {
            viewModel.addSuggestions.test {
                viewModel.onEvent(ShoppingListEvent.AddQueryChanged("みかん"))
                awaitUntil { it.query == "みかん" }

                viewModel.onEvent(ShoppingListEvent.AddByText("みかん"))

                awaitUntil { it.query.isEmpty() }
                cancelAndIgnoreRemainingEvents()
            }
        }
}
