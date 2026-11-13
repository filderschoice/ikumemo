package com.ikumemo.mobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import com.ikumemo.core.reminder.ReminderSettings
import com.ikumemo.mobile.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * お知らせの設定の行（PLAN §3）。通知の時刻と、期限当日のお知らせのオン/オフ。
 * 「操作とお知らせ」（[OperationContent]）の中に並べる（BL-143 で独立の画面をやめた）。
 */
@Composable
fun ReminderSettingsRows(
    settings: ReminderSettings,
    onEvent: (ReminderSettingsEvent) -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
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
            formatReminderTime(settings.time),
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
                Text(stringResource(R.string.common_decide))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_back)) }
        },
    )
}

/** お知らせの時刻の書き方（例 9:00）。設定の行と、期限の画面の「お知らせの時刻 9:00」で揃える。 */
fun formatReminderTime(time: LocalTime): String = time.format(TIME_FORMAT)

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

object ReminderSettingsTestTags {
    const val TIME = "reminder_settings_time"
    const val TIME_DONE = "reminder_settings_time_done"
    const val ON_DUE_DATE = "reminder_settings_on_due_date"
}
