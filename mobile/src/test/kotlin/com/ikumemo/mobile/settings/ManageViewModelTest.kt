package com.ikumemo.mobile.settings

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ikumemo.core.suggest.Suggestions
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.AddItemResult
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** BL-092 の完了条件「使わない印・統合・非表示が DB に反映される」。 */
@RunWith(RobolectricTestRunner::class)
class ManageViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var stores: StoreRepository
    private lateinit var shopping: ShoppingRepository
    private val normalizer = ProductNameNormalizer()

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val clock = fixedClock()
        stores = StoreRepository(db, testSettings(), normalizer, clock)
        shopping = ShoppingRepository(db, normalizer, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
        while (true) {
            val value = awaitItem()
            if (predicate(value)) return value
        }
    }

    private suspend fun buy(
        name: String,
        storeId: Long? = null,
    ): Long {
        val itemId = (shopping.addItem(name) as AddItemResult.Added).itemId
        return (shopping.markBought(itemId, storeId) as UndoableAction.Bought).recordId
    }

    @Test
    fun archivedStoreLeavesListButKeepsRecordName() =
        runTest {
            val store = requireNotNull(stores.addStore("さくらスーパー"))
            val recordId = buy("牛乳", store.id)
            val viewModel = StoreManageViewModel(stores)

            viewModel.stores.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.isNotEmpty() }
                viewModel.onEvent(StoreManageEvent.Archive(store.id))
                awaitUntil { it.isEmpty() }
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()

            // 記録のお店は残る（R-16）
            assertEquals(store.id, db.purchaseRecordDao().getById(recordId)?.storeId)
            assertTrue(requireNotNull(db.storeDao().getById(store.id)).archived)
        }

    @Test
    fun mergeMovesRecordsIntoOtherStore() =
        runTest {
            val from = requireNotNull(stores.addStore("さくらスーパー駅前"))
            val into = requireNotNull(stores.addStore("さくらスーパー"))
            val recordId = buy("牛乳", from.id)
            val viewModel = StoreManageViewModel(stores)

            viewModel.stores.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.size == 2 }
                viewModel.onEvent(StoreManageEvent.Merge(from = from.id, into = into.id))
                assertEquals(listOf(into.id), awaitUntil { it.size == 1 }.map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()

            assertEquals(into.id, db.purchaseRecordDao().getById(recordId)?.storeId)
        }

    @Test
    fun productsAreSortedByPurchaseCountAndCanBeHiddenFromSuggestions() =
        runTest {
            buy("牛乳")
            buy("牛乳")
            buy("たまご")
            val viewModel = ProductManageViewModel(shopping.products)

            viewModel.products.test(timeout = TURBINE_TIMEOUT) {
                val products = awaitUntil { it.size == 2 }
                assertEquals(listOf("牛乳", "たまご"), products.map { it.name })

                viewModel.onEvent(SetProductVisible(products[0].id, visible = false))
                awaitUntil { list -> list.first { it.name == "牛乳" }.hidden }
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()

            // 候補（いつもの）には出なくなる
            val summaries = shopping.products.observeSummaries().first()
            assertEquals(listOf("たまご"), Suggestions(normalizer).usual(summaries, emptySet()).map { it.name })
        }
}
