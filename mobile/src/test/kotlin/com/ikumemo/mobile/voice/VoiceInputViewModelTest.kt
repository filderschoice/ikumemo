package com.ikumemo.mobile.voice

import androidx.lifecycle.viewModelScope
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.testing.FakeSpeechEngine
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VoiceInputViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var repository: ShoppingRepository
    private val engine = FakeSpeechEngine()
    private lateinit var viewModel: VoiceInputViewModel

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = ShoppingRepository(db, ProductNameNormalizer(), fixedClock())
        viewModel = VoiceInputViewModel(repository, engine)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        db.close()
    }

    /** Room の実行スレッドから戻るのを待ちながら、目的の状態になるまで進める。 */
    private fun TestScope.awaitState(predicate: (VoiceInputState) -> Boolean): VoiceInputState {
        repeat(MAX_WAIT_ROUNDS) {
            runCurrent()
            val state = viewModel.state.value
            if (predicate(state)) return state
            Thread.sleep(WAIT_MILLIS)
        }
        error("状態が変わらない: ${viewModel.state.value}")
    }

    @Test
    fun spokenTextBecomesCardsAndIsAdded() =
        runTest {
            viewModel.onEvent(VoiceInputEvent.Start)
            engine.emit(SpeechEvent.Partial("牛乳2本と"))
            assertEquals(VoiceInputState.Listening("牛乳2本と"), viewModel.state.value)

            engine.emit(SpeechEvent.Results(listOf("牛乳2本とたまご")))
            val confirm = awaitState { it is VoiceInputState.Confirm } as VoiceInputState.Confirm

            assertEquals(listOf("牛乳" to "2本", "たまご" to null), confirm.cards.map { it.name to it.quantity })
            assertTrue(confirm.cards.all { it.included })

            viewModel.onEvent(VoiceInputEvent.AddAll)
            assertEquals(VoiceInputState.Added(2), awaitState { it is VoiceInputState.Added })
            assertEquals(
                listOf("牛乳" to "2本", "たまご" to null),
                repository.observeItems().first().map {
                    it.displayName to it.quantity
                },
            )
        }

    /** BL-190。追加後に画面が結果を受け取ったら Idle へ戻り、続けて Start すると聞き取り中になる（連続 3 回）。 */
    @Test
    fun listensAgainAfterAddedIsHandled() =
        runTest {
            repeat(3) { round ->
                viewModel.onEvent(VoiceInputEvent.Start)
                assertEquals(VoiceInputState.Listening(), viewModel.state.value)
                assertEquals(round + 1, engine.startCount)
                engine.emit(SpeechEvent.Results(listOf("牛乳$round")))
                awaitState { it is VoiceInputState.Confirm }
                viewModel.onEvent(VoiceInputEvent.AddAll)
                awaitState { it is VoiceInputState.Added }
                viewModel.onEvent(VoiceInputEvent.AddedHandled)
                assertEquals(VoiceInputState.Idle, viewModel.state.value)
            }
        }

    /** BL-191。「追加して続けて話す」は追加したうえで Added を経ずに Listening へ戻り、3 回続けて使える。 */
    @Test
    fun addAndContinueAddsThenListensAgain() =
        runTest {
            viewModel.onEvent(VoiceInputEvent.Start)
            repeat(3) { round ->
                assertEquals(VoiceInputState.Listening(), viewModel.state.value)
                assertEquals(round + 1, engine.startCount)
                engine.emit(SpeechEvent.Results(listOf("牛乳$round")))
                awaitState { it is VoiceInputState.Confirm }
                viewModel.onEvent(VoiceInputEvent.AddAllAndContinue)
                awaitState { it is VoiceInputState.Listening && engine.startCount == round + 2 }
            }
            assertEquals(3, repository.observeItems().first().size)
        }

    @Test
    fun itemAlreadyInMemoIsMarkedAndExcludedByDefault() =
        runTest {
            repository.addItem("卵")
            viewModel.onEvent(VoiceInputEvent.Start)
            engine.emit(SpeechEvent.Results(listOf("牛乳とタマゴ")))

            val confirm = awaitState { it is VoiceInputState.Confirm } as VoiceInputState.Confirm

            val egg = confirm.cards.single { it.name == "タマゴ" }
            assertTrue(egg.alreadyInMemo)
            assertEquals(false, egg.included)
            viewModel.onEvent(VoiceInputEvent.AddAll)
            assertEquals(VoiceInputState.Added(1), awaitState { it is VoiceInputState.Added })
        }

    @Test
    fun cardsCanBeEditedAndExcluded() =
        runTest {
            viewModel.onEvent(VoiceInputEvent.Start)
            engine.emit(SpeechEvent.Results(listOf("牛乳とパン")))
            awaitState { it is VoiceInputState.Confirm }

            viewModel.onEvent(VoiceInputEvent.EditCard(0, "低脂肪乳", "1本"))
            viewModel.onEvent(VoiceInputEvent.ToggleCard(1))
            viewModel.onEvent(VoiceInputEvent.AddAll)

            assertEquals(VoiceInputState.Added(1), awaitState { it is VoiceInputState.Added })
            assertEquals(
                listOf("低脂肪乳" to "1本"),
                repository.observeItems().first().map { it.displayName to it.quantity },
            )
        }

    @Test
    fun silenceForFiveSecondsStopsWithNotHeard() =
        runTest {
            viewModel.onEvent(VoiceInputEvent.Start)

            advanceTimeBy(4_900)
            runCurrent()
            assertTrue(viewModel.state.value is VoiceInputState.Listening)
            advanceTimeBy(200)
            runCurrent()

            assertEquals(VoiceInputState.NotHeard, viewModel.state.value)
            assertEquals(1, engine.cancelCount)
        }

    @Test
    fun speakingCancelsSilenceTimeout() =
        runTest {
            viewModel.onEvent(VoiceInputEvent.Start)
            engine.emit(SpeechEvent.BeginningOfSpeech)

            advanceTimeBy(10_000)
            runCurrent()

            assertTrue(viewModel.state.value is VoiceInputState.Listening)
        }

    @Test
    fun recognitionErrorsAreShown() =
        runTest {
            viewModel.onEvent(VoiceInputEvent.Start)
            engine.emit(SpeechEvent.Error(SpeechError.NOT_HEARD))
            assertEquals(VoiceInputState.NotHeard, viewModel.state.value)

            viewModel.onEvent(VoiceInputEvent.Start)
            engine.emit(SpeechEvent.Error(SpeechError.PERMISSION))
            assertEquals(VoiceInputState.PermissionDenied, viewModel.state.value)
            assertEquals(2, engine.startCount)
        }

    @Test
    fun unavailableEngineIsReported() =
        runTest {
            engine.isAvailable = false

            viewModel.onEvent(VoiceInputEvent.Start)

            assertEquals(VoiceInputState.Unavailable, viewModel.state.value)
            assertEquals(0, engine.startCount)
        }

    @Test
    fun cancelDiscardsCardsWithoutAdding() =
        runTest {
            viewModel.onEvent(VoiceInputEvent.Start)
            engine.emit(SpeechEvent.Results(listOf("牛乳")))
            awaitState { it is VoiceInputState.Confirm }

            viewModel.onEvent(VoiceInputEvent.Cancel)

            assertEquals(VoiceInputState.Idle, viewModel.state.value)
            assertEquals(emptyList<Any>(), repository.observeItems().first())
        }

    private companion object {
        const val MAX_WAIT_ROUNDS = 100
        const val WAIT_MILLIS = 20L
    }
}
