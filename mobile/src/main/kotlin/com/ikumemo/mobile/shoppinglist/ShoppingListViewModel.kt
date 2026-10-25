package com.ikumemo.mobile.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.core.suggest.ProductSummary
import com.ikumemo.core.suggest.Suggestions
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.GuidePrefs
import com.ikumemo.mobile.data.ItemEdit
import com.ikumemo.mobile.data.OperationSettings
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.UndoableAction
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.data.db.StoreEntity
import com.ikumemo.mobile.guide.GuideId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** ホームに重ねて開くシート。 */
sealed interface HomeSheet {
    /** 話して追加（F-01）。 */
    data object Voice : HomeSheet

    /** 文字で追加（F-03）。 */
    data object AddByText : HomeSheet

    /** いつものお店から選ぶ（F-14）。 */
    data object StorePicker : HomeSheet

    /** 項目の編集（F-04）。 */
    data class Edit(
        val itemId: Long,
    ) : HomeSheet
}

/** メモ画面の操作（単方向データフロー、PLAN §4.1）。 */
sealed interface ShoppingListEvent {
    data object OpenVoice : ShoppingListEvent

    data object OpenAddByText : ShoppingListEvent

    data class OpenEditor(
        val itemId: Long,
    ) : ShoppingListEvent

    data object CloseSheet : ShoppingListEvent

    data object OpenStorePicker : ShoppingListEvent

    data class SelectStore(
        val storeId: Long,
    ) : ShoppingListEvent

    /** 新しいお店を登録して選ぶ（F-30）。 */
    data class AddStore(
        val name: String,
    ) : ShoppingListEvent

    /** 選ばない（選択の解除）。 */
    data object ClearStore : ShoppingListEvent

    /** 文字で追加する。追加できたらシートは開いたまま、続けて入れられるようにする。 */
    data class AddByText(
        val name: String,
    ) : ShoppingListEvent

    /** 追加シートの入力が変わった（入力中の候補を出し直す、F-20）。 */
    data class AddQueryChanged(
        val text: String,
    ) : ShoppingListEvent

    /** 候補・いつものチップを押した（F-20・F-21）。押すたびに 1 件ずつ入る。 */
    data class AddProduct(
        val name: String,
    ) : ShoppingListEvent

    data class SaveEdit(
        val itemId: Long,
        val edit: ItemEdit,
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

    /** 項目まで送って一瞬光らせる（メモ一覧ウィジェットの行から開いたとき、R-40）。 */
    data class Highlight(
        val itemId: Long,
    ) : ShoppingListEvent

    data object HighlightShown : ShoppingListEvent

    data object MessageShown : ShoppingListEvent

    /** その場のガイドを「わかった」で閉じた。 */
    data class GuideDismissed(
        val guide: GuideId,
    ) : ShoppingListEvent

    /** 吹き出しの「ガイドを出さない」（R-53）。 */
    data class GuideDisableAll(
        val guide: GuideId,
    ) : ShoppingListEvent
}

/** 追加シートの候補（F-20 入力中の候補、F-21 いつもの）。 */
data class AddSuggestions(
    val query: String = "",
    val candidates: List<ProductSummary> = emptyList(),
    val usual: List<ProductSummary> = emptyList(),
)

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
    /** 一瞬光らせる項目（重複時の既存項目 R-01、ウィジェットで押した項目 R-40）。 */
    val highlightedItemId: Long? = null,
    val message: HomeMessage? = null,
    val undo: UndoOffer? = null,
    /** 選んでいるいつものお店。選んでいなければ null（F-14）。 */
    val selectedStore: StoreEntity? = null,
    /** いつものお店の候補（最近使った順、F-31）。 */
    val stores: List<StoreEntity> = emptyList(),
    /** そうさの設定（○ の表示・振動）。 */
    val operation: OperationSettings = OperationSettings(),
    /** いま出すその場のガイド（F-70、1 つだけ R-50）。 */
    val guide: GuideId? = null,
    /** メモの一番上に「れんしゅう：にんじん」を出すか（G-03・R-54）。DB には入れない。 */
    val practice: Boolean = false,
)

private data class LocalState(
    val sheet: HomeSheet? = null,
    val highlightedItemId: Long? = null,
    val message: HomeMessage? = null,
    val undo: UndoOffer? = null,
)

/**
 * メモ画面。[guidePrefs] を渡すとその場のガイド（G-02〜G-06）と「れんしゅう」項目を出す（渡さなければ出さない）。
 */
