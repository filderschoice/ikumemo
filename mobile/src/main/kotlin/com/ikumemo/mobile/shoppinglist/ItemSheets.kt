package com.ikumemo.mobile.shoppinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.db.ShoppingItemEntity

/** 文字で追加するシート（F-03）。追加後も開いたままにして、続けて入れられるようにする。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val submit = {
        if (text.isNotBlank()) {
            onAdd(text)
            text = ""
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.add_sheet_title))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.add_sheet_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth().testTag(TestTags.ADD_SHEET_INPUT),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.common_close))
                }
                Button(
                    onClick = submit,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.heightIn(min = 48.dp).testTag(TestTags.ADD_SHEET_SUBMIT),
                ) {
                    Text(stringResource(R.string.add_sheet_submit))
                }
            }
        }
    }
}

/** 項目の編集シート（F-04）。閉じたときに内容を保存する（確認ダイアログは出さない、PLAN §1.3-4）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemSheet(
    item: ShoppingItemEntity,
    onSave: (name: String, quantity: String, note: String) -> Unit,
) {
    var name by rememberSaveable(item.id) { mutableStateOf(item.displayName) }
    var quantity by rememberSaveable(item.id) { mutableStateOf(item.quantity.orEmpty()) }
    var note by rememberSaveable(item.id) { mutableStateOf(item.note.orEmpty()) }
    ModalBottomSheet(
        onDismissRequest = { onSave(name, quantity, note) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.edit_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.EDIT_NAME),
            )
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text(stringResource(R.string.edit_quantity)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.EDIT_QUANTITY),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.edit_note)) },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.EDIT_NOTE),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { onSave(name, quantity, note) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag(TestTags.EDIT_CLOSE),
                ) {
                    Text(stringResource(R.string.common_close))
                }
            }
        }
    }
}
