package com.ikumemo.mobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.db.StoreEntity

/** お店の管理（PLAN §3）。お店ごとに「使わない」と「ほかのお店とまとめる」（R-16）。 */
@Composable
fun StoreManageScreen(
    onBack: () -> Unit,
    viewModel: StoreManageViewModel = viewModel(factory = AppViewModelFactory.Factory),
) {
    val stores by viewModel.stores.collectAsStateWithLifecycle()
    SettingsScaffold(title = R.string.settings_stores, onBack = onBack) {
        StoreManageContent(stores, viewModel::onEvent)
    }
}

/** 開いている確認の種類。対象のお店の ID とあわせて rememberSaveable で持つ（画面を回しても残る）。 */
private enum class StoreDialog { ARCHIVE, MERGE }

@Composable
fun StoreManageContent(
    stores: List<StoreEntity>,
    onEvent: (StoreManageEvent) -> Unit,
) {
    var targetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dialog by rememberSaveable { mutableStateOf<StoreDialog?>(null) }
    if (stores.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.stores_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(stores, key = { it.id }) { store ->
            StoreRow(
                store = store,
                canMerge = stores.size > 1,
                onAction = { action ->
                    targetId = store.id
                    dialog = action
                },
            )
            HorizontalDivider()
        }
    }
    val target = stores.firstOrNull { it.id == targetId }
    val close = {
        targetId = null
        dialog = null
    }
    when {
        target == null -> Unit
        dialog == StoreDialog.ARCHIVE -> ArchiveDialog(target, onEvent, close)
        dialog == StoreDialog.MERGE -> MergeDialog(target, stores - target, onEvent, close)
    }
}

@Composable
private fun StoreRow(
    store: StoreEntity,
    canMerge: Boolean,
    onAction: (StoreDialog) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 8.dp)) {
        Text(store.name, style = MaterialTheme.typography.bodyLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (canMerge) {
                TextButton(
                    onClick = { onAction(StoreDialog.MERGE) },
                    modifier = Modifier.testTag(ManageTestTags.merge(store.id)),
                ) { Text(stringResource(R.string.stores_merge)) }
            }
            TextButton(
                onClick = { onAction(StoreDialog.ARCHIVE) },
                modifier = Modifier.testTag(ManageTestTags.archive(store.id)),
            ) { Text(stringResource(R.string.stores_archive)) }
        }
    }
}

@Composable
private fun ArchiveDialog(
    store: StoreEntity,
    onEvent: (StoreManageEvent) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.stores_archive_title, store.name)) },
        text = { Text(stringResource(R.string.stores_archive_detail)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onEvent(StoreManageEvent.Archive(store.id))
                    onClose()
                },
                modifier = Modifier.testTag(ManageTestTags.ARCHIVE_CONFIRM),
            ) { Text(stringResource(R.string.stores_archive)) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.common_back)) } },
    )
}

/** まとめ先を選ぶ。選んだお店へ記録・メモを移し、元のお店は使わない印にする。 */
@Composable
private fun MergeDialog(
    store: StoreEntity,
    others: List<StoreEntity>,
    onEvent: (StoreManageEvent) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.stores_merge_title, store.name)) },
        text = {
            Column {
                Text(stringResource(R.string.stores_merge_detail))
                others.forEach { other ->
                    Text(
                        other.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    onEvent(StoreManageEvent.Merge(from = store.id, into = other.id))
                                    onClose()
                                }.padding(vertical = 12.dp)
                                .testTag(ManageTestTags.mergeInto(other.id)),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.common_back)) } },
    )
}

object ManageTestTags {
    const val ARCHIVE_CONFIRM = "stores_archive_confirm"

    fun archive(storeId: Long) = "stores_archive_$storeId"

    fun merge(storeId: Long) = "stores_merge_$storeId"

    fun mergeInto(storeId: Long) = "stores_merge_into_$storeId"

    fun product(productId: Long) = "products_visible_$productId"
}