class ShoppingListViewModel(
    private val repository: ShoppingRepository,
    private val storeRepository: StoreRepository,
    private val suggestions: Suggestions = Suggestions(),
    operation: Flow<OperationSettings> = flowOf(OperationSettings()),
    private val guidePrefs: GuidePrefs? = null,
) : ViewModel() {
    private val local = MutableStateFlow(LocalState())
    private val addQuery = MutableStateFlow("")

    // 書き込みは操作した順に 1 つずつ行う（続けて追加したときにメモの並びが入れ替わらないように）
    private val writeLock = Mutex()
    private var undoSerial = 0L

    private val guides = MemoGuideController(guidePrefs, viewModelScope)

    val uiState: StateFlow<ShoppingListUiState> =
        combine(
            combine(
                repository.observeItems(),
                local,
                storeRepository.observeSelectedStore(),
                storeRepository.observeActiveStores(),
                operation,
            ) { items, state, selectedStore, stores, operationSettings ->
                ShoppingListUiState(
                    items = items,
                    loaded = true,
                    sheet = state.sheet,
                    highlightedItemId = state.highlightedItemId,
                    message = state.message,
                    undo = state.undo,
                    selectedStore = selectedStore,
                    stores = stores,
                    operation = operationSettings,
                )
            },
            guides.state,
        ) { ui, guideState -> guides.decide(ui, guideState) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ShoppingListUiState())

    /** メモにある物を除いた候補。 */
    val addSuggestions: StateFlow<AddSuggestions> =
        combine(addQuery, repository.products.observeSummaries(), repository.observeItems()) { query, products, items ->
            val inMemo = items.map { it.productId }.toSet()
            AddSuggestions(
                query = query,
                candidates = suggestions.candidates(query, products, inMemo),
                usual = suggestions.usual(products, inMemo),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AddSuggestions())

    fun onEvent(event: ShoppingListEvent) {
        if (guides.handle(event)) return
        if (!handleScreenEvent(event)) handleDataEvent(event)
    }

    /** 画面だけの状態（シート・光らせる項目・知らせ）を変える操作。扱った場合は true。 */
    private fun handleScreenEvent(event: ShoppingListEvent): Boolean {
        when (event) {
            ShoppingListEvent.OpenVoice -> local.update { it.copy(sheet = HomeSheet.Voice) }
            ShoppingListEvent.OpenAddByText -> local.update { it.copy(sheet = HomeSheet.AddByText) }
            ShoppingListEvent.OpenStorePicker -> local.update { it.copy(sheet = HomeSheet.StorePicker) }
            is ShoppingListEvent.OpenEditor -> local.update { it.copy(sheet = HomeSheet.Edit(event.itemId)) }
            ShoppingListEvent.CloseSheet -> {
                addQuery.value = ""
                local.update { it.copy(sheet = null) }
            }
            is ShoppingListEvent.UndoExpired ->
                local.update { if (it.undo?.serial == event.serial) it.copy(undo = null) else it }
            is ShoppingListEvent.Highlight -> local.update { it.copy(highlightedItemId = event.itemId) }
            ShoppingListEvent.HighlightShown -> local.update { it.copy(highlightedItemId = null) }
            ShoppingListEvent.MessageShown -> local.update { it.copy(message = null) }
            is ShoppingListEvent.AddQueryChanged -> addQuery.value = event.text
            else -> return false
        }
        return true
    }

    /** DB・設定を書き換える操作。 */
    private fun handleDataEvent(event: ShoppingListEvent) {
        when (event) {
            is ShoppingListEvent.AddByText -> {
                addQuery.value = ""
                write { handle(repository.addItem(event.name)) }
            }
            is ShoppingListEvent.AddProduct -> {
                addQuery.value = ""
                write { handle(repository.addItem(event.name)) }
            }
            is ShoppingListEvent.SaveEdit -> saveEdit(event)
            is ShoppingListEvent.MarkBought -> writeUndoable { markBought(event.itemId) }
            is ShoppingListEvent.Dismiss -> writeUndoable { repository.dismiss(event.itemId) }
            is ShoppingListEvent.Undo -> undo(event.serial)
            is ShoppingListEvent.SelectStore -> chooseStore { storeRepository.select(event.storeId) }
            is ShoppingListEvent.AddStore ->
                chooseStore { storeRepository.addStore(event.name)?.let { storeRepository.select(it.id) } }
            ShoppingListEvent.ClearStore -> chooseStore { storeRepository.clearSelection() }
            else -> Unit
        }
    }

    /** 選んでいるお店を付けて「買った」にする（F-14）。解除の条件を満たしていればお店なしで記録する（R-06）。 */
    private suspend fun markBought(itemId: Long): UndoableAction? {
        val storeId = storeRepository.currentStoreIdForPurchase()
        val action = repository.markBought(itemId, storeId)
        if (action != null && storeId != null) storeRepository.onBought(storeId)
        return action
    }

    private fun chooseStore(block: suspend () -> Unit) =
        write {
            block()
            local.update { it.copy(sheet = null) }
        }

    private fun saveEdit(event: ShoppingListEvent.SaveEdit) =
        write {
            handle(repository.updateItem(event.itemId, event.edit))
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
