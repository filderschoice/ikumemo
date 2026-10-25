package com.ikumemo.mobile.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.mobile.data.NewRecord
import com.ikumemo.mobile.data.RecordEdit
import com.ikumemo.mobile.data.RecordRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.StoreEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/** 記録 1 件と、そのお店の名前（使わない印のお店も名前は残す、R-16）。 */
data class RecordRow(
    val record: PurchaseRecordEntity,
    val storeName: String?,
)

data class CalendarUiState(
    val month: YearMonth,
    val selectedDate: LocalDate,
    val today: LocalDate,
    /** 買い物のあった日と、その日のお店（ドットの色、F-40）。お店なしは null。 */
    val shoppingDays: Map<LocalDate, List<Long?>> = emptyMap(),
    /** 選んだ日の記録（F-41）。 */
    val selectedRecords: List<RecordRow> = emptyList(),
    val searchOpen: Boolean = false,
    val query: String = "",
    val searchResults: List<RecordRow> = emptyList(),
    /** 編集中の記録（F-16）。 */
    val editing: PurchaseRecordEntity? = null,
    /** あとから記録のシートを開いているか（F-17）。 */
    val adding: Boolean = false,
    val stores: List<StoreEntity> = emptyList(),
)

sealed interface CalendarEvent {
    data object PreviousMonth : CalendarEvent

    data object NextMonth : CalendarEvent

    data class SelectDate(
        val date: LocalDate,
    ) : CalendarEvent

    data object OpenSearch : CalendarEvent

    data object CloseSearch : CalendarEvent

    data class QueryChanged(
        val text: String,
    ) : CalendarEvent

    /** 検索結果から該当日へ移る（F-42）。 */
    data class JumpTo(
        val date: LocalDate,
    ) : CalendarEvent

    data class EditRecord(
        val recordId: Long,
    ) : CalendarEvent

    data object CloseEditor : CalendarEvent

    data class SaveRecord(
        val recordId: Long,
        val edit: RecordEdit,
    ) : CalendarEvent

    data class DeleteRecord(
        val recordId: Long,
    ) : CalendarEvent

    /** 選んだ日に、メモに入れていなかった物を記録する（F-17）。 */
    data object OpenAddRecord : CalendarEvent

    data object CloseAddRecord : CalendarEvent

    data class AddRecord(
        val record: NewRecord,
    ) : CalendarEvent
}

private data class CalendarLocal(
    val month: YearMonth,
    val selectedDate: LocalDate,
    val searchOpen: Boolean = false,
    val query: String = "",
    val editingId: Long? = null,
    val adding: Boolean = false,
)

/** カレンダー（F-40 月表示、F-41 日の詳細、F-42 検索、F-16 記録の修正・削除、F-17 あとから記録）。日曜始まり。 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val records: RecordRepository,
    stores: StoreRepository,
    private val clock: Clock,
) : ViewModel() {
    private val today get() = LocalDate.now(clock)
    private val local = MutableStateFlow(CalendarLocal(YearMonth.from(today), today))

    private val monthRecords =
        local
            .map { it.month }
            .distinctUntilChanged()
            .flatMapLatest { month -> records.observeBetween(month.atDay(1), month.atEndOfMonth()) }

    private val searchResults =
        local
            .map { it.query }
            .distinctUntilChanged()
            .mapLatest { records.search(it) }

    val uiState: StateFlow<CalendarUiState> =
        combine(local, monthRecords, searchResults, stores.observeAllStores()) { state, inMonth, found, allStores ->
            val names = allStores.associate { it.id to it.name }
            val rows = { list: List<PurchaseRecordEntity> -> list.map { RecordRow(it, it.storeId?.let(names::get)) } }
            CalendarUiState(
                month = state.month,
                selectedDate = state.selectedDate,
                today = today,
                shoppingDays =
                    inMonth.groupBy { it.purchasedDate }.mapValues {
                            (_, day) ->
                        day.map { it.storeId }.distinct()
                    },
                selectedRecords = rows(inMonth.filter { it.purchasedDate == state.selectedDate }),
                searchOpen = state.searchOpen,
                query = state.query,
                searchResults = if (state.searchOpen) rows(found) else emptyList(),
                editing = state.editingId?.let { id -> inMonth.firstOrNull { it.id == id } },
                adding = state.adding,
                stores = allStores.filter { !it.archived },
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            CalendarUiState(YearMonth.from(today), today, today),
        )

    fun onEvent(event: CalendarEvent) {
        when (event) {
            CalendarEvent.PreviousMonth -> showMonth(local.value.month.minusMonths(1))
            CalendarEvent.NextMonth -> showMonth(local.value.month.plusMonths(1))
            is CalendarEvent.SelectDate -> local.update { it.copy(selectedDate = event.date) }
            CalendarEvent.OpenSearch -> local.update { it.copy(searchOpen = true) }
            CalendarEvent.CloseSearch -> local.update { it.copy(searchOpen = false, query = "") }
            is CalendarEvent.QueryChanged -> local.update { it.copy(query = event.text) }
            is CalendarEvent.JumpTo ->
                local.update {
                    it.copy(
                        month = YearMonth.from(event.date),
                        selectedDate = event.date,
                        searchOpen = false,
                        query = "",
                    )
                }
            else -> onRecordEvent(event)
        }
    }

    /** 記録の修正・削除（F-16）とあとから記録（F-17）のシートの操作。 */
    private fun onRecordEvent(event: CalendarEvent) {
        when (event) {
            is CalendarEvent.EditRecord -> local.update { it.copy(editingId = event.recordId) }
            CalendarEvent.CloseEditor -> local.update { it.copy(editingId = null) }
            is CalendarEvent.SaveRecord -> saveRecord(event)
            is CalendarEvent.DeleteRecord -> deleteRecord(event.recordId)
            CalendarEvent.OpenAddRecord -> local.update { it.copy(adding = true) }
            CalendarEvent.CloseAddRecord -> local.update { it.copy(adding = false) }
            is CalendarEvent.AddRecord -> addRecord(event.record)
            else -> Unit
        }
    }

    /** 月を切り替える。今月なら今日、ほかの月なら 1 日を選ぶ。 */
    private fun showMonth(month: YearMonth) {
        val selected = if (month == YearMonth.from(today)) today else month.atDay(1)
        local.update { it.copy(month = month, selectedDate = selected) }
    }

    private fun saveRecord(event: CalendarEvent.SaveRecord) {
        viewModelScope.launch {
            records.updateRecord(event.recordId, event.edit)
            // 購入日を別の月へ直したときも、直した記録が見えるようその日へ移る
            local.update {
                it.copy(
                    editingId = null,
                    month = YearMonth.from(event.edit.purchasedDate),
                    selectedDate = event.edit.purchasedDate,
                )
            }
        }
    }

    private fun addRecord(record: NewRecord) {
        viewModelScope.launch {
            // 品名が空なら記録せず、シートを開いたままにする
            records.addRecord(record) ?: return@launch
            // 買った日を別の日・月へ直して記録したときも、記録した日が見えるようその日へ移る
            local.update {
                it.copy(
                    adding = false,
                    month = YearMonth.from(record.purchasedDate),
                    selectedDate = record.purchasedDate,
                )
            }
        }
    }

    private fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            records.deleteRecord(recordId)
            local.update { it.copy(editingId = null) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
