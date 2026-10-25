package com.ikumemo.mobile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.core.reminder.ReminderSettings
import com.ikumemo.mobile.data.ReminderPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalTime

sealed interface ReminderSettingsEvent {
    data class SetTime(
        val time: LocalTime,
    ) : ReminderSettingsEvent

    data class SetNotifyOnDueDate(
        val enabled: Boolean,
    ) : ReminderSettingsEvent
}

/**
 * おしらせの設定（通知の時刻・当日のおしらせのオン/オフ、R-20・R-21）。保存したら次の通知を予約し直す（[reschedule]）。
 */
class ReminderSettingsViewModel(
    private val prefs: ReminderPrefs,
    private val reschedule: suspend () -> Unit,
) : ViewModel() {
    val settings: StateFlow<ReminderSettings> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ReminderSettings())

    // 続けて切り替えたときに、古い値で上書きしないよう 1 つずつ保存する
    private val writeLock = Mutex()

    fun onEvent(event: ReminderSettingsEvent) {
        viewModelScope.launch {
            writeLock.withLock {
                val current = prefs.currentSettings()
                val updated =
                    when (event) {
                        is ReminderSettingsEvent.SetTime -> current.copy(time = event.time)
                        is ReminderSettingsEvent.SetNotifyOnDueDate -> current.copy(notifyOnDueDate = event.enabled)
                    }
                prefs.update(updated)
                reschedule()
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
