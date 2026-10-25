package com.ikumemo.mobile.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.backup.ExportKind
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** データ（PLAN §3）。書き出し・読み込み（F-72）とデータをすべて消す（F-76）。 */
@Composable
fun DataScreen(
    onBack: () -> Unit,
    viewModel: DataViewModel = viewModel(factory = AppViewModelFactory.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val zipLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let { viewModel.onEvent(DataEvent.Export(ExportKind.ZIP, it)) }
        }
    val jsonLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { viewModel.onEvent(DataEvent.Export(ExportKind.JSON, it)) }
        }
    val csvLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let { viewModel.onEvent(DataEvent.Export(ExportKind.CSV, it)) }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.onEvent(DataEvent.Import(it)) }
        }
    SettingsScaffold(title = R.string.settings_data, onBack = onBack) {
        val stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        DataContent(
            state = state,
            onEvent = viewModel::onEvent,
            onExport = { kind ->
                when (kind) {
                    ExportKind.ZIP -> zipLauncher.launch("ikumemo-$stamp.zip")
                    ExportKind.JSON -> jsonLauncher.launch("ikumemo-$stamp.json")
                    ExportKind.CSV -> csvLauncher.launch("ikumemo-records-$stamp.csv")
                }
            },
            onPickImportFile = { importLauncher.launch(arrayOf("*/*")) },
        )
    }
}

private enum class DataDialog { IMPORT, ERASE }

@Composable
fun DataContent(
    state: DataUiState,
    onEvent: (DataEvent) -> Unit,
    onExport: (ExportKind) -> Unit,
    onPickImportFile: () -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf<DataDialog?>(null) }
    val enabled = !state.busy
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        DataRow(R.string.data_export_zip_title, R.string.data_export_zip_detail, enabled, DataTestTags.EXPORT_ZIP) {
            onExport(ExportKind.ZIP)
        }
        DataRow(R.string.data_export_json_title, R.string.data_export_json_detail, enabled, DataTestTags.EXPORT_JSON) {
            onExport(ExportKind.JSON)
        }
        DataRow(R.string.data_export_csv_title, R.string.data_export_csv_detail, enabled, DataTestTags.EXPORT_CSV) {
            onExport(ExportKind.CSV)
        }
        DataRow(R.string.data_import_title, R.string.data_import_detail, enabled, DataTestTags.IMPORT) {
            dialog = DataDialog.IMPORT
        }
        DataRow(R.string.data_erase_title, R.string.data_erase_detail, enabled, DataTestTags.ERASE) {
            dialog = DataDialog.ERASE
        }
        state.result?.let {
            Text(
                stringResource(resultText(it)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp).testTag(DataTestTags.RESULT),
            )
        }
    }
    when (dialog) {
        DataDialog.IMPORT ->
            ConfirmDialog(IMPORT_CONFIRM, onDismiss = { dialog = null }) { onPickImportFile() }
        DataDialog.ERASE ->
            ConfirmDialog(ERASE_CONFIRM, onDismiss = { dialog = null }) { onEvent(DataEvent.EraseAll) }
        null -> Unit
    }
}

@Composable
private fun DataRow(
    title: Int,
    detail: Int,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(tag),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}

/** 確認ダイアログの文言とボタンのタグ。 */
private class ConfirmSpec(
    val title: Int,
    val detail: Int,
    val confirm: Int,
    val confirmTag: String,
)

private val IMPORT_CONFIRM =
    ConfirmSpec(
        R.string.data_import_confirm_title,
        R.string.data_import_confirm_detail,
        R.string.data_import_confirm,
        DataTestTags.IMPORT_CONFIRM,
    )

private val ERASE_CONFIRM =
    ConfirmSpec(
        R.string.data_erase_confirm_title,
        R.string.data_erase_confirm_detail,
        R.string.data_erase_confirm,
        DataTestTags.ERASE_CONFIRM,
    )

@Composable
private fun ConfirmDialog(
    spec: ConfirmSpec,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(spec.title)) },
        text = { Text(stringResource(spec.detail)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onConfirm()
                },
                modifier = Modifier.testTag(spec.confirmTag),
            ) { Text(stringResource(spec.confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.reminder_settings_cancel)) } },
    )
}

private fun resultText(result: DataResult): Int =
    when (result) {
        DataResult.ERASED -> R.string.data_result_erased
        DataResult.EXPORTED -> R.string.data_result_exported
        DataResult.EXPORT_FAILED -> R.string.data_result_export_failed
        DataResult.IMPORTED -> R.string.data_result_imported
        DataResult.IMPORT_INVALID -> R.string.data_result_import_invalid
        DataResult.IMPORT_FAILED -> R.string.data_result_import_failed
    }

object DataTestTags {
    const val EXPORT_ZIP = "data_export_zip"
    const val EXPORT_JSON = "data_export_json"
    const val EXPORT_CSV = "data_export_csv"
    const val IMPORT = "data_import"
    const val IMPORT_CONFIRM = "data_import_confirm"
    const val ERASE = "data_erase"
    const val ERASE_CONFIRM = "data_erase_confirm"
    const val RESULT = "data_result"
}
