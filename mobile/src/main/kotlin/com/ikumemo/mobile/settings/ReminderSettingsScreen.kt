package com.ikumemo.mobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikumemo.core.reminder.ReminderSettings
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** おしらせの設定（PLAN §3）。通知の時刻と、期限当日のおしらせのオン/オフ。 */
@Composable
fun ReminderSettingsScreen(
    onBack: () -> Unit,
    viewModel: ReminderSettingsViewModel = viewModel(factory = AppViewModelFactory.Factory),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsScaffold(title = R.string.settings_reminder, onBack = onBack) {
        ReminderSettingsContent(settings, viewModel::onEvent)
    }
}

@Composable
fun ReminderSettingsContent(
    settings: ReminderSettings,
    onEvent: (ReminderSettingsEvent) -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { picking = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag(ReminderSettingsTestTags.TIME),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text(stringResource(R.string.reminder_settings_time), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.reminder_settings_time_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                settings.time.format(TIME_FORMAT),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        HorizontalDivider()
        SettingsSwitchRow(
            title = R.string.reminder_settings_on_due_date,
            detail = R.string.reminder_settings_on_due_date_detail,
            checked = settings.notifyOnDueDate,
            onCheckedChange = { onEvent(ReminderSettingsEvent.SetNotifyOnDueDate(it)) },
            modifier = Modifier.testTag(ReminderSettingsTestTags.ON_DUE_DATE),
        )
    }
    if (picking) {
        TimePickerDialog(
            initial = settings.time,
            onPicked = {
                picking = false
                onEvent(ReminderSettingsEvent.SetTime(it))
            },
            onDismiss = { picking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onPicked: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_settings_pick_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(
                onClick = { onPicked(LocalTime.of(state.hour, state.minute)) },
                modifier = Modifier.testTag(ReminderSettingsTestTags.TIME_DONE),
            ) {
                Text(stringResource(R.string.common_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.reminder_settings_cancel)) }
        },
    )
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

object ReminderSettingsTestTags {
    const val TIME = "reminder_settings_time"
    const val TIME_DONE = "reminder_settings_time_done"
    const val ON_DUE_DATE = "reminder_settings_on_due_date"
}
