package com.ikumemo.mobile.shoppinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.UndoableAction
import com.ikumemo.mobile.voice.VoiceInputSheet
import com.ikumemo.mobile.voice.VoiceInputViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun rememberPhotoStore(): PhotoStore {
    val context = LocalContext.current
    return remember(context) { PhotoStore(context) }
}

/** 一瞬光らせる時間（R-01）。 */
private const val HIGHLIGHT_MILLIS = 1_200L

/** 「元に戻す」の表示時間（F-12）。TalkBack 等の利用時は端末の推奨時間まで延ばす（R-04）。 */
private const val UNDO_MILLIS = 5_000L

@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel = viewModel(factory = AppViewModelFactory.Factory),
    voiceViewModel: VoiceInputViewModel = viewModel(factory = AppViewModelFactory.Factory),
    photoStore: PhotoStore = rememberPhotoStore(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val addSuggestions by viewModel.addSuggestions.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent
    ShoppingListContent(state = state, voiceAvailable = voiceViewModel.isAvailable, onEvent = onEvent)
    when (val sheet = state.sheet) {
        HomeSheet.Voice ->
            VoiceInputSheet(
                viewModel = voiceViewModel,
                onTypeInstead = { onEvent(ShoppingListEvent.OpenAddByText) },
                onAdded = { onEvent(ShoppingListEvent.CloseSheet) },
                onDismiss = { onEvent(ShoppingListEvent.CloseSheet) },
            )
        HomeSheet.StorePicker ->
            StorePickerSheet(
                stores = state.stores,
                selectedStoreId = state.selectedStore?.id,
                onEvent = onEvent,
            )
        HomeSheet.AddByText ->
            AddItemSheet(suggestions = addSuggestions, onEvent = onEvent)
        is HomeSheet.Edit ->
            state.items.firstOrNull { it.id == sheet.itemId }?.let { item ->
                EditItemSheet(
                    item = item,
                    stores = state.stores,
                    photoStore = photoStore,
                    onSave = { onEvent(ShoppingListEvent.SaveEdit(item.id, it)) },
                )
            }
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListContent(
    state: ShoppingListUiState,
    voiceAvailable: Boolean,
    onEvent: (ShoppingListEvent) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    MessageEffect(state.message, snackbarHostState) { onEvent(ShoppingListEvent.MessageShown) }
    UndoEffect(state.undo, snackbarHostState, onEvent)
    LaunchedEffect(state.highlightedItemId) {
        val id = state.highlightedItemId ?: return@LaunchedEffect
        val index = state.items.indexOfFirst { it.id == id }
        if (index >= 0) listState.animateScrollToItem(index)
        delay(HIGHLIGHT_MILLIS)
        onEvent(ShoppingListEvent.HighlightShown)
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(R.string.memo_title)) },
                windowInsets = WindowInsets(0),
            )
            StoreChip(
                selectedStore = state.selectedStore,
                onClick = { onEvent(ShoppingListEvent.OpenStorePicker) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (state.loaded && state.items.isEmpty()) {
                EmptyMemo(Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag(TestTags.MEMO_LIST),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        SwipeableItemRow(
                            item = item,
                            highlighted = item.id == state.highlightedItemId,
                            onClick = { onEvent(ShoppingListEvent.OpenEditor(item.id)) },
                            onBought = { onEvent(ShoppingListEvent.MarkBought(item.id)) },
                            onDismissed = { onEvent(ShoppingListEvent.Dismiss(item.id)) },
                        )
                        HorizontalDivider()
                    }
                }
            }
            AddButtons(voiceAvailable = voiceAvailable, onEvent = onEvent)
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).testTag(TestTags.SNACKBAR))
    }
}

@Composable
private fun MessageEffect(
    message: HomeMessage?,
    snackbarHostState: SnackbarHostState,
    onMessageShown: () -> Unit,
) {
    val alreadyInMemoFormat = stringResource(R.string.memo_already_in_memo)
    LaunchedEffect(message) {
        when (message) {
            is HomeMessage.AlreadyInMemo -> {
                onMessageShown()
                snackbarHostState.showSnackbar(alreadyInMemoFormat.format(message.displayName))
            }
            null -> Unit
        }
    }
}

