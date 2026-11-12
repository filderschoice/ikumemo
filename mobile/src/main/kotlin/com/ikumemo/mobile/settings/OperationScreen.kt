package com.ikumemo.mobile.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikumemo.core.reminder.ReminderSettings
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.OperationSettings

/**
 * 操作とお知らせ（PLAN §3、BL-143 で 2 つの画面を 1 つにまとめた）。
 * ○ の表示と振動のオン/オフ、お知らせの時刻と期限の当日のお知らせ。保存先（DataStore のキー）は分けていた頃のまま。
 */
@Composable
fun OperationScreen(
    onBack: () -> Unit,
    viewModel: OperationViewModel = viewModel(factory = AppViewModelFactory.Factory),
    reminderViewModel: ReminderSettingsViewModel = viewModel(factory = AppViewModelFactory.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val reminder by reminderViewModel.settings.collectAsStateWithLifecycle()
    SettingsScaffold(title = R.string.settings_operation, onBack = onBack) {
        OperationContent(settings, viewModel::onEvent, reminder, reminderViewModel::onEvent)
    }
}

@Composable
fun OperationContent(
    settings: OperationSettings,
    onEvent: (OperationEvent) -> Unit,
    reminder: ReminderSettings = ReminderSettings(),
    onReminderEvent: (ReminderSettingsEvent) -> Unit = {},
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsSwitchRow(
            title = R.string.operation_show_bought_button,
            detail = R.string.operation_show_bought_button_detail,
            checked = settings.showBoughtButton,
            onCheckedChange = { onEvent(OperationEvent.SetShowBoughtButton(it)) },
            modifier = Modifier.testTag(OperationTestTags.SHOW_BOUGHT_BUTTON),
        )
        HorizontalDivider()
        SettingsSwitchRow(
            title = R.string.operation_vibration,
            detail = R.string.operation_vibration_detail,
            checked = settings.vibration,
            onCheckedChange = { onEvent(OperationEvent.SetVibration(it)) },
            modifier = Modifier.testTag(OperationTestTags.VIBRATION),
        )
        HorizontalDivider()
        ReminderSettingsRows(reminder, onReminderEvent)
    }
}

object OperationTestTags {
    const val SHOW_BOUGHT_BUTTON = "operation_show_bought_button"
    const val VIBRATION = "operation_vibration"
}
