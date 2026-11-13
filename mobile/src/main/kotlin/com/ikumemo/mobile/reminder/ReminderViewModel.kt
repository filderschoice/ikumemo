package com.ikumemo.mobile.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.core.reminder.ReminderSettings
import com.ikumemo.mobile.data.GuidePrefs
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.data.ReminderRepository
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ReminderRow
import com.ikumemo.mobile.guide.GuideId
import com.ikumemo.mobile.guide.GuideScene
import com.ikumemo.mobile.guide.ScreenGuides
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

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
    /** いま出すその場のガイド（G-08）。 */
    val guide: GuideId? = null,
    /** 「期限なしにしました：品名 ／ 元に戻す」の表示（BL-142）。 */
    val undo: NoDueUndo? = null,
    /** お知らせの時刻（設定値。一覧の上の「お知らせの時刻 9:00」に出す、BL-143）。 */
    val notifyTime: LocalTime = ReminderSettings.DEFAULT_TIME,
)

/** 「期限なし」で外した記録を元に戻す申し出。[serial] は押すたびに変わり、前の表示を置き換える。 */
data class NoDueUndo(
    val serial: Long,
    val recordId: Long,
    val name: String,
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

    /** 最近買った物の「期限なし」。記録は消さず、一覧から外すだけ（BL-142）。 */
    data class NoDue(
        val recordId: Long,
        val name: String,
    ) : ReminderEvent

    /** 「期限なし」の「元に戻す」を押した。 */
    data class UndoNoDue(
        val serial: Long,
    ) : ReminderEvent

    /** 「元に戻す」を出す時間が過ぎた。 */
    data class NoDueUndoExpired(
        val serial: Long,
    ) : ReminderEvent

    /** 画面に戻ったときに通知の許可を確かめ直す。 */
    data class NotificationStateChanged(
        val enabled: Boolean,
    ) : ReminderEvent

    /** 通知の許可を求めた。 */
    data object PermissionRequested : ReminderEvent

    data class GuideDismissed(
        val guide: GuideId,
    ) : ReminderEvent

    data class GuideDisableAll(
        val guide: GuideId,
    ) : ReminderEvent
}

private data class ReminderLocal(
    val notificationsEnabled: Boolean = true,
    val dueTarget: DueTarget? = null,
    val permissionRequests: Int = 0,
    val undo: NoDueUndo? = null,
)

/** おしらせ画面（F-50 期限の設定、F-53 期限が近い順の一覧、R-22 期限切れの片付け、R-29 通知オフの案内）。 */
class ReminderViewModel(
    private val repository: ReminderRepository,
    private val prefs: ReminderPrefs,
    private val clock: Clock,
    guidePrefs: GuidePrefs? = null,
) : ViewModel() {
    private val local = MutableStateFlow(ReminderLocal())
    private val guides = ScreenGuides(guidePrefs, viewModelScope)
    private var undoSerial = 0L

    init {
        viewModelScope.launch { local.update { it.copy(permissionRequests = prefs.permissionRequestCount()) } }
    }

    val uiState: StateFlow<ReminderUiState> =
        combine(
            repository.observeRecentWithoutReminder(),
            repository.observePending(),
            local,
            guides.state,
            combine(prefs.hiddenRecentIds, prefs.settings.map { it.time }, ::Pair),
        ) { allRecent, pending, state, guideState, (hidden, notifyTime) ->
            // 「期限なし」で外した記録を除く（BL-142）
            val recent = allRecent.filterNot { it.id in hidden }
            ReminderUiState(
                today = LocalDate.now(clock),
                recent = recent,
                pending = pending,
                notificationsEnabled = state.notificationsEnabled,
                dueTarget = state.dueTarget,
                loaded = true,
                permissionRequests = state.permissionRequests,
                undo = state.undo,
                notifyTime = notifyTime,
                // G-08: 初めて「買った」にした後に開いた（外した記録を除いた最近買った物があり、「期限を決める」を照らせる）。
                // 期限の設定シートを出している間は出さない（R-52）
                guide =
                    guides.next(
                        if (recent.isEmpty()) emptyList() else listOf(GuideId.G08_DUE_DATE),
                        guideState,
                        GuideScene(sheetOpen = state.dueTarget != null),
                    ),
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            ReminderUiState(LocalDate.now(clock)),
        )

    fun onEvent(event: ReminderEvent) {
        when (event) {
            is ReminderEvent.OpenDue -> {
                // R-51: 自分で「期限を決める」を押したら、G-08 は要らない
                guides.markShown(GuideId.G08_DUE_DATE)
                local.update { it.copy(dueTarget = event.target) }
            }
            ReminderEvent.CloseDue -> local.update { it.copy(dueTarget = null) }
            is ReminderEvent.SetDue ->
                viewModelScope.launch {
                    repository.setDue(event.recordId, event.dueDate)
                    local.update { it.copy(dueTarget = null) }
                }
            is ReminderEvent.UsedUp -> viewModelScope.launch { repository.markUsedUp(event.reminderId) }
            is ReminderEvent.Discarded -> viewModelScope.launch { repository.markDiscarded(event.reminderId) }
            is ReminderEvent.NoDue -> hideRecent(event)
            is ReminderEvent.UndoNoDue -> undoNoDue(event.serial)
            is ReminderEvent.NoDueUndoExpired -> expireUndo(event.serial)
            is ReminderEvent.NotificationStateChanged -> local.update { it.copy(notificationsEnabled = event.enabled) }
            ReminderEvent.PermissionRequested ->
                viewModelScope.launch {
                    prefs.countPermissionRequest()
                    local.update { it.copy(permissionRequests = prefs.permissionRequestCount()) }
                }
            is ReminderEvent.GuideDismissed -> guides.markShown(event.guide)
            is ReminderEvent.GuideDisableAll -> guides.disableAll(event.guide)
        }
    }

    /**
     * 「期限なし」。外した id を保存し、7 日を過ぎた・期限を決めた・消したなどで一覧に出なくなった id は同時に掃除する。
     */
    private fun hideRecent(event: ReminderEvent.NoDue) {
        undoSerial++
        local.update { it.copy(undo = NoDueUndo(undoSerial, event.recordId, event.name)) }
        viewModelScope.launch {
            val alive = repository.observeRecentWithoutReminder().first().map { it.id }.toSet()
            prefs.updateHiddenRecentIds { (it + event.recordId) intersect alive }
        }
    }

    private fun undoNoDue(serial: Long) {
        val undo = local.value.undo?.takeIf { it.serial == serial } ?: return
        local.update { it.copy(undo = null) }
        viewModelScope.launch { prefs.updateHiddenRecentIds { it - undo.recordId } }
    }

    private fun expireUndo(serial: Long) {
        local.update { if (it.undo?.serial == serial) it.copy(undo = null) else it }
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
