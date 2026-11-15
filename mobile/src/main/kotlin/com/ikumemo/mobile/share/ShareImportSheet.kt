package com.ikumemo.mobile.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ikumemo.mobile.R

/** 共有された文の取り込みシート（BL-197）。チェック付きの一覧で確かめてから追加する。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareImportSheet(
    viewModel: ShareImportViewModel,
    onAdded: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        (state as? ShareImportState.Added)?.let { onAdded(it.count) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ShareImportContent(state = state, onEvent = viewModel::onEvent, onCancel = onDismiss)
    }
}

@Composable
fun ShareImportContent(
    state: ShareImportState,
    onEvent: (ShareImportEvent) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.share_import_title), style = MaterialTheme.typography.titleMedium)
        when (state) {
            is ShareImportState.Review -> Review(state.rows, onEvent, onCancel)
            ShareImportState.Empty -> {
                Text(
                    stringResource(R.string.share_import_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag(ShareImportTestTags.EMPTY),
                )
                TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.common_close))
                }
            }
            else -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun ColumnScope.Review(
    rows: List<ShareImportRow>,
    onEvent: (ShareImportEvent) -> Unit,
    onCancel: () -> Unit,
) {
    Text(stringResource(R.string.share_import_hint), style = MaterialTheme.typography.bodyMedium)
    // 品物が多くてもボタンが画面の外へ出ないよう、一覧だけをスクロールさせる
    LazyColumn(Modifier.weight(1f, fill = false)) {
        items(rows, key = { it.key }) { row -> RowItem(row, onToggle = { onEvent(ShareImportEvent.Toggle(row.key)) }) }
    }
    val includedCount = rows.count { it.included }
    Button(
        onClick = { onEvent(ShareImportEvent.AddSelected) },
        enabled = includedCount > 0,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(ShareImportTestTags.ADD),
    ) {
        Text(
            if (includedCount > 0) {
                stringResource(R.string.voice_add_count, includedCount)
            } else {
                stringResource(R.string.voice_add_all)
            },
        )
    }
    TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
        Text(stringResource(R.string.share_import_cancel))
    }
}

/** 1 品物の行。行全体を押すとチェックが切り替わる（読み上げはチェックボックスとして読む）。 */
@Composable
private fun RowItem(
    row: ShareImportRow,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = row.included, role = Role.Checkbox, onValueChange = { onToggle() })
                .testTag(ShareImportTestTags.row(row.key)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = row.included, onCheckedChange = null)
        Text(
            row.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        row.quantity?.let { Text(it, modifier = Modifier.padding(horizontal = 8.dp)) }
        if (row.alreadyInMemo) {
            Text(
                stringResource(R.string.voice_already_in_memo),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

object ShareImportTestTags {
    const val ADD = "share_import_add"
    const val EMPTY = "share_import_empty"

    fun row(key: Int) = "share_import_row_$key"
}
