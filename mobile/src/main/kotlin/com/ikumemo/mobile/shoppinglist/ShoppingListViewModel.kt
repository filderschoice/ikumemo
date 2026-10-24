package com.ikumemo.mobile.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** ホームに重ねて開くシート。 */
sealed interface HomeSheet {
    /** 文字で追加（F-03）。 */
    data object AddByText : HomeSheet

    /** 項目の編集（F-04）。 */
    data class Edit(
        val itemId: Long,
    ) : HomeSheet
}

/** 画面下に一度だけ出す知らせ。 */
sealed interface HomeMessage {
    /** 「牛乳はもうメモにあります」（R-01）。 */
    data class AlreadyInMemo(
        val displayName: String,
    ) : HomeMessage
}

data class ShoppingListUiState(
    val items: List<ShoppingItemEntity> = emptyList(),
    val loaded: Boolean = false,
    val sheet: HomeSheet? = null,
    /** 一瞬光らせる項目（重複時の既存項目、R-01）。 */
    val highlightedItemId: Long? = null,
    val message: HomeMessage? = null,
)

private data class LocalState(
    val sheet: HomeSheet? = null,
    val highlightedItemId: Long? = null,
    val message: HomeMessage? = null,
)

class ShoppingListViewModel(
    private val repository: ShoppingRepository,
) : ViewModel() {
    private val local = MutableStateFlow(LocalState())

    // 書き込みは操作した順に 1 つずつ行う（続けて追加したときにメモの並びが入れ替わらないように）
    private val writeLock = Mutex()

    val uiState: StateFlow<ShoppingListUiState> =
        combine(repository.observeItems(), local) { items, state ->
            ShoppingListUiState(
                items = items,
                loaded = true,
                sheet = state.sheet,
                highlightedItemId = state.highlightedItemId,
                message = state.message,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ShoppingListUiState())

    fun openAddByText() = local.update { it.copy(sheet = HomeSheet.AddByText) }

    fun openEditor(itemId: Long) = local.update { it.copy(sheet = HomeSheet.Edit(itemId)) }

    fun closeSheet() = local.update { it.copy(sheet = null) }

    /** 文字で追加する。追加できたらシートは開いたまま、続けて入れられるようにする。 */
    fun addByText(name: String) {
        viewModelScope.launch { writeLock.withLock { handle(repository.addItem(name)) } }
    }

    fun saveEdit(
        itemId: Long,
        name: String,
        quantity: String,
        note: String,
    ) {
        viewModelScope.launch {
            writeLock.withLock { handle(repository.updateItem(itemId, name, quantity, note)) }
            closeSheet()
        }
    }

    fun highlightShown() = local.update { it.copy(highlightedItemId = null) }

    fun messageShown() = local.update { it.copy(message = null) }

    private fun handle(result: AddItemResult) {
        if (result is AddItemResult.AlreadyInMemo) {
            local.update {
                it.copy(
                    highlightedItemId = result.itemId,
                    message = HomeMessage.AlreadyInMemo(result.displayName),
                )
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
