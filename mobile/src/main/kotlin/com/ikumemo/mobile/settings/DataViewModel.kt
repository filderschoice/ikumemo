package com.ikumemo.mobile.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.mobile.data.DataEraser
import com.ikumemo.mobile.data.backup.BackupExporter
import com.ikumemo.mobile.data.backup.BackupImporter
import com.ikumemo.mobile.data.backup.ExportKind
import com.ikumemo.mobile.data.backup.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** 処理を終えたときの知らせ。 */
enum class DataResult {
    ERASED,
    EXPORTED,
    EXPORT_FAILED,
    IMPORTED,
    IMPORT_INVALID,
    IMPORT_FAILED,
}

/** データ画面の状態。[busy] は処理中（二重に押せなくする）、[result] は最後の処理の知らせ。 */
data class DataUiState(
    val busy: Boolean = false,
    val result: DataResult? = null,
)

sealed interface DataEvent {
    /** データをすべて消す（F-76）。確認は画面側で済んでいる。 */
    data object EraseAll : DataEvent

    /** [uri] へ書き出す（F-72）。 */
    data class Export(
        val kind: ExportKind,
        val uri: Uri,
    ) : DataEvent

    /** [uri] のファイルを読み込み、今のデータを置き換える（F-72）。確認は画面側で済んでいる。 */
    data class Import(
        val uri: Uri,
    ) : DataEvent
}

/** データ（F-72・F-76）。書き出しと読み込みのファイルは、画面が選んだ [Uri] を [openOutput] / [openInput] で開く。 */
class DataViewModel(
    private val eraser: DataEraser,
    private val exporter: BackupExporter,
    private val importer: BackupImporter,
    private val openOutput: (Uri) -> OutputStream?,
    private val openInput: (Uri) -> InputStream?,
) : ViewModel() {
    private val _state = MutableStateFlow(DataUiState())
    val state: StateFlow<DataUiState> = _state.asStateFlow()

    fun onEvent(event: DataEvent) {
        if (_state.value.busy) return
        _state.update { DataUiState(busy = true) }
        viewModelScope.launch {
            val result =
                when (event) {
                    DataEvent.EraseAll -> erase()
                    is DataEvent.Export -> export(event)
                    is DataEvent.Import -> import(event)
                }
            _state.update { DataUiState(result = result) }
        }
    }

    private suspend fun erase(): DataResult {
        eraser.eraseAll()
        return DataResult.ERASED
    }

    private suspend fun export(event: DataEvent.Export): DataResult =
        try {
            val output = openOutput(event.uri) ?: throw IOException("開けません")
            output.use { exporter.export(event.kind, it) }
            DataResult.EXPORTED
        } catch (_: IOException) {
            DataResult.EXPORT_FAILED
        }

    private suspend fun import(event: DataEvent.Import): DataResult =
        try {
            val input = openInput(event.uri) ?: throw IOException("開けません")
            when (input.use { importer.import(it) }) {
                ImportResult.Success -> DataResult.IMPORTED
                is ImportResult.Invalid -> DataResult.IMPORT_INVALID
                ImportResult.Failed -> DataResult.IMPORT_FAILED
            }
        } catch (_: IOException) {
            DataResult.IMPORT_FAILED
        }
}
