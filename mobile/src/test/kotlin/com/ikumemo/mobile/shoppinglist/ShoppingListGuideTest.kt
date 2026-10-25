package com.ikumemo.mobile.shoppinglist

import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.GuidePrefs
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.guide.GuideId
import com.ikumemo.mobile.guide.MemoGuides
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.TURBINE_TIMEOUT
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import com.ikumemo.mobile.testing.testDataStore
import com.ikumemo.mobile.testing.testSettings
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** メモ画面のその場のガイド（G-02〜G-06）とれんしゅう項目（R-50〜R-54、BL-096 の完了条件）。 */
@RunWith(RobolectricTestRunner::class)
class ShoppingListGuideTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var guidePrefs: GuidePrefs
    private lateinit var viewModel: ShoppingListViewModel

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val clock = fixedClock()
        val normalizer = ProductNameNormalizer()
        guidePrefs = GuidePrefs(testDataStore())
        // はじめての案内は見終えた状態から始める
        runBlocking { guidePrefs.finishOnboarding() }
        viewModel =
            ShoppingListViewModel(
                ShoppingRepository(db, normalizer, clock),
                StoreRepository(db, testSettings(), normalizer, clock),
                guidePrefs = guidePrefs,
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

    private suspend fun shown() = guidePrefs.state.first().shown

    @Test
    fun emptyMemoShowsVoiceGuideAndOpeningVoiceCountsAsUsed() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                assertEquals(GuideId.G02_VOICE_BUTTON, awaitUntil { it.loaded && it.guide != null }.guide)

                // R-52: 音声入力のシートを出している間は出さない
                viewModel.onEvent(ShoppingListEvent.OpenVoice)
                assertNull(awaitUntil { it.sheet != null }.guide)
                viewModel.onEvent(ShoppingListEvent.CloseSheet)

                // R-51: 先に 🎤 を押したので、シートを閉じても G-02 は出ない
                awaitUntil { it.sheet == null && it.guide == null }
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(GuideId.G02_VOICE_BUTTON in shown())
        }

    @Test
    fun practiceItemIsNotRecordedAndLeadsToBoughtButtonGuide() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.loaded }
                viewModel.onEvent(ShoppingListEvent.AddByText("牛乳"))
                val practicing = awaitUntil { it.items.size == 1 && it.practice }
                assertEquals(GuideId.G03_SWIPE_PRACTICE, practicing.guide)

                // R-54: れんしゅうを「買った」にしても記録・品目・元に戻すに残らない
                viewModel.onEvent(ShoppingListEvent.MarkBought(MemoGuides.PRACTICE_ITEM_ID))
                // れんしゅうが消えるのと同時に、次は ○ の案内（G-04）になる
                val done = awaitUntil { !it.practice }
                assertEquals(GuideId.G04_BOUGHT_BUTTON, done.guide)
                assertNull(done.undo)
                assertEquals(listOf("牛乳"), done.items.map { it.displayName })
                assertEquals(listOf("牛乳"), db.productDao().getAllNames())
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(GuideId.G03_SWIPE_PRACTICE in shown())
        }

    @Test
    fun swipingRealItemFirstSkipsPracticeAndShowsUndoGuide() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.loaded }
                viewModel.onEvent(ShoppingListEvent.AddByText("牛乳"))
                val item = awaitUntil { it.items.size == 1 }.items.single()

                // R-51: 自分でスワイプできたので、れんしゅうは出さない
                viewModel.onEvent(ShoppingListEvent.MarkBought(item.id))

                // G-05: 初めて「買った」にしたら「元に戻す」を照らす（1 つだけ、R-50）
                val bought = awaitUntil { it.undo != null && !it.practice }
                assertEquals(GuideId.G05_UNDO, bought.guide)
                cancelAndIgnoreRemainingEvents()
            }
            // 「買った」の結果は G-03 の保存（別スレッドの書き込み）より先に届くことがあるため、保存されるまで待つ
            guidePrefs.state.first { GuideId.G03_SWIPE_PRACTICE in it.shown }
        }

    @Test
    fun dismissedGuideIsNotShownAgainAndDisableAllStopsEverything() =
        runTest {
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.guide == GuideId.G02_VOICE_BUTTON }
                viewModel.onEvent(ShoppingListEvent.GuideDismissed(GuideId.G02_VOICE_BUTTON))
                awaitUntil { it.guide == null }

                viewModel.onEvent(ShoppingListEvent.AddByText("牛乳"))
                awaitUntil { it.guide == GuideId.G03_SWIPE_PRACTICE }
                // R-53: 「ガイドを出さない」で、れんしゅう項目も含めてすべて止まる
                viewModel.onEvent(ShoppingListEvent.GuideDisableAll(GuideId.G03_SWIPE_PRACTICE))
                val stopped = awaitUntil { it.guide == null && !it.practice }
                assertEquals(1, stopped.items.size)
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(guidePrefs.state.first().disabled)
            assertFalse(guidePrefs.state.first().shown.isEmpty())
        }

    @Test
    fun storeChipGuideComesAfterUndoGuideWhenNoStoreSelected() =
        runTest {
            guidePrefs.markShown(GuideId.G03_SWIPE_PRACTICE)
            guidePrefs.markShown(GuideId.G04_BOUGHT_BUTTON)
            guidePrefs.markShown(GuideId.G05_UNDO)
            viewModel.uiState.test(timeout = TURBINE_TIMEOUT) {
                awaitUntil { it.loaded }
                viewModel.onEvent(ShoppingListEvent.AddByText("たまご"))
                assertEquals(GuideId.G06_STORE_CHIP, awaitUntil { it.items.isNotEmpty() && it.guide != null }.guide)

                // R-51: お店を選ぶ画面を開いたら、もう出さない
                viewModel.onEvent(ShoppingListEvent.OpenStorePicker)
                viewModel.onEvent(ShoppingListEvent.CloseSheet)
                awaitUntil { it.sheet == null && it.guide == null }
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue(GuideId.G06_STORE_CHIP in shown())
        }
}
