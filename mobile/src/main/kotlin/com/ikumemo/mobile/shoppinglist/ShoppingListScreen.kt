package com.ikumemo.mobile.shoppinglist

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import kotlinx.coroutines.delay

/** 一瞬光らせる時間（R-01）。 */
private const val HIGHLIGHT_MILLIS = 1_200L

@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel = viewModel(factory = AppViewModelFactory.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShoppingListContent(
        state = state,
        onAddByTextClick = viewModel::openAddByText,
        onItemClick = viewModel::openEditor,
        onHighlightShown = viewModel::highlightShown,
        onMessageShown = viewModel::messageShown,
    )
    when (val sheet = state.sheet) {
        HomeSheet.AddByText ->
            AddItemSheet(
                onAdd = viewModel::addByText,
                onDismiss = viewModel::closeSheet,
            )
        is HomeSheet.Edit ->
            state.items.firstOrNull { it.id == sheet.itemId }?.let { item ->
                EditItemSheet(
                    item = item,
                    onSave = { name, quantity, note -> viewModel.saveEdit(item.id, name, quantity, note) },
                )
            }
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListContent(
    state: ShoppingListUiState,
    onAddByTextClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onHighlightShown: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val alreadyInMemoFormat = stringResource(R.string.memo_already_in_memo)
    LaunchedEffect(state.message) {
        when (val message = state.message) {
            is HomeMessage.AlreadyInMemo -> {
                onMessageShown()
                snackbarHostState.showSnackbar(alreadyInMemoFormat.format(message.displayName))
            }
            null -> Unit
        }
    }
    LaunchedEffect(state.highlightedItemId) {
        val id = state.highlightedItemId ?: return@LaunchedEffect
        val index = state.items.indexOfFirst { it.id == id }
        if (index >= 0) listState.animateScrollToItem(index)
        delay(HIGHLIGHT_MILLIS)
        onHighlightShown()
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(R.string.memo_title)) },
                windowInsets = WindowInsets(0),
            )
            if (state.loaded && state.items.isEmpty()) {
                EmptyMemo(Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag(TestTags.MEMO_LIST),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        ShoppingItemRow(
                            item = item,
                            highlighted = item.id == state.highlightedItemId,
                            onClick = { onItemClick(item.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
            AddButtons(onAddByTextClick = onAddByTextClick)
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun EmptyMemo(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.home_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItemEntity,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (highlighted) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "highlight",
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(background)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(TestTags.item(item.id)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        item.quantity?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AddButtons(onAddByTextClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onAddByTextClick,
            modifier = Modifier.heightIn(min = 56.dp).widthIn(min = 200.dp).testTag(TestTags.ADD_BY_TEXT),
        ) {
            Icon(Icons.Rounded.Keyboard, contentDescription = null)
            Text(stringResource(R.string.memo_add_by_text), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** UI テストとその場のガイド（PLAN §4.5）の目印。 */
object TestTags {
    const val MEMO_LIST = "memo_list"
    const val ADD_BY_TEXT = "add_by_text"
    const val ADD_SHEET_INPUT = "add_sheet_input"
    const val ADD_SHEET_SUBMIT = "add_sheet_submit"
    const val EDIT_NAME = "edit_name"
    const val EDIT_QUANTITY = "edit_quantity"
    const val EDIT_NOTE = "edit_note"
    const val EDIT_CLOSE = "edit_close"

    fun item(id: Long) = "memo_item_$id"
}
