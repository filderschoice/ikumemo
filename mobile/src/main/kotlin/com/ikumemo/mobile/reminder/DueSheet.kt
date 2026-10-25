package com.ikumemo.mobile.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import java.time.LocalDate

private const val MILLIS_PER_DAY = 86_400_000L

/** 期限を決めるシート（F-50）。「3日後 / 1週間後 / 2週間後 / 1か月後 / 日付を選ぶ」。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DueSheet(
    target: DueTarget,
    today: LocalDate,
    onEvent: (ReminderEvent) -> Unit,
) {
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = { onEvent(ReminderEvent.CloseDue) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.reminder_due_title, target.name), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DueChoice.entries.forEach { choice ->
                    AssistChip(
                        onClick = { onEvent(ReminderEvent.SetDue(target.recordId, choice.from(today))) },
                        label = { Text(stringResource(choice.label())) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(ReminderTestTags.choice(choice)),
                    )
                }
                AssistChip(
                    onClick = { pickingDate = true },
                    label = { Text(stringResource(R.string.reminder_due_pick)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
    if (pickingDate) {
        val initial = (target.currentDue ?: today.plusDays(DueChoice.ONE_WEEK.days)).toEpochDay() * MILLIS_PER_DAY
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickingDate = false
                    pickerState.selectedDateMillis?.let {
                        onEvent(ReminderEvent.SetDue(target.recordId, LocalDate.ofEpochDay(it / MILLIS_PER_DAY)))
                    }
                }) { Text(stringResource(R.string.common_done)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { pickingDate = false },
                ) { Text(stringResource(R.string.common_close)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun DueChoice.label(): Int =
    when (this) {
        DueChoice.THREE_DAYS -> R.string.reminder_due_3days
        DueChoice.ONE_WEEK -> R.string.reminder_due_1week
        DueChoice.TWO_WEEKS -> R.string.reminder_due_2weeks
        DueChoice.ONE_MONTH -> R.string.reminder_due_1month
    }
