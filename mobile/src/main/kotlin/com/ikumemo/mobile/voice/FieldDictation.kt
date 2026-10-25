package com.ikumemo.mobile.voice

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ikumemo.mobile.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 入力欄 1 つ分の聞き取りの状態。[result] は聞き取った文字（入れたら [FieldDictationViewModel.resultConsumed]）。 */
data class FieldDictationState(
    /** 聞いている欄（欄のテストタグ）。聞いていなければ null。 */
    val listening: String? = null,
    val result: FieldText? = null,
    /** 聞き取れなかった・マイクが使えないなどで、利用者へ一度だけ知らせる文言（聞いていた欄が出す）。 */
    val message: FieldMessage? = null,
)

data class FieldMessage(
    val field: String,
    val text: Int,
)

data class FieldText(
    val field: String,
    val text: String,
)

/**
 * 各入力欄のマイク（F-04）。聞き取った文字は分割せず、そのまま欄に入れる（品名欄も分割しない、R-32）。
 * 聞くのは一度に 1 欄だけ。アプリ全体で 1 つ持ち、[LocalFieldDictation] で入力欄へ渡す。
 */
class FieldDictationViewModel(
    private val engine: SpeechEngine,
) : ViewModel() {
    val isAvailable: Boolean get() = engine.isAvailable

    private val mutableState = MutableStateFlow(FieldDictationState())
    val state: StateFlow<FieldDictationState> = mutableState.asStateFlow()

    fun start(field: String) {
        engine.cancel()
        mutableState.update { it.copy(listening = field, result = null, message = null) }
        engine.start { event -> onSpeech(field, event) }
    }

    /** 聞くのをやめる（聞いている途中でマイクをもう一度押した）。何も入れない。 */
    fun stop() {
        engine.cancel()
        mutableState.update { it.copy(listening = null) }
    }

    fun resultConsumed() = mutableState.update { it.copy(result = null) }

    fun messageShown() = mutableState.update { it.copy(message = null) }

    fun permissionDenied(field: String) =
        mutableState.update {
            it.copy(listening = null, message = FieldMessage(field, R.string.voice_permission_denied))
        }

    private fun onSpeech(
        field: String,
        event: SpeechEvent,
    ) {
        if (mutableState.value.listening != field) return
        when (event) {
            is SpeechEvent.Results -> {
                val text = event.candidates.firstOrNull()?.trim().orEmpty()
                mutableState.update {
                    if (text.isEmpty()) {
                        it.copy(listening = null, message = FieldMessage(field, R.string.voice_not_heard))
                    } else {
                        it.copy(listening = null, result = FieldText(field, text))
                    }
                }
            }
            is SpeechEvent.Error ->
                mutableState.update {
                    val message =
                        when (event.reason) {
                            SpeechError.PERMISSION -> R.string.voice_permission_denied
                            else -> R.string.voice_not_heard
                        }
                    it.copy(listening = null, message = FieldMessage(field, message))
                }
            else -> Unit
        }
    }

    override fun onCleared() {
        engine.destroy()
    }

    companion object {
        /**
         * 聞き取った文字を欄の内容へ入れる。1 行の欄（品名・数量）は置き換え、メモのような複数行の欄は空でなければ改行して足す。
         */
        fun merge(
            current: String,
            spoken: String,
            singleLine: Boolean,
        ): String =
            when {
                singleLine || current.isBlank() -> spoken
                else -> current.trimEnd() + "\n" + spoken
            }
    }
}

/** 入力欄のマイクの窓口。渡されていない画面（テストなど）ではマイクを出さない。 */
val LocalFieldDictation = staticCompositionLocalOf<FieldDictationViewModel?> { null }

/**
 * 入力欄の横のマイク（[field] は欄のテストタグ）。押すと聞き始め、聞いている間は止めるボタンになる。
 * マイクの許可が無ければここで求め、断られたら文字での入力を案内する（R-44）。
 */
@Composable
fun FieldMic(
    field: String,
    singleLine: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val dictation = LocalFieldDictation.current ?: return
    if (!dictation.isAvailable) return
    val context = LocalContext.current
    val state by dictation.state.collectAsStateWithLifecycle()
    val currentValue by rememberUpdatedState(value)
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) dictation.start(field) else dictation.permissionDenied(field)
        }
    LaunchedEffect(state.result) {
        val result = state.result ?: return@LaunchedEffect
        if (result.field != field) return@LaunchedEffect
        onValueChange(FieldDictationViewModel.merge(currentValue, result.text, singleLine))
        dictation.resultConsumed()
    }
    LaunchedEffect(state.message) {
        val message = state.message?.takeIf { it.field == field } ?: return@LaunchedEffect
        Toast.makeText(context, message.text, Toast.LENGTH_SHORT).show()
        dictation.messageShown()
    }
    val listening = state.listening == field
    IconButton(
        onClick = {
            when {
                listening -> dictation.stop()
                context.hasRecordAudioPermission() -> dictation.start(field)
                else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        modifier = Modifier.testTag(fieldMicTag(field)),
    ) {
        if (listening) {
            Icon(
                Icons.Rounded.Stop,
                contentDescription = stringResource(R.string.field_mic_stop),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(Icons.Rounded.Mic, contentDescription = stringResource(R.string.field_mic_start))
        }
    }
}

fun fieldMicTag(field: String) = "${field}_mic"
