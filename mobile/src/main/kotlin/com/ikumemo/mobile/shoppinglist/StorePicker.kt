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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.db.StoreEntity

/** メモの上部のお店チップ（F-14、G-06 で照らす場所）。「🏪 買うお店を選ぶ」／「🏪 ○○ で買う」。 */
@Composable
fun StoreChip(
    selectedStore: StoreEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                selectedStore?.let { stringResource(R.string.store_chip_selected, it.name) }
                    ?: stringResource(R.string.store_chip_unselected),
            )
        },
        leadingIcon = { Icon(Icons.Rounded.Storefront, contentDescription = null) },
        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
        modifier = modifier.heightIn(min = 48.dp).testTag(TestTags.STORE_CHIP),
    )
}

/** いつものお店から選ぶシート（F-14・F-31）。最近使った順の上位 5 件、ほかのお店、新しいお店、選ばない。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StorePickerSheet(
    stores: List<StoreEntity>,
    selectedStoreId: Long?,
    onEvent: (ShoppingListEvent) -> Unit,
) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = { onEvent(ShoppingListEvent.CloseSheet) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.store_picker_title), style = MaterialTheme.typography.titleMedium)
            val shown = if (showAll) stores else stores.take(StoreRepository.TOP_STORE_COUNT)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                shown.forEach { store ->
                    FilterChip(
                        selected = store.id == selectedStoreId,
                        onClick = { onEvent(ShoppingListEvent.SelectStore(store.id)) },
                        label = { Text(store.name) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(TestTags.store(store.id)),
                    )
                }
            }
            if (!showAll && stores.size > StoreRepository.TOP_STORE_COUNT) {
                TextButton(onClick = { showAll = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.store_picker_other))
                }
            }
            NewStoreRow(onAdd = { onEvent(ShoppingListEvent.AddStore(it)) })
            if (selectedStoreId != null) {
                TextButton(
                    onClick = { onEvent(ShoppingListEvent.ClearStore) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag(TestTags.STORE_CLEAR),
                ) {
                    Text(stringResource(R.string.store_picker_clear))
                }
            }
        }
    }
}

/** 新しいお店の名前を入れて登録する行（F-30。初めてのお店は自動で登録される）。 */
@Composable
private fun NewStoreRow(onAdd: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val add = { if (name.isNotBlank()) onAdd(name) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.store_picker_new)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
            modifier = Modifier.weight(1f).testTag(TestTags.STORE_NEW_INPUT),
        )
        Button(
            onClick = add,
            enabled = name.isNotBlank(),
            modifier = Modifier.heightIn(min = 48.dp).testTag(TestTags.STORE_NEW_SUBMIT),
        ) {
            Text(stringResource(R.string.store_picker_add))
        }
    }
}
