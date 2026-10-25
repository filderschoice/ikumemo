package com.ikumemo.mobile.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.db.IkumemoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/** 読み込みの結果。[Invalid] はファイルの問題、[Failed] は読み込み中の失敗。どちらも今のデータは変わらない。 */
sealed interface ImportResult {
    data object Success : ImportResult

    data class Invalid(
        val reason: String,
    ) : ImportResult

    data object Failed : ImportResult
}

/**
 * 書き出したファイルの読み込み（F-72）。今のデータをすべて置き換える。
 * 外から来たファイルのため、まず一時フォルダへ取り出して全部を検査し（[BackupValidator]）、
 * 通ったときだけ DB を 1 つのトランザクションで入れ替える。検査や DB の入れ替えで失敗しても今のデータは変わらない。
 */
class BackupImporter(
    context: Context,
    private val database: IkumemoDatabase,
    private val settings: BackupSettingsStore,
    private val photoStore: PhotoStore,
    private val backgroundStore: PhotoStore,
    private val onImported: suspend () -> Unit,
) {
    private val workRoot = File(context.applicationContext.cacheDir, WORK_DIR)

    suspend fun import(input: InputStream): ImportResult =
        withContext(Dispatchers.IO) {
            val work = File(workRoot, UUID.randomUUID().toString()).apply { mkdirs() }
            try {
                when (val staged = stage(input, work)) {
                    is Staged.Bad -> ImportResult.Invalid(staged.reason)
                    is Staged.Ok -> {
                        apply(staged.file, work)
                        onImported()
                        ImportResult.Success
                    }
                }
            } catch (_: IOException) {
                ImportResult.Failed
            } catch (_: IllegalStateException) {
                ImportResult.Failed
            } finally {
                work.deleteRecursively()
            }
        }

    private sealed interface Staged {
        data class Ok(
            val file: BackupFile,
        ) : Staged

        data class Bad(
            val reason: String,
        ) : Staged
    }

    /** 入力を一時ファイルへ写し、JSON を読んで検査し、ZIP なら写真を取り出す。 */
    private fun stage(
        input: InputStream,
        work: File,
    ): Staged {
        val raw = File(work, "input")
        raw.outputStream().use { LimitedIo.copy(input, it, MAX_TOTAL_BYTES) }
        val json =
            if (isZip(
                    raw,
                )
            ) {
                readZip(raw, work)
            } else {
                raw.inputStream().use { LimitedIo.readText(it, MAX_JSON_BYTES) }
            }
        return if (json == null) Staged.Bad("ikumemo.json がありません") else parse(json, work)
    }

    private fun parse(
        json: String,
        work: File,
    ): Staged {
        val parsed =
            try {
                JSON.decodeFromString<BackupFile>(json)
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        val reason = if (parsed == null) "ファイルの形式が正しくありません" else BackupValidator.validate(parsed)
        return if (parsed == null || reason != null) {
            Staged.Bad(
                reason.orEmpty(),
            )
        } else {
            Staged.Ok(withoutMissingPhotos(parsed, work))
        }
    }

    private fun isZip(raw: File): Boolean {
        val head = ByteArray(ZIP_MAGIC.size)
        val size = raw.inputStream().use { it.read(head) }
        return size == ZIP_MAGIC.size && head.contentEquals(ZIP_MAGIC)
    }

    /** ZIP から JSON を読み、写真と背景画像を [work] の下へ取り出す。JSON が無ければ null。 */
    private fun readZip(
        raw: File,
        work: File,
    ): String? {
        File(work, PhotoStore.PHOTOS_DIR).mkdirs()
        File(work, PhotoStore.BACKGROUND_DIR).mkdirs()
        return ZipFile(raw).use { zip ->
            val entries = zip.entries().toList().filterNot { it.isDirectory }
            if (entries.size > MAX_ENTRIES) throw IOException("ファイルの数が多すぎます")
            entries.mapNotNull { extract(zip, it, work) }.firstOrNull()
        }
    }

    /** JSON の項目なら中身を返す。写真と背景画像の決まった名前なら [work] へ取り出す。ほかの名前は読み飛ばす。 */
    private fun extract(
        zip: ZipFile,
        entry: ZipEntry,
        work: File,
    ): String? {
        val name = entry.name
        val isImage = BackupValidator.isPhotoPath(name) || BackupValidator.isBackgroundPath(name)
        return when {
            name == BackupExporter.JSON_ENTRY ->
                zip.getInputStream(
                    entry,
                ).use { LimitedIo.readText(it, MAX_JSON_BYTES) }
            isImage -> {
                File(work, name).outputStream().use { out ->
                    zip.getInputStream(entry).use { LimitedIo.copy(it, out, MAX_PHOTO_BYTES) }
                }
                null
            }
            else -> null
        }
    }

    /** ファイルが取り出せていない写真のパスは空にする（写真なしの JSON を読み込んだときなど）。 */
    private fun withoutMissingPhotos(
        data: BackupFile,
        work: File,
    ): BackupFile {
        val keep: (String?) -> String? = { path -> path?.takeIf { File(work, it).isFile } }
        return data.copy(
            items = data.items.map { it.copy(photoUri = keep(it.photoUri)) },
            trips = data.trips.map { it.copy(photoUri = keep(it.photoUri)) },
            records = data.records.map { it.copy(photoUri = keep(it.photoUri)) },
            settings = data.settings.copy(backgroundImage = keep(data.settings.backgroundImage)),
        )
    }

    private suspend fun apply(
        data: BackupFile,
        work: File,
    ) {
        val delete = database.backupDeleteDao()
        val insert = database.backupInsertDao()
        database.withTransaction {
            delete.deleteAll()
            insert.insertStores(data.stores.map { it.toEntity() })
            insert.insertProducts(data.products.map { it.toEntity() })
            insert.insertItems(data.items.map { it.toEntity() })
            insert.insertTrips(data.trips.map { it.toEntity() })
            insert.insertRecords(data.records.map { it.toEntity() })
            insert.insertReminders(data.reminders.map { it.toEntity() })
        }
        replaceFolder(photoStore, PhotoStore.PHOTOS_DIR, work)
        replaceFolder(backgroundStore, PhotoStore.BACKGROUND_DIR, work)
        settings.apply(data.settings)
    }

    /** [store] のフォルダを、取り出した [work] の同じ名前のフォルダの中身に入れ替える。 */
    private fun replaceFolder(
        store: PhotoStore,
        dirName: String,
        work: File,
    ) {
        store.deleteAll()
        File(work, dirName).listFiles()?.forEach { it.copyTo(store.fileOf("$dirName/${it.name}"), overwrite = true) }
    }

    private companion object {
        const val WORK_DIR = "import"
        const val MAX_JSON_BYTES = 64L * 1024 * 1024
        const val MAX_PHOTO_BYTES = 32L * 1024 * 1024
        const val MAX_TOTAL_BYTES = 2L * 1024 * 1024 * 1024
        const val MAX_ENTRIES = 100_000
        val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
