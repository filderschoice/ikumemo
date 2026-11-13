package com.ikumemo.mobile.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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

/**
 * 結果の知らせを一番上に出し、見出し「書き出す」「読み込む」「消す」で区切る（BL-134）。
 * 「データをすべて消す」は間を空けて離し、文字を error 色にする。処理中は押した行に進み具合を出す。
 */
@Composable
fun DataContent(
    state: DataUiState,
    onEvent: (DataEvent) -> Unit,
    onExport: (ExportKind) -> Unit,
    onPickImportFile: () -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf<DataDialog?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        state.result?.let { ResultText(it) }
        SectionHeading(R.string.data_section_export, DataTestTags.HEADING_EXPORT)
        DataRow(EXPORT_ZIP_ROW, state) { onExport(ExportKind.ZIP) }
        DataRow(EXPORT_JSON_ROW, state) { onExport(ExportKind.JSON) }
        DataRow(EXPORT_CSV_ROW, state) { onExport(ExportKind.CSV) }
        SectionHeading(R.string.data_section_import, DataTestTags.HEADING_IMPORT)
        DataRow(IMPORT_ROW, state) { dialog = DataDialog.IMPORT }
        Spacer(Modifier.height(ERASE_GAP))
        HorizontalDivider()
        SectionHeading(R.string.data_section_erase, DataTestTags.HEADING_ERASE)
        DataRow(ERASE_ROW, state) { dialog = DataDialog.ERASE }
    }
    when (dialog) {
        DataDialog.IMPORT ->
            ConfirmDialog(IMPORT_CONFIRM, onDismiss = { dialog = null }) { onPickImportFile() }
        DataDialog.ERASE ->
            ConfirmDialog(ERASE_CONFIRM, onDismiss = { dialog = null }) { onEvent(DataEvent.EraseAll) }
        null -> Unit
    }
}

private val ERASE_GAP = 32.dp

@Composable
private fun ResultText(result: DataResult) {
    Text(
        stringResource(resultText(result)),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(16.dp).testTag(DataTestTags.RESULT),
    )
}

@Composable
private fun SectionHeading(
    text: Int,
    tag: String,
) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp).testTag(tag),
    )
}

/** データ画面の 1 行。[danger] は取り消せない操作（文字を error 色にする）。 */
private class DataRowSpec(
    val title: Int,
    val detail: Int,
    val tag: String,
    val task: DataTask,
    val progress: Int,
    val danger: Boolean = false,
)

private val EXPORT_ZIP_ROW =
    DataRowSpec(
        R.string.data_export_zip_title,
        R.string.data_export_zip_detail,
        DataTestTags.EXPORT_ZIP,
        DataTask.EXPORT_ZIP,
        R.string.data_progress_export,
    )

private val EXPORT_JSON_ROW =
    DataRowSpec(
        R.string.data_export_json_title,
        R.string.data_export_json_detail,
        DataTestTags.EXPORT_JSON,
        DataTask.EXPORT_JSON,
        R.string.data_progress_export,
    )

private val EXPORT_CSV_ROW =
    DataRowSpec(
        R.string.data_export_csv_title,
        R.string.data_export_csv_detail,
        DataTestTags.EXPORT_CSV,
        DataTask.EXPORT_CSV,
        R.string.data_progress_export,
    )

private val IMPORT_ROW =
    DataRowSpec(
        R.string.data_import_title,
        R.string.data_import_detail,
        DataTestTags.IMPORT,
        DataTask.IMPORT,
        R.string.data_progress_import,
    )

private val ERASE_ROW =
    DataRowSpec(
        R.string.data_erase_title,
        R.string.data_erase_detail,
        DataTestTags.ERASE,
        DataTask.ERASE,
        R.string.data_progress_erase,
        danger = true,
    )

@Composable
private fun DataRow(
    spec: DataRowSpec,
    state: DataUiState,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = !state.busy, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(spec.tag),
    ) {
        Text(
            stringResource(spec.title),
            style = MaterialTheme.typography.bodyLarge,
            color = if (spec.danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(spec.detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.running == spec.task) Progress(spec.progress)
    }
    HorizontalDivider()
}

@Composable
private fun Progress(text: Int) {
    Row(
        Modifier.padding(top = 8.dp).testTag(DataTestTags.PROGRESS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
    }
}

/** 確認ダイアログの文言とボタンのタグ。[danger] は確定ボタンを error 色にする（取り消せない操作）。 */
private class ConfirmSpec(
    val title: Int,
    val detail: Int,
    val confirm: Int,
    val confirmTag: String,
    val danger: Boolean = false,
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
        danger = true,
    )

@Composable
private fun ConfirmDialog(
    spec: ConfirmSpec,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val confirmColors =
        if (spec.danger) {
            ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.textButtonColors()
        }
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
                colors = confirmColors,
                modifier = Modifier.testTag(spec.confirmTag),
            ) { Text(stringResource(spec.confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_back)) } },
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
    const val PROGRESS = "data_progress"
    const val HEADING_EXPORT = "data_heading_export"
    const val HEADING_IMPORT = "data_heading_import"
    const val HEADING_ERASE = "data_heading_erase"
}
