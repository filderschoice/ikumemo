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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.ikumemo.mobile.data.NewRecord
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.db.StoreEntity
import com.ikumemo.mobile.shoppinglist.EditField
import com.ikumemo.mobile.shoppinglist.PhotoSection
import com.ikumemo.mobile.shoppinglist.PlannedStore
import java.time.LocalDate

/**
 * あとから記録のシート（F-17）。メモに入れていなかった物（衝動買い・ついで買い）を、カレンダーで選んだ日に
 * 買った物として記録する。買った日はシートの中でも直せる。品名が空のうちは記録できない。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordSheet(
    date: LocalDate,
    stores: List<StoreEntity>,
    photoStore: PhotoStore,
    onEvent: (CalendarEvent) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableLongStateOf(date.toEpochDay()) }
    var storeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var quantity by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = { onEvent(CalendarEvent.CloseAddRecord) },
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
            Text(stringResource(R.string.calendar_add_record), style = MaterialTheme.typography.titleLarge)
            EditField(name, { name = it }, R.string.edit_name, CalendarTestTags.ADD_RECORD_NAME, singleLine = true)
            val picked = LocalDate.ofEpochDay(day)
            OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.calendar_record_date, picked.year, picked.monthValue, picked.dayOfMonth))
            }
            PlannedStore(stores, storeId, { storeId = it }, title = R.string.calendar_record_store)
            EditField(quantity, { quantity = it }, R.string.edit_quantity, "add_record_quantity", singleLine = true)
            EditField(note, { note = it }, R.string.edit_note, "add_record_note", singleLine = false)
            PhotoSection(photo = photo, photoStore = photoStore, onPhotoChange = { photo = it })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = {
                        onEvent(
                            CalendarEvent.AddRecord(NewRecord(name, picked, storeId, quantity, note, photo)),
                        )
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.heightIn(min = 48.dp).testTag(CalendarTestTags.ADD_RECORD_SAVE),
                ) {
                    Text(stringResource(R.string.calendar_add_record_save))
                }
            }
        }
    }
    if (pickingDate) {
        RecordDatePicker(
            epochDay = day,
            onPicked = {
                day = it
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
}
