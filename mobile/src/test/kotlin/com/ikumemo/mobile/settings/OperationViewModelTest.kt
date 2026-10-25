package com.ikumemo.mobile.settings

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.ikumemo.core.reminder.ReminderSettings
import com.ikumemo.mobile.data.OperationPrefs
import com.ikumemo.mobile.data.OperationSettings
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.TURBINE_TIMEOUT
import com.ikumemo.mobile.testing.testDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalTime

/** BL-082 の完了条件「そうさ・おしらせの設定が保存され、通知の予約に反映される」。 */
@RunWith(RobolectricTestRunner::class)
class OperationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dataStore = testDataStore()

    @Test
    fun operationDefaultsAreOnAndChangesAreSaved() =
        runTest {
            val prefs = OperationPrefs(dataStore)
            assertEquals(OperationSettings(showBoughtButton = true, vibration = true), prefs.settings.first())
            val viewModel = OperationViewModel(prefs)

            viewModel.settings.test(timeout = TURBINE_TIMEOUT) {
                viewModel.onEvent(OperationEvent.SetShowBoughtButton(false))
                viewModel.onEvent(OperationEvent.SetVibration(false))
                while (awaitItem() != OperationSettings(showBoughtButton = false, vibration = false)) Unit
                cancelAndIgnoreRemainingEvents()
            }
            viewModel.viewModelScope.cancel()

            // 作り直しても保存した値で始まる
            assertEquals(OperationSettings(false, false), OperationPrefs(dataStore).settings.first())
        }

    @Test
    fun reminderSettingsAreSavedAndRescheduled() =
        runTest {
            val prefs = ReminderPrefs(dataStore)
            val rescheduled = MutableStateFlow(0)
            val viewModel = ReminderSettingsViewModel(prefs) { rescheduled.update { it + 1 } }

            viewModel.settings.test(timeout = TURBINE_TIMEOUT) {
                // 続けて変えても、両方とも残る
                viewModel.onEvent(ReminderSettingsEvent.SetTime(LocalTime.of(7, 30)))
                viewModel.onEvent(ReminderSettingsEvent.SetNotifyOnDueDate(false))
                val expected = ReminderSettings(time = LocalTime.of(7, 30), notifyOnDueDate = false)
                while (awaitItem() != expected) Unit
                cancelAndIgnoreRemainingEvents()
            }
            // 保存のたびに予約し直す（保存の後に呼ぶため、2 回目が終わるまで待つ）
            // runTest の仮想時間では待てないため、実時間で待つ
            withContext(Dispatchers.Default) { withTimeout(TURBINE_TIMEOUT) { rescheduled.first { it == 2 } } }
            viewModel.viewModelScope.cancel()

            assertEquals(ReminderSettings(LocalTime.of(7, 30), false), prefs.currentSettings())
            assertEquals(2, rescheduled.value)
        }
}
