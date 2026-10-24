package com.ikumemo.mobile.shoppinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.ikumemo.core.suggest.ProductSummary
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.db.ShoppingItemEntity

/**
 * 文字で追加するシート（F-03）。入力中の候補（F-20）と、よく買う物の「いつもの」チップ（F-21）を出す。
 * チップは押すたびに 1 件ずつ入るので、続けて押せば複数件を入れられる。追加後もシートは開いたままにする。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddItemSheet(
    suggestions: AddSuggestions,
    onEvent: (ShoppingListEvent) -> Unit,
) {
    val text = suggestions.query
    val submit = { if (text.isNotBlank()) onEvent(ShoppingListEvent.AddByText(text)) }
    ModalBottomSheet(
        onDismissRequest = { onEvent(ShoppingListEvent.CloseSheet) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.add_sheet_title))
            OutlinedTextField(
                value = text,
                onValueChange = { onEvent(ShoppingListEvent.AddQueryChanged(it)) },
                label = { Text(stringResource(R.string.add_sheet_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth().testTag(TestTags.ADD_SHEET_INPUT),
            )
            ProductChips(suggestions.candidates, TestTags::candidate, onEvent)
            if (suggestions.usual.isNotEmpty()) {
                Text(stringResource(R.string.add_sheet_usual), style = MaterialTheme.typography.titleSmall)
                ProductChips(suggestions.usual, TestTags::usual, onEvent)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { onEvent(ShoppingListEvent.CloseSheet) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductChips(
    products: List<ProductSummary>,
    tag: (Long) -> String,
    onEvent: (ShoppingListEvent) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        products.forEach { product ->
            AssistChip(
                onClick = { onEvent(ShoppingListEvent.AddProduct(product.name)) },
                label = { Text(stringResource(R.string.add_sheet_chip, product.name)) },
                modifier = Modifier.heightIn(min = 48.dp).testTag(tag(product.id)),
            )
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
