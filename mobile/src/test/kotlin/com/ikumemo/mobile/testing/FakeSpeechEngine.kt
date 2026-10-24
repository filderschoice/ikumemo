package com.ikumemo.mobile.testing

import com.ikumemo.mobile.voice.SpeechEngine
import com.ikumemo.mobile.voice.SpeechEvent

/** テスト用の音声認識。[start] で受け取った通知先へ、テストから [emit] で結果を送る。 */
class FakeSpeechEngine(
    override var isAvailable: Boolean = true,
) : SpeechEngine {
    private var listener: ((SpeechEvent) -> Unit)? = null
    var startCount = 0
        private set
    var cancelCount = 0
        private set
    var destroyed = false
        private set

    /** start 直後に自動で送る結果（UI テスト用）。 */
    var autoResults: List<String>? = null

    override fun start(onEvent: (SpeechEvent) -> Unit) {
        listener = onEvent
        startCount++
        autoResults?.let { onEvent(SpeechEvent.Results(it)) }
    }

    override fun stop() = Unit

    override fun cancel() {
        cancelCount++
    }

    override fun destroy() {
        destroyed = true
    }

    fun emit(event: SpeechEvent) {
        requireNotNull(listener) { "start が呼ばれていない" }.invoke(event)
    }
}
