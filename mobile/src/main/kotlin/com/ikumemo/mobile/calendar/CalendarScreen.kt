package com.ikumemo.mobile.calendar

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.designsystem.PanelEdge
import com.ikumemo.mobile.designsystem.ReadablePanel
import com.ikumemo.mobile.designsystem.panelEdge
import com.ikumemo.mobile.guide.GuideOverlay
import com.ikumemo.mobile.guide.LocalGuideTargets
import com.ikumemo.mobile.guide.rememberGuideTargets
import com.ikumemo.mobile.settings.SettingsButton
import com.ikumemo.mobile.voice.FieldMic
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    onOpenSettings: () -> Unit = {},
    showGuide: Boolean = true,
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelFactory.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val photoStore = remember(context) { PhotoStore(context) }
    CalendarContent(
        state = state,
        photoStore = photoStore,
        onEvent = viewModel::onEvent,
        showGuide = showGuide,
        onOpenSettings = onOpenSettings,
    )
    state.editing?.let { record ->
        RecordEditSheet(record = record, stores = state.stores, photoStore = photoStore, onEvent = viewModel::onEvent)
    }
    if (state.adding) {
        AddRecordSheet(
            date = state.selectedDate,
            stores = state.stores,
            photoStore = photoStore,
            onEvent = viewModel::onEvent,
        )
    }
}