/** 「買った：牛乳 [元に戻す]」を表示する。新しい操作が来たら前の表示は置き換わる（R-04）。 */
@Composable
private fun UndoEffect(
    offer: UndoOffer?,
    snackbarHostState: SnackbarHostState,
    onEvent: (ShoppingListEvent) -> Unit,
) {
    val boughtFormat = stringResource(R.string.memo_bought_message)
    val dismissedFormat = stringResource(R.string.memo_dismissed_message)
    val undoLabel = stringResource(R.string.memo_undo)
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(offer?.serial) {
        offer ?: return@LaunchedEffect
        val text =
            when (val action = offer.action) {
                is UndoableAction.Bought -> boughtFormat.format(action.item.displayName)
                is UndoableAction.Dismissed -> dismissedFormat.format(action.item.displayName)
            }
        val timeout =
            accessibilityManager?.calculateRecommendedTimeoutMillis(
                UNDO_MILLIS,
                containsIcons = false,
                containsText = true,
                containsControls = true,
            ) ?: UNDO_MILLIS
        snackbarHostState.currentSnackbarData?.dismiss()
        val dismissJob =
            launch {
                delay(timeout)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        val result = snackbarHostState.showSnackbar(text, undoLabel, duration = SnackbarDuration.Indefinite)
        dismissJob.cancel()
        if (result == SnackbarResult.ActionPerformed) {
            onEvent(ShoppingListEvent.Undo(offer.serial))
        } else {
            onEvent(ShoppingListEvent.UndoExpired(offer.serial))
        }
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

/** 画面下の追加ボタン。🎤 話して追加（56dp 以上、G-02 で照らす場所）と ⌨ 文字で追加（PLAN §3.1）。 */
@Composable
private fun AddButtons(
    voiceAvailable: Boolean,
    onEvent: (ShoppingListEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (voiceAvailable) {
            Button(
                onClick = { onEvent(ShoppingListEvent.OpenVoice) },
                modifier = Modifier.heightIn(min = 64.dp).widthIn(min = 220.dp).testTag(TestTags.ADD_BY_VOICE),
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = null)
                Text(
                    stringResource(R.string.memo_add_by_voice),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            TextButton(
                onClick = { onEvent(ShoppingListEvent.OpenAddByText) },
                modifier = Modifier.heightIn(min = 48.dp).testTag(TestTags.ADD_BY_TEXT),
            ) {
                Icon(Icons.Rounded.Keyboard, contentDescription = null)
                Text(stringResource(R.string.memo_add_by_text), modifier = Modifier.padding(start = 8.dp))
            }
        } else {
            // 音声入力の機能が無い端末ではマイクを出さず、文字での入力を案内する（PLAN §12）
            Text(
                stringResource(R.string.voice_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = { onEvent(ShoppingListEvent.OpenAddByText) },
                modifier = Modifier.heightIn(min = 56.dp).widthIn(min = 200.dp).testTag(TestTags.ADD_BY_TEXT),
            ) {
                Icon(Icons.Rounded.Keyboard, contentDescription = null)
                Text(stringResource(R.string.memo_add_by_text), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** UI テストとその場のガイド（PLAN §4.5）の目印。 */
object TestTags {
    const val MEMO_LIST = "memo_list"
    const val ADD_BY_TEXT = "add_by_text"

    /** 🎤 話して追加（G-02 で照らす場所）。 */
    const val ADD_BY_VOICE = "add_by_voice"
    const val ADD_SHEET_INPUT = "add_sheet_input"
    const val ADD_SHEET_SUBMIT = "add_sheet_submit"
    const val EDIT_NAME = "edit_name"
    const val EDIT_QUANTITY = "edit_quantity"
    const val EDIT_NOTE = "edit_note"
    const val EDIT_CLOSE = "edit_close"
    const val EDIT_PHOTO = "edit_photo"
    const val SNACKBAR = "snackbar"

    /** お店チップ（G-06 で照らす場所）。 */
    const val STORE_CHIP = "store_chip"
    const val STORE_NEW_INPUT = "store_new_input"
    const val STORE_NEW_SUBMIT = "store_new_submit"
    const val STORE_CLEAR = "store_clear"

    fun store(id: Long) = "store_$id"

    fun item(id: Long) = "memo_item_$id"

    fun candidate(productId: Long) = "add_candidate_$productId"

    fun usual(productId: Long) = "add_usual_$productId"

    /** ○ ボタン（G-04 で照らす場所）。 */
    fun boughtButton(id: Long) = "memo_item_bought_$id"
}
