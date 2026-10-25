package com.ikumemo.mobile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.mobile.data.DataEraser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** データ画面の状態。[busy] は処理中（二重に押せなくする）、[erased] は消し終えた知らせ。 */
data class DataUiState(
    val busy: Boolean = false,
    val erased: Boolean = false,
)

sealed interface DataEvent {
    /** データをすべて消す（F-76）。確認は画面側で済んでいる。 */
    data object EraseAll : DataEvent
}

/** データ（F-76）。 */
class DataViewModel(
    private val eraser: DataEraser,
) : ViewModel() {
    private val _state = MutableStateFlow(DataUiState())
    val state: StateFlow<DataUiState> = _state.asStateFlow()

    fun onEvent(event: DataEvent) {
        when (event) {
            DataEvent.EraseAll -> eraseAll()
        }
    }

    private fun eraseAll() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, erased = false) }
        viewModelScope.launch {
            try {
                eraser.eraseAll()
            } finally {
                _state.update { it.copy(busy = false, erased = true) }
            }
        }
    }
}
