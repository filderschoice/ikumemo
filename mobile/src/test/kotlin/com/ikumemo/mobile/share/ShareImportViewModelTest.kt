package com.ikumemo.mobile.share

import androidx.lifecycle.viewModelScope
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 共有された文の取り込み（BL-197）。チェックを外した物は追加せず、既にある物は最初から外す。 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ShareImportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var repository: ShoppingRepository
    private lateinit var viewModel: ShareImportViewModel

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = ShoppingRepository(db, ProductNameNormalizer(), fixedClock())
        viewModel = ShareImportViewModel(repository)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        db.close()
    }

    private fun TestScope.awaitState(predicate: (ShareImportState) -> Boolean): ShareImportState {
        repeat(MAX_WAIT_ROUNDS) {
            runCurrent()
            val state = viewModel.state.value
            if (predicate(state)) return state
            Thread.sleep(WAIT_MILLIS)
        }
        error("状態が変わらない: ${viewModel.state.value}")
    }

    private suspend fun memo() = repository.observeItems().first().map { it.displayName to it.quantity }

    @Test
    fun ikumemoTextBecomesRowsAndExistingItemStartsUnchecked() =
        runTest {
            repository.addItem("たまご")
            viewModel.load("🛒いくメモ\n・牛乳 2本\n・たまご\n・パン")

            val review = awaitState { it is ShareImportState.Review } as ShareImportState.Review

            assertEquals(
                listOf(
                    ShareImportRow(0, "牛乳", "2本", alreadyInMemo = false, included = true),
                    ShareImportRow(1, "たまご", null, alreadyInMemo = true, included = false),
                    ShareImportRow(2, "パン", null, alreadyInMemo = false, included = true),
                ),
                review.rows,
            )
        }

    @Test
    fun uncheckedRowsAreNotAdded() =
        runTest {
            viewModel.load("🛒いくメモ\n・牛乳 2本\n・パン\n・バナナ 1房")
            awaitState { it is ShareImportState.Review }

            viewModel.onEvent(ShareImportEvent.Toggle(1))
            viewModel.onEvent(ShareImportEvent.AddSelected)

            assertEquals(ShareImportState.Added(2), awaitState { it is ShareImportState.Added })
            assertEquals(listOf("牛乳" to "2本", "バナナ" to "1房"), memo())
        }

    @Test
    fun recheckedExistingItemIsNotAddedTwice() =
        runTest {
            repository.addItem("パン")
            viewModel.load("パン\n牛乳")
            awaitState { it is ShareImportState.Review }

            viewModel.onEvent(ShareImportEvent.Toggle(0))
            viewModel.onEvent(ShareImportEvent.AddSelected)

            assertEquals(ShareImportState.Added(1), awaitState { it is ShareImportState.Added })
            assertEquals(listOf("パン" to null, "牛乳" to null), memo())
        }

    @Test
    fun missingTextShowsEmpty() =
        runTest {
            viewModel.load(null)
            assertEquals(ShareImportState.Empty, awaitState { it != ShareImportState.Loading })
        }

    @Test
    fun blankTextShowsEmptyAndAddsNothing() =
        runTest {
            viewModel.load(" \n\u0000 ")
            assertEquals(ShareImportState.Empty, awaitState { it != ShareImportState.Loading })

            viewModel.onEvent(ShareImportEvent.AddSelected)
            runCurrent()
            assertEquals(ShareImportState.Empty, viewModel.state.value)
            assertEquals(emptyList<Any>(), memo())
        }

    @Test
    fun veryLongTextIsLimited() =
        runTest {
            viewModel.load((1..10_000).joinToString("\n") { "品物$it" })

            val review = awaitState { it is ShareImportState.Review } as ShareImportState.Review

            assertEquals(100, review.rows.size)
        }

    @Test
    fun loadOnlyReadsTheFirstText() =
        runTest {
            viewModel.load("牛乳")
            viewModel.load("パン")

            val review = awaitState { it is ShareImportState.Review } as ShareImportState.Review

            assertEquals(listOf("牛乳"), review.rows.map { it.name })
        }

    private companion object {
        const val MAX_WAIT_ROUNDS = 250
        const val WAIT_MILLIS = 20L
    }
}