@Composable
fun CalendarContent(
    state: CalendarUiState,
    photoStore: PhotoStore,
    onEvent: (CalendarEvent) -> Unit,
    showGuide: Boolean = true,
    onOpenSettings: () -> Unit = {},
) {
    var enlargedPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    val guideTargets = rememberGuideTargets()
    val snackbarHostState = remember { SnackbarHostState() }
    RecordUndoEffect(state.undo, snackbarHostState, onEvent)
    CompositionLocalProvider(LocalGuideTargets provides guideTargets) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                CalendarTopBar(searchOpen = state.searchOpen, onEvent = onEvent, onOpenSettings = onOpenSettings)
                if (state.searchOpen) {
                    SearchPane(state, onEvent)
                } else {
                    MonthPane(state, photoStore, onEvent, onPhotoClick = { enlargedPhoto = it })
                }
            }
            // 記録を消した後の「元に戻す」（F-16）
            SnackbarHost(
                snackbarHostState,
                Modifier.align(Alignment.BottomCenter).testTag(CalendarTestTags.SNACKBAR),
            )
            // 通知などから開いた直後は出さない（表示済みにもしないので、次に開いたときに出る、R-52）
            state.guide?.takeIf { showGuide }?.let { guide ->
                GuideOverlay(
                    guide = guide,
                    onDismiss = { onEvent(CalendarEvent.GuideDismissed(guide)) },
                    onDisableAll = { onEvent(CalendarEvent.GuideDisableAll(guide)) },
                )
            }
        }
    }
    enlargedPhoto?.let { photo ->
        Dialog(onDismissRequest = { enlargedPhoto = null }) {
            AsyncImage(
                model = photoStore.fileOf(photo),
                contentDescription = stringResource(R.string.edit_photo_description),
                modifier = Modifier.fillMaxWidth().clickable { enlargedPhoto = null },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopBar(
    searchOpen: Boolean,
    onEvent: (CalendarEvent) -> Unit,
    onOpenSettings: () -> Unit,
) {
    // 右は 🔍 探す → ⚙ 設定（設定はメモ・期限と同じ右端、BL-143）。文字が大きくても重ならないよう、
    // 題名は 1 行に収めて溢れたら省く（BL-137）
    TopAppBar(
        title = {
            Text(stringResource(R.string.tab_calendar), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        actions = {
            SearchToggleButton(searchOpen = searchOpen, onEvent = onEvent)
            SettingsButton(onOpenSettings, CalendarTestTags.SETTINGS)
        },
        windowInsets = WindowInsets(0),
    )
}

/**
 * 上部のバーの「🔍 探す」。メモ画面の「⚙ 設定」とそろえて文字を添える（BL-137）。
 * 検索中は「✕ やめる」にし、読み上げは「探すのをやめる」と伝える。
 */
@Composable
private fun SearchToggleButton(
    searchOpen: Boolean,
    onEvent: (CalendarEvent) -> Unit,
) {
    val description = stringResource(if (searchOpen) R.string.calendar_search_close else R.string.calendar_search)
    TextButton(
        onClick = { onEvent(if (searchOpen) CalendarEvent.CloseSearch else CalendarEvent.OpenSearch) },
        modifier =
            Modifier
                .heightIn(min = 48.dp)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .testTag(CalendarTestTags.SEARCH),
    ) {
        Icon(if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search, contentDescription = null)
        Text(
            stringResource(if (searchOpen) R.string.calendar_search_stop else R.string.calendar_search),
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** 月のカレンダーと、選んだ日の記録（F-40・F-41）。 */
@Composable
private fun MonthPane(
    state: CalendarUiState,
    photoStore: PhotoStore,
    onEvent: (CalendarEvent) -> Unit,
    onPhotoClick: (String?) -> Unit,
) {
    // 背景の写真があるときは、月の表と日の詳細（見出し〜「買った物を記録する」）にそれぞれ面を敷く（BL-130）
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ReadablePanel {
                MonthHeader(
                    month = state.month,
                    onPrevious = { onEvent(CalendarEvent.PreviousMonth) },
                    onNext = { onEvent(CalendarEvent.NextMonth) },
                    // 今月以外を見ているときだけ「今日」を出す（BL-137）
                    onToday =
                        { onEvent(CalendarEvent.GoToToday) }
                            .takeIf { state.month != YearMonth.from(state.today) },
                )
                MonthGrid(
                    month = state.month,
                    selectedDate = state.selectedDate,
                    today = state.today,
                    shoppingDays = state.shoppingDays,
                    onSelect = { onEvent(CalendarEvent.SelectDate(it)) },
                )
            }
            HorizontalDivider(Modifier.padding(top = 8.dp))
            ReadablePanel(PanelEdge.TOP) {
                Text(
                    dayTitle(state.selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
                if (state.selectedRecords.isEmpty()) {
                    Text(stringResource(R.string.calendar_no_shopping), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
        items(state.selectedRecords, key = { it.record.id }) { row ->
            ReadablePanel(PanelEdge.MIDDLE) {
                RecordRowView(
                    row = row,
                    photoStore = photoStore,
                    onClick = { onEvent(CalendarEvent.EditRecord(row.record.id)) },
                    onPhotoClick = { onPhotoClick(row.record.photoUri) },
                    modifier = Modifier.testTag(CalendarTestTags.record(row.record.id)),
                )
            }
        }
        item {
            ReadablePanel(PanelEdge.BOTTOM) {
                // あとから記録（F-17）。買い物が無い日も、ある日も記録の下に出す
                OutlinedButton(
                    onClick = { onEvent(CalendarEvent.OpenAddRecord) },
                    modifier =
                        Modifier
                            .padding(16.dp)
                            .heightIn(min = 48.dp)
                            .testTag(CalendarTestTags.ADD_RECORD),
                ) {
                    Text(stringResource(R.string.calendar_add_record))
                }
            }
        }
    }
}

@Composable
private fun SearchPane(
    state: CalendarUiState,
    onEvent: (CalendarEvent) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // 背景の写真があるときは、検索の欄と結果の一覧にそれぞれ面を敷く（BL-130）
        ReadablePanel {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onEvent(CalendarEvent.QueryChanged(it)) },
                label = { Text(stringResource(R.string.calendar_search_hint)) },
                // 話した品名でも探せる（F-42、BL-117）。編集シートの欄と同じマイクで、聞き取った文字で検索語を置き換える
                trailingIcon = {
                    FieldMic(
                        field = CalendarTestTags.SEARCH_INPUT,
                        singleLine = true,
                        value = state.query,
                        onValueChange = { onEvent(CalendarEvent.QueryChanged(it)) },
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp).testTag(CalendarTestTags.SEARCH_INPUT),
            )
            if (state.query.isNotBlank() && state.searchResults.isEmpty()) {
                Text(
                    stringResource(R.string.calendar_search_empty),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        LazyColumn {
            itemsIndexed(state.searchResults, key = { _, row -> row.record.id }) { index, row ->
                ReadablePanel(panelEdge(index, state.searchResults.size)) {
                    SearchResultRow(row, onClick = { onEvent(CalendarEvent.JumpTo(row.record.purchasedDate)) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    row: RecordRow,
    onClick: () -> Unit,
) {
    val date = row.record.purchasedDate
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(CalendarTestTags.result(row.record.id)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.record.displayName, style = MaterialTheme.typography.bodyLarge)
            row.storeName?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(stringResource(R.string.calendar_result_date, date.year, date.monthValue, date.dayOfMonth))
    }
}

@Composable
private fun RecordRowView(
    row: RecordRow,
    photoStore: PhotoStore,
    onClick: () -> Unit,
    onPhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier.fillMaxWidth().heightIn(
                min = 56.dp,
            ).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.record.displayName, style = MaterialTheme.typography.bodyLarge)
                row.record.quantity?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            val sub = listOfNotNull(row.storeName, row.record.note).joinToString("　")
            if (sub.isNotEmpty()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        row.record.photoUri?.let { photo ->
            AsyncImage(
                model = photoStore.fileOf(photo),
                contentDescription = stringResource(R.string.calendar_photo_enlarge),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onPhotoClick),
            )
        }
    }
}

@Composable
private fun dayTitle(date: LocalDate): String {
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.JAPANESE)
    return stringResource(R.string.calendar_day_title, date.monthValue, date.dayOfMonth, weekday)
}
