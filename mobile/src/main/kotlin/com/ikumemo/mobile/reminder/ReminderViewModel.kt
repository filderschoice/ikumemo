package com.ikumemo.mobile.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.data.ReminderRepository
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ReminderRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** 期限を決めるシートの対象。 */
data class DueTarget(
    val recordId: Long,
    val name: String,
    val currentDue: LocalDate? = null,
)

data class ReminderUiState(
    val today: LocalDate,
    /** 最近買った物（7 日以内・期限未設定、F-50）。 */
    val recent: List<PurchaseRecordEntity> = emptyList(),
    /** 期限が近い順の一覧（F-53）。 */
    val pending: List<ReminderRow> = emptyList(),
    /** 通知が許可されているか（R-29）。 */
    val notificationsEnabled: Boolean = true,
    val dueTarget: DueTarget? = null,
    val loaded: Boolean = false,
    /** 通知の許可を求めた回数（2 回断られたら端末の設定を開く、R-29）。 */
    val permissionRequests: Int = 0,
)

sealed interface ReminderEvent {
    data class OpenDue(
        val target: DueTarget,
    ) : ReminderEvent

    data object CloseDue : ReminderEvent

    data class SetDue(
        val recordId: Long,
        val dueDate: LocalDate,
    ) : ReminderEvent

    data class UsedUp(
        val reminderId: Long,
    ) : ReminderEvent

    data class Discarded(
        val reminderId: Long,
    ) : ReminderEvent

    /** 画面に戻ったときに通知の許可を確かめ直す。 */
    data class NotificationStateChanged(
        val enabled: Boolean,
    ) : ReminderEvent

    /** 通知の許可を求めた。 */
    data object PermissionRequested : ReminderEvent
}

private data class ReminderLocal(
    val notificationsEnabled: Boolean = true,
    val dueTarget: DueTarget? = null,
    val permissionRequests: Int = 0,
)

/** おしらせ画面（F-50 期限の設定、F-53 期限が近い順の一覧、R-22 期限切れの片付け、R-29 通知オフの案内）。 */
class ReminderViewModel(
    private val repository: ReminderRepository,
    private val prefs: ReminderPrefs,
    private val clock: Clock,
) : ViewModel() {
    private val local = MutableStateFlow(ReminderLocal())

    init {
        viewModelScope.launch { local.update { it.copy(permissionRequests = prefs.permissionRequestCount()) } }
    }

    val uiState: StateFlow<ReminderUiState> =
        combine(
            repository.observeRecentWithoutReminder(),
            repository.observePending(),
            local,
        ) { recent, pending, state ->
            ReminderUiState(
                today = LocalDate.now(clock),
                recent = recent,
                pending = pending,
                notificationsEnabled = state.notificationsEnabled,
                dueTarget = state.dueTarget,
                loaded = true,
                permissionRequests = state.permissionRequests,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            ReminderUiState(LocalDate.now(clock)),
        )

    fun onEvent(event: ReminderEvent) {
        when (event) {
            is ReminderEvent.OpenDue -> local.update { it.copy(dueTarget = event.target) }
            ReminderEvent.CloseDue -> local.update { it.copy(dueTarget = null) }
            is ReminderEvent.SetDue ->
                viewModelScope.launch {
                    repository.setDue(event.recordId, event.dueDate)
                    local.update { it.copy(dueTarget = null) }
                }
            is ReminderEvent.UsedUp -> viewModelScope.launch { repository.markUsedUp(event.reminderId) }
            is ReminderEvent.Discarded -> viewModelScope.launch { repository.markDiscarded(event.reminderId) }
            is ReminderEvent.NotificationStateChanged -> local.update { it.copy(notificationsEnabled = event.enabled) }
            ReminderEvent.PermissionRequested ->
                viewModelScope.launch {
                    prefs.countPermissionRequest()
                    local.update { it.copy(permissionRequests = prefs.permissionRequestCount()) }
                }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** 期限の選択肢（F-50）。日付入力をなるべく要らなくする。 */
enum class DueChoice(
    val days: Long,
) {
    THREE_DAYS(3),
    ONE_WEEK(7),
    TWO_WEEKS(14),
    ONE_MONTH(30),
    ;

    fun from(today: LocalDate): LocalDate = if (this == ONE_MONTH) today.plusMonths(1) else today.plusDays(days)
}
