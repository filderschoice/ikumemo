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
import java.time.format.TextStyle
import java.util.Locale

private const val MILLIS_PER_DAY = 86_400_000L

/** 期限を決めるシート（F-50）。「3日後 / 1週間後 / 2週間後 / 1か月後 / 日付を選ぶ」。候補は下の行に日付（曜日付き）を出す。 */
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
                    DueChoiceChip(choice, today) { onEvent(ReminderEvent.SetDue(target.recordId, choice.from(today))) }
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
                }) { Text(stringResource(R.string.common_decide)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { pickingDate = false },
                ) { Text(stringResource(R.string.common_back)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** 候補のチップ。上の行に「1週間後」など、下の行に日付（曜日付き）を出す（BL-132）。 */
@Composable
private fun DueChoiceChip(
    choice: DueChoice,
    today: LocalDate,
    onClick: () -> Unit,
) {
    val date = choice.dateParts(today)
    AssistChip(
        onClick = onClick,
        label = {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(stringResource(choice.label()))
                Text(
                    stringResource(R.string.reminder_due_choice_date, date.month, date.day, date.weekday),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
        modifier = Modifier.heightIn(min = 48.dp).testTag(ReminderTestTags.choice(choice)),
    )
}

/** 候補のチップの 2 行目に出す日付の値（BL-132）。曜日は端末の言語によらず日本語（カレンダーの日の見出しと同じ）。 */
data class DueDateParts(
    val month: Int,
    val day: Int,
    val weekday: String,
)

/** 今日から数えたこの候補の日付を、月・日・曜日に分ける。月をまたぐ・月末の丸めは [DueChoice.from] に従う。 */
fun DueChoice.dateParts(today: LocalDate): DueDateParts {
    val date = from(today)
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.JAPANESE)
    return DueDateParts(date.monthValue, date.dayOfMonth, weekday)
}

private fun DueChoice.label(): Int =
    when (this) {
        DueChoice.THREE_DAYS -> R.string.reminder_due_3days
        DueChoice.ONE_WEEK -> R.string.reminder_due_1week
        DueChoice.TWO_WEEKS -> R.string.reminder_due_2weeks
        DueChoice.ONE_MONTH -> R.string.reminder_due_1month
    }
