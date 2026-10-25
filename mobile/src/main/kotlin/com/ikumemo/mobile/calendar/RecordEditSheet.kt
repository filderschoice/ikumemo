package com.ikumemo.mobile.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.RecordEdit
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.StoreEntity
import com.ikumemo.mobile.shoppinglist.EditField
import com.ikumemo.mobile.shoppinglist.PhotoSection
import com.ikumemo.mobile.shoppinglist.PlannedStore
import java.time.LocalDate

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * 記録の編集シート（F-16）。購入日・お店・数量・メモ・写真を直す、または記録を消す。
 * 消すのは取り消せないが、記録 1 件の操作のため確認は出さない（PLAN §1.3-4 の例外は F-76 だけ）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditSheet(
    record: PurchaseRecordEntity,
    stores: List<StoreEntity>,
    photoStore: PhotoStore,
    onEvent: (CalendarEvent) -> Unit,
) {
    var date by rememberSaveable(record.id) { mutableLongStateOf(record.purchasedDate.toEpochDay()) }
    var storeId by rememberSaveable(record.id) { mutableStateOf(record.storeId) }
    var quantity by rememberSaveable(record.id) { mutableStateOf(record.quantity.orEmpty()) }
    var note by rememberSaveable(record.id) { mutableStateOf(record.note.orEmpty()) }
    var photo by rememberSaveable(record.id) { mutableStateOf(record.photoUri) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val save = {
        onEvent(
            CalendarEvent.SaveRecord(record.id, RecordEdit(LocalDate.ofEpochDay(date), storeId, quantity, note, photo)),
        )
    }
    ModalBottomSheet(
        onDismissRequest = { onEvent(CalendarEvent.CloseEditor) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(record.displayName, style = MaterialTheme.typography.titleLarge)
            val day = LocalDate.ofEpochDay(date)
            OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.calendar_record_date, day.year, day.monthValue, day.dayOfMonth))
            }
            PlannedStore(stores, storeId, { storeId = it }, title = R.string.calendar_record_store)
            EditField(quantity, { quantity = it }, R.string.edit_quantity, "record_quantity", singleLine = true)
            EditField(note, { note = it }, R.string.edit_note, "record_note", singleLine = false)
            PhotoSection(photo = photo, photoStore = photoStore, onPhotoChange = { photo = it })
            DeleteAndSave(onDelete = { onEvent(CalendarEvent.DeleteRecord(record.id)) }, onSave = save)
        }
    }
    if (pickingDate) {
        RecordDatePicker(
            epochDay = date,
            onPicked = {
                date = it
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
}

@Composable
private fun DeleteAndSave(
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            onClick = onDelete,
            modifier = Modifier.heightIn(min = 48.dp).testTag(CalendarTestTags.RECORD_DELETE),
        ) {
            Text(stringResource(R.string.calendar_record_delete), color = MaterialTheme.colorScheme.error)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            Button(onClick = onSave, modifier = Modifier.heightIn(min = 48.dp).testTag(CalendarTestTags.RECORD_SAVE)) {
                Text(stringResource(R.string.calendar_record_save))
            }
        }
    }
}

/** 買った日を選ぶ（DatePicker は UTC の 0 時のミリ秒で日付を扱う）。記録の修正とあとから記録で使う。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordDatePicker(
    epochDay: Long,
    onPicked: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = epochDay * MILLIS_PER_DAY)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPicked(pickerState.selectedDateMillis?.div(MILLIS_PER_DAY) ?: epochDay) }) {
                Text(stringResource(R.string.common_done))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
    ) {
        DatePicker(state = pickerState)
    }
}
