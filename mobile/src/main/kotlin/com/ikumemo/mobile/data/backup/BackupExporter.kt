package com.ikumemo.mobile.data.backup

import androidx.room.withTransaction
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.db.IkumemoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.time.Clock
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** 書き出しの種類。[ZIP] は写真と背景画像つき、[JSON] は写真なし、[CSV] は買った記録だけ（表計算用）。 */
enum class ExportKind { ZIP, JSON, CSV }

/** データの書き出し（F-72）。 */
class BackupExporter(
    private val database: IkumemoDatabase,
    private val settings: BackupSettingsStore,
    private val photoStore: PhotoStore,
    private val backgroundStore: PhotoStore,
    private val clock: Clock,
) {
    /** [output] へ書く。閉じるのは呼び出し側。 */
    suspend fun export(
        kind: ExportKind,
        output: OutputStream,
    ) {
        withContext(Dispatchers.IO) {
            val data = snapshot(withPhotos = kind == ExportKind.ZIP)
            when (kind) {
                ExportKind.JSON -> output.write(JSON.encodeToString(data).toByteArray())
                ExportKind.CSV -> output.write(RecordCsv.build(data.records, data.stores).toByteArray())
                ExportKind.ZIP -> writeZip(data, output)
            }
            output.flush()
        }
    }

    private suspend fun snapshot(withPhotos: Boolean): BackupFile {
        val dao = database.backupReadDao()
        val currentSettings = settings.read()
        val base =
            database.withTransaction {
                BackupFile(
                    exportedAt = clock.millis(),
                    stores = dao.stores().map { it.toBackup() },
                    products = dao.products().map { it.toBackup() },
                    items = dao.items().map { it.toBackup() },
                    trips = dao.trips().map { it.toBackup() },
                    records = dao.records().map { it.toBackup() },
                    reminders = dao.reminders().map { it.toBackup() },
                    settings = currentSettings,
                )
            }
        return base.keepingPhotos(withPhotos)
    }

    /** 写真を含めないとき、またはファイルが無い写真のパスを空にする。 */
    private fun BackupFile.keepingPhotos(withPhotos: Boolean): BackupFile {
        val keep: (String?) -> String? = { path -> path?.takeIf { withPhotos && photoStore.fileOf(it).isFile } }
        val background = settings.backgroundImage?.takeIf { withPhotos && backgroundStore.fileOf(it).isFile }
        return copy(
            items = items.map { it.copy(photoUri = keep(it.photoUri)) },
            trips = trips.map { it.copy(photoUri = keep(it.photoUri)) },
            records = records.map { it.copy(photoUri = keep(it.photoUri)) },
            settings = settings.copy(backgroundImage = background),
        )
    }

    private fun writeZip(
        data: BackupFile,
        output: OutputStream,
    ) {
        ZipOutputStream(output).run {
            putNextEntry(ZipEntry(JSON_ENTRY))
            write(JSON.encodeToString(data).toByteArray())
            closeEntry()
            val paths =
                (data.items.map { it.photoUri } + data.trips.map { it.photoUri } + data.records.map { it.photoUri })
                    .filterNotNull()
                    .toSet() + listOfNotNull(data.settings.backgroundImage)
            paths.forEach { path ->
                putNextEntry(ZipEntry(path))
                photoStore.fileOf(path).inputStream().use { it.copyTo(this) }
                closeEntry()
            }
            finish()
        }
    }

    companion object {
        const val JSON_ENTRY = "ikumemo.json"
        private val JSON = Json { encodeDefaults = true }
    }
}
