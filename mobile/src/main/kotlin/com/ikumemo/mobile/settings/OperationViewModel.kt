package com.ikumemo.mobile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.mobile.data.OperationPrefs
import com.ikumemo.mobile.data.OperationSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OperationEvent {
    data class SetShowBoughtButton(
        val show: Boolean,
    ) : OperationEvent

    data class SetVibration(
        val enabled: Boolean,
    ) : OperationEvent
}

/** そうさ（○ の表示・振動のオン/オフ）。切り替えたらすぐ保存し、メモ画面へ反映する。 */
class OperationViewModel(
    private val prefs: OperationPrefs,
) : ViewModel() {
    val settings: StateFlow<OperationSettings> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), OperationSettings())

    fun onEvent(event: OperationEvent) {
        viewModelScope.launch {
            when (event) {
                is OperationEvent.SetShowBoughtButton -> prefs.setShowBoughtButton(event.show)
                is OperationEvent.SetVibration -> prefs.setVibration(event.enabled)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
