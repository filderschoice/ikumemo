package com.ikumemo.mobile.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.core.share.SharedTextParser
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.ShoppingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 共有された文から取り出した 1 品物。[included] が false の物は追加しない。 */
data class ShareImportRow(
    val key: Int,
    val name: String,
    val quantity: String?,
    val alreadyInMemo: Boolean,
    val included: Boolean,
)

sealed interface ShareImportState {
    /** 文を読んでいる途中。 */
    data object Loading : ShareImportState

    /** チェック付きの一覧で確かめている。 */
    data class Review(
        val rows: List<ShareImportRow>,
    ) : ShareImportState {
        /** [key] の行のチェックを切り替えた一覧。 */
        fun toggled(key: Int): Review =
            copy(rows = rows.map { row -> if (row.key == key) row.copy(included = !row.included) else row })
    }

    /** 取り込める品物が無かった（空の文など）。 */
    data object Empty : ShareImportState

    /** 追加した。[count] は実際にメモへ入った数。 */
    data class Added(
        val count: Int,
    ) : ShareImportState
}

sealed interface ShareImportEvent {
    data class Toggle(
        val key: Int,
    ) : ShareImportEvent

    data object AddSelected : ShareImportEvent
}

/**
 * LINE などから共有された文をメモへ取り込む（F-08・D-9、BL-197）。
 * 文を [SharedTextParser] で品物に分け、既にメモにある物は最初からチェックを外す。受け取った文はログに出さない。
 */
class ShareImportViewModel(
    private val repository: ShoppingRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ShareImportState>(ShareImportState.Loading)
    val state: StateFlow<ShareImportState> = mutableState.asStateFlow()
    private var loadStarted = false

    /** 受け取った文を読む。画面の作り直し（回転など）で呼ばれ直しても、最初の 1 回だけ読む。 */
    fun load(text: String?) {
        if (loadStarted) return
        loadStarted = true
        viewModelScope.launch {
            val parser = SharedTextParser(repository.products.names())
            val items = withContext(Dispatchers.Default) { parser.parse(text) }
            val rows =
                items.mapIndexed { index, item ->
                    val inMemo = repository.findInMemo(item.name) != null
                    ShareImportRow(index, item.name, item.quantity, alreadyInMemo = inMemo, included = !inMemo)
                }
            mutableState.value = if (rows.isEmpty()) ShareImportState.Empty else ShareImportState.Review(rows)
        }
    }

    fun onEvent(event: ShareImportEvent) {
        when (event) {
            is ShareImportEvent.Toggle ->
                mutableState.update { state ->
                    if (state is ShareImportState.Review) state.toggled(event.key) else state
                }
            ShareImportEvent.AddSelected -> addSelected()
        }
    }

    private fun addSelected() {
        val review = mutableState.value as? ShareImportState.Review ?: return
        // 二度押しで 2 回追加しないよう、先に状態を進める
        mutableState.value = ShareImportState.Loading
        viewModelScope.launch {
            val added =
                review.rows
                    .filter { it.included }
                    .count { repository.addItem(it.name, it.quantity) is AddItemResult.Added }
            mutableState.value = ShareImportState.Added(added)
        }
    }
}
