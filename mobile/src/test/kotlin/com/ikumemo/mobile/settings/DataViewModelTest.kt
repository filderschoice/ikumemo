package com.ikumemo.mobile.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.data.AppSettings
import com.ikumemo.mobile.data.AppearancePrefs
import com.ikumemo.mobile.data.DataEraser
import com.ikumemo.mobile.data.OperationPrefs
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.data.backup.BackupExporter
import com.ikumemo.mobile.data.backup.BackupImporter
import com.ikumemo.mobile.data.backup.BackupSettingsStore
import com.ikumemo.mobile.data.backup.ExportKind
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.TURBINE_TIMEOUT
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import com.ikumemo.mobile.testing.testDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * BL-114 の完了条件。データの書き出し・読み込み・消去の知らせと、ファイルを開けないときに落ちずに失敗を知らせること。
 * 状態は Turbine で 1 件ずつ追わず、StateFlow を実時間で「結果が出るまで」待つ（負荷で取りこぼして時間切れにならないように）。
 */
@RunWith(RobolectricTestRunner::class)
class DataViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: IkumemoDatabase
    private val dataStore = testDataStore()
    private val photos by lazy { PhotoStore(app) }
    private val backgrounds by lazy { PhotoStore(app, PhotoStore.BACKGROUND_DIR) }
    private val uri = Uri.parse("content://com.ikumemo.test/backup.json")
    private var openOutputCalls = 0
    private var openInputCalls = 0
    private var viewModel: DataViewModel? = null

    @Before
    fun setUp() {
        db = inMemoryDatabase()
    }

    @After
    fun tearDown() {
        viewModel?.viewModelScope?.cancel()
        db.close()
        File(app.filesDir, PhotoStore.PHOTOS_DIR).deleteRecursively()
        File(app.filesDir, PhotoStore.BACKGROUND_DIR).deleteRecursively()
    }

    private fun settingsStore() =
        BackupSettingsStore(
            AppSettings(dataStore),
            AppearancePrefs(dataStore),
            OperationPrefs(dataStore),
            ReminderPrefs(dataStore),
        )

    private fun exporter() = BackupExporter(db, settingsStore(), photos, backgrounds, fixedClock())

    private fun viewModel(
        openOutput: (Uri) -> OutputStream? = { ByteArrayOutputStream() },
        openInput: (Uri) -> InputStream? = { null },
    ): DataViewModel =
        DataViewModel(
            DataEraser(db, dataStore, listOf(photos, backgrounds)) {},
            exporter(),
            BackupImporter(app, db, settingsStore(), photos, backgrounds) {},
            openOutput = {
                openOutputCalls++
                openOutput(it)
            },
            openInput = {
                openInputCalls++
                openInput(it)
            },
        ).also { viewModel = it }

    /** [event] を送り、処理が終わって知らせが出るまで実時間で待つ。 */
    private suspend fun DataViewModel.run(event: DataEvent): DataResult? {
        onEvent(event)
        return awaitResult()
    }

    private suspend fun DataViewModel.awaitResult(): DataResult? =
        withContext(Dispatchers.Default) {
            withTimeout(TURBINE_TIMEOUT) { state.first { !it.busy && it.result != null } }
        }.result

    @Test
    fun secondPressWhileBusyIsIgnored() =
        runTest {
            val viewModel = viewModel()

            viewModel.onEvent(DataEvent.EraseAll)
            assertTrue(viewModel.state.value.busy)
            // 処理中に重ねて押しても、書き出しは始まらない
            viewModel.onEvent(DataEvent.Export(ExportKind.JSON, uri))
            viewModel.onEvent(DataEvent.Import(uri))

            assertEquals(DataResult.ERASED, viewModel.awaitResult())
            assertEquals(0, openOutputCalls)
            assertEquals(0, openInputCalls)
            assertFalse(viewModel.state.value.busy)
        }

    @Test
    fun exportFailsWhenFileCannotBeOpened() =
        runTest {
            val failures =
                listOf<(Uri) -> OutputStream?>(
                    { null },
                    { throw SecurityException("権限が取り消された") },
                    { throw IllegalArgumentException("扱えない Uri") },
                )
            failures.forEach { open ->
                val viewModel = viewModel(openOutput = open)
                assertEquals(DataResult.EXPORT_FAILED, viewModel.run(DataEvent.Export(ExportKind.JSON, uri)))
                viewModel.viewModelScope.cancel()
            }
            assertEquals(failures.size, openOutputCalls)
        }

    @Test
    fun importFailsWhenFileCannotBeOpened() =
        runTest {
            val failures =
                listOf<(Uri) -> InputStream?>(
                    { null },
                    { throw SecurityException("権限が取り消された") },
                    { throw IllegalArgumentException("扱えない Uri") },
                )
            failures.forEach { open ->
                val viewModel = viewModel(openInput = open)
                assertEquals(DataResult.IMPORT_FAILED, viewModel.run(DataEvent.Import(uri)))
                viewModel.viewModelScope.cancel()
            }
            assertEquals(failures.size, openInputCalls)
        }

    @Test
    fun exportImportAndEraseReportSuccess() =
        runTest {
            val written = ByteArrayOutputStream()
            val viewModel =
                viewModel(
                    openOutput = { written },
                    openInput = { ByteArrayInputStream(written.toByteArray()) },
                )

            assertEquals(DataResult.EXPORTED, viewModel.run(DataEvent.Export(ExportKind.JSON, uri)))
            assertTrue(written.size() > 0)
            assertEquals(DataResult.IMPORTED, viewModel.run(DataEvent.Import(uri)))
            assertEquals(DataResult.ERASED, viewModel.run(DataEvent.EraseAll))
        }
}
