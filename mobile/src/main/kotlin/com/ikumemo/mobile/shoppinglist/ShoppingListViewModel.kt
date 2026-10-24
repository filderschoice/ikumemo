package com.ikumemo.mobile.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.UndoableAction
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

/** メモ画面の操作（単方向データフロー、PLAN §4.1）。 */
sealed interface ShoppingListEvent {
    data object OpenAddByText : ShoppingListEvent

    data class OpenEditor(
        val itemId: Long,
    ) : ShoppingListEvent

    data object CloseSheet : ShoppingListEvent

    /** 文字で追加する。追加できたらシートは開いたまま、続けて入れられるようにする。 */
    data class AddByText(
        val name: String,
    ) : ShoppingListEvent

    data class SaveEdit(
        val itemId: Long,
        val name: String,
        val quantity: String,
        val note: String,
    ) : ShoppingListEvent

    /** 「買った」にする（右スワイプ・○、F-10・F-13）。 */
    data class MarkBought(
        val itemId: Long,
    ) : ShoppingListEvent

    /** 「やめた」にする（左スワイプ、F-11）。 */
    data class Dismiss(
        val itemId: Long,
    ) : ShoppingListEvent

    data class Undo(
        val serial: Long,
    ) : ShoppingListEvent

    /** 「元に戻す」の表示時間が過ぎた。 */
    data class UndoExpired(
        val serial: Long,
    ) : ShoppingListEvent

    data object HighlightShown : ShoppingListEvent

    data object MessageShown : ShoppingListEvent
}

/** 画面下に一度だけ出す知らせ。 */
sealed interface HomeMessage {
    /** 「牛乳はもうメモにあります」（R-01）。 */
    data class AlreadyInMemo(
        val displayName: String,
    ) : HomeMessage
}

/** 「元に戻す」の表示（F-12）。[serial] は同じ品目を続けて操作したときも表示し直すための通し番号。 */
data class UndoOffer(
    val serial: Long,
    val action: UndoableAction,
)

data class ShoppingListUiState(
    val items: List<ShoppingItemEntity> = emptyList(),
    val loaded: Boolean = false,
    val sheet: HomeSheet? = null,
    /** 一瞬光らせる項目（重複時の既存項目、R-01）。 */
    val highlightedItemId: Long? = null,
    val message: HomeMessage? = null,
    val undo: UndoOffer? = null,
)

private data class LocalState(
    val sheet: HomeSheet? = null,
    val highlightedItemId: Long? = null,
    val message: HomeMessage? = null,
    val undo: UndoOffer? = null,
)

class ShoppingListViewModel(
    private val repository: ShoppingRepository,
) : ViewModel() {
    private val local = MutableStateFlow(LocalState())

    // 書き込みは操作した順に 1 つずつ行う（続けて追加したときにメモの並びが入れ替わらないように）
    private val writeLock = Mutex()
    private var undoSerial = 0L

    val uiState: StateFlow<ShoppingListUiState> =
        combine(repository.observeItems(), local) { items, state ->
            ShoppingListUiState(
                items = items,
                loaded = true,
                sheet = state.sheet,
                highlightedItemId = state.highlightedItemId,
                message = state.message,
                undo = state.undo,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ShoppingListUiState())

    fun onEvent(event: ShoppingListEvent) {
        when (event) {
            ShoppingListEvent.OpenAddByText -> local.update { it.copy(sheet = HomeSheet.AddByText) }
            is ShoppingListEvent.OpenEditor -> local.update { it.copy(sheet = HomeSheet.Edit(event.itemId)) }
            ShoppingListEvent.CloseSheet -> local.update { it.copy(sheet = null) }
            is ShoppingListEvent.AddByText -> write { handle(repository.addItem(event.name)) }
            is ShoppingListEvent.SaveEdit -> saveEdit(event)
            is ShoppingListEvent.MarkBought -> writeUndoable { repository.markBought(event.itemId) }
            is ShoppingListEvent.Dismiss -> writeUndoable { repository.dismiss(event.itemId) }
            is ShoppingListEvent.Undo -> undo(event.serial)
            is ShoppingListEvent.UndoExpired ->
                local.update { if (it.undo?.serial == event.serial) it.copy(undo = null) else it }
            ShoppingListEvent.HighlightShown -> local.update { it.copy(highlightedItemId = null) }
            ShoppingListEvent.MessageShown -> local.update { it.copy(message = null) }
        }
    }

    private fun saveEdit(event: ShoppingListEvent.SaveEdit) =
        write {
            handle(repository.updateItem(event.itemId, event.name, event.quantity, event.note))
            local.update { it.copy(sheet = null) }
        }

    /** 表示中の「元に戻す」を実行する。元に戻せるのは最新の 1 件だけ（R-04）。 */
    private fun undo(serial: Long) =
        write {
            val offer = local.value.undo?.takeIf { it.serial == serial } ?: return@write
            local.update { it.copy(undo = null) }
            repository.undo(offer.action)
        }

    private fun writeUndoable(block: suspend () -> UndoableAction?) =
        write {
            val action = block() ?: return@write
            undoSerial += 1
            local.update { it.copy(undo = UndoOffer(undoSerial, action)) }
        }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { writeLock.withLock { block() } }
    }

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
