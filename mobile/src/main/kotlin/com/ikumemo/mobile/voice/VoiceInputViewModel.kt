package com.ikumemo.mobile.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.core.voice.VoiceTextParser
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.ShoppingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 確認カード 1 枚（PLAN §3.2）。メモにすでにある物は印を付けて既定で外す（R-01）。 */
data class VoiceCard(
    val key: Int,
    val name: String,
    val quantity: String?,
    val alreadyInMemo: Boolean,
    val included: Boolean,
)

sealed interface VoiceInputState {
    data object Idle : VoiceInputState

    /** 聞いている。認識中の文字をそのまま見せる。 */
    data class Listening(
        val partialText: String = "",
    ) : VoiceInputState

    data class Confirm(
        val cards: List<VoiceCard>,
    ) : VoiceInputState

    /** うまく聞き取れなかった（R-31）。 */
    data object NotHeard : VoiceInputState

    /** マイクの許可が拒否された（§2.10・R-44）。 */
    data object PermissionDenied : VoiceInputState

    /** 端末に音声入力の機能が無い（§2.10・R-45）。 */
    data object Unavailable : VoiceInputState

    /** メモへ入れ終わった。[count] は実際に入れた件数。 */
    data class Added(
        val count: Int,
    ) : VoiceInputState
}

sealed interface VoiceInputEvent {
    /** 聞き取りを始める。マイクの許可は画面側で得てから送る。 */
    data object Start : VoiceInputEvent

    data object PermissionDenied : VoiceInputEvent

    /** 途中で閉じた。確認カードの内容は捨てる（R-42）。 */
    data object Cancel : VoiceInputEvent

    data class ToggleCard(
        val key: Int,
    ) : VoiceInputEvent

    data class EditCard(
        val key: Int,
        val name: String,
        val quantity: String,
    ) : VoiceInputEvent

    data object AddAll : VoiceInputEvent

    /** 確認中の品を追加し、シートを閉じずに次の聞き取りを始める（BL-191、案 (d)）。追加は確認してから行う（R-42）。 */
    data object AddAllAndContinue : VoiceInputEvent

    /** 追加の結果（[VoiceInputState.Added]）を画面が受け取った。次に開くときに古い結果が残らないよう Idle へ戻す（BL-190）。 */
    data object AddedHandled : VoiceInputEvent
}

/**
 * 音声入力部品の状態（F-01・F-02）。ホームの追加シートとウィジェットの音声入力シート（W-01）で共用する。
 * 品名欄ごとのマイク（R-32、分割しない）は別の部品で扱う。
 */
class VoiceInputViewModel(
    private val repository: ShoppingRepository,
    private val engine: SpeechEngine,
) : ViewModel() {
    private val mutableState = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val state: StateFlow<VoiceInputState> = mutableState.asStateFlow()

    val isAvailable: Boolean get() = engine.isAvailable

    private var silenceJob: Job? = null

    fun onEvent(event: VoiceInputEvent) {
        when (event) {
            VoiceInputEvent.Start -> start()
            VoiceInputEvent.PermissionDenied -> mutableState.value = VoiceInputState.PermissionDenied
            VoiceInputEvent.Cancel -> cancel()
            is VoiceInputEvent.ToggleCard ->
                updateCards {
                    if (it.key == event.key) {
                        it.copy(
                            included = !it.included,
                        )
                    } else {
                        it
                    }
                }
            is VoiceInputEvent.EditCard ->
                updateCards {
                    if (it.key == event.key) {
                        it.copy(name = event.name.trim(), quantity = event.quantity.trim().ifEmpty { null })
                    } else {
                        it
                    }
                }
            VoiceInputEvent.AddAll -> addAll(continueListening = false)
            VoiceInputEvent.AddAllAndContinue -> addAll(continueListening = true)
            VoiceInputEvent.AddedHandled ->
                mutableState.update { if (it is VoiceInputState.Added) VoiceInputState.Idle else it }
        }
    }

    private fun start() {
        if (!engine.isAvailable) {
            mutableState.value = VoiceInputState.Unavailable
            return
        }
        mutableState.value = VoiceInputState.Listening()
        // 何も話さなければ 5 秒で止めて「聞き取れませんでした」を出す（R-31）。結果がすぐ返っても取り消せるよう、
        // 認識を始める前に仕掛ける
        silenceJob?.cancel()
        silenceJob =
            viewModelScope.launch {
                delay(SILENCE_TIMEOUT_MILLIS)
                engine.cancel()
                mutableState.value = VoiceInputState.NotHeard
            }
        engine.start(::onSpeechEvent)
    }

    private fun onSpeechEvent(event: SpeechEvent) {
        when (event) {
            SpeechEvent.BeginningOfSpeech -> silenceJob?.cancel()
            is SpeechEvent.Partial -> {
                silenceJob?.cancel()
                mutableState.update { if (it is VoiceInputState.Listening) it.copy(partialText = event.text) else it }
            }
            is SpeechEvent.Results -> {
                silenceJob?.cancel()
                viewModelScope.launch { showCards(event.candidates) }
            }
            is SpeechEvent.Error -> {
                silenceJob?.cancel()
                mutableState.value =
                    when (event.reason) {
                        SpeechError.PERMISSION -> VoiceInputState.PermissionDenied
                        else -> VoiceInputState.NotHeard
                    }
            }
        }
    }

    private suspend fun showCards(candidates: List<String>) {
        val parser = VoiceTextParser(knownNames = repository.products.names())
        val items = parser.parseBest(candidates)
        if (items.isEmpty()) {
            mutableState.value = VoiceInputState.NotHeard
            return
        }
        val cards =
            items.mapIndexed { index, item ->
                val inMemo = repository.findInMemo(item.name) != null
                VoiceCard(index, item.name, item.quantity, alreadyInMemo = inMemo, included = !inMemo)
            }
        mutableState.value = VoiceInputState.Confirm(cards)
    }

    private fun addAll(continueListening: Boolean) {
        val confirm = mutableState.value as? VoiceInputState.Confirm ?: return
        viewModelScope.launch {
            val added =
                confirm.cards
                    .filter { it.included && it.name.isNotBlank() }
                    .count { repository.addItem(it.name, it.quantity) is AddItemResult.Added }
            if (continueListening) start() else mutableState.value = VoiceInputState.Added(added)
        }
    }

    private fun cancel() {
        silenceJob?.cancel()
        engine.cancel()
        mutableState.value = VoiceInputState.Idle
    }

    private fun updateCards(transform: (VoiceCard) -> VoiceCard) {
        mutableState.update { if (it is VoiceInputState.Confirm) it.copy(cards = it.cards.map(transform)) else it }
    }

    override fun onCleared() {
        engine.destroy()
    }

    private companion object {
        const val SILENCE_TIMEOUT_MILLIS = 5_000L
    }
}
