package com.ikumemo.mobile.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** 音声認識の途中経過と結果。 */
sealed interface SpeechEvent {
    data object BeginningOfSpeech : SpeechEvent

    data class Partial(
        val text: String,
    ) : SpeechEvent

    /** 認識候補（N-best、確からしい順）。 */
    data class Results(
        val candidates: List<String>,
    ) : SpeechEvent

    data class Error(
        val reason: SpeechError,
    ) : SpeechEvent
}

enum class SpeechError {
    /** 聞き取れなかった・何も話さなかった（R-31）。 */
    NOT_HEARD,

    /** マイクの許可がない。 */
    PERMISSION,

    /** それ以外（認識サービスの不調など）。 */
    OTHER,
}

/** 音声認識の窓口。テストでは偽物に差し替える。 */
interface SpeechEngine {
    /** 端末に音声認識の機能があるか（PLAN §12、`<queries>` の宣言が必要）。 */
    val isAvailable: Boolean

    fun start(onEvent: (SpeechEvent) -> Unit)

    fun stop()

    fun cancel()

    fun destroy()
}

/**
 * Android 標準の [SpeechRecognizer] を使う実装（PLAN §4.1）。日本語固定（R-30）、オンデバイス認識を優先する。
 * 録音は認識サービスの中で行われ、本アプリは音声を保存しない。メインスレッドから呼ぶ。
 */
class AndroidSpeechEngine(
    private val context: Context,
) : SpeechEngine {
    private var recognizer: SpeechRecognizer? = null

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(onEvent: (SpeechEvent) -> Unit) {
        val target = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        target.setRecognitionListener(Listener(onEvent))
        target.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            },
        )
    }

    override fun stop() {
        recognizer?.stopListening()
    }

    override fun cancel() {
        recognizer?.cancel()
    }

    override fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private class Listener(
        private val onEvent: (SpeechEvent) -> Unit,
    ) : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = onEvent(SpeechEvent.BeginningOfSpeech)

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            val reason =
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NOT_HEARD
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.PERMISSION
                    else -> SpeechError.OTHER
                }
            onEvent(SpeechEvent.Error(reason))
        }

        override fun onResults(results: Bundle?) {
            val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            onEvent(
                if (candidates.isEmpty()) SpeechEvent.Error(SpeechError.NOT_HEARD) else SpeechEvent.Results(candidates),
            )
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.let { onEvent(SpeechEvent.Partial(it)) }
        }

        override fun onEvent(
            eventType: Int,
            params: Bundle?,
        ) = Unit
    }

    private companion object {
        const val LANGUAGE = "ja-JP"
        const val MAX_RESULTS = 5
    }
}
