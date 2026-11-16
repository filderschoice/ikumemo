package com.ikumemo.mobile.data.backup

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.core.reminder.ReminderSettings
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.AppSettings
import com.ikumemo.mobile.data.AppearancePrefs
import com.ikumemo.mobile.data.OperationPrefs
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.data.db.ProductEntity
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ReminderEntity
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.data.db.ShoppingTripEntity
import com.ikumemo.mobile.data.db.StoreEntity
import com.ikumemo.mobile.designsystem.DarkMode
import com.ikumemo.mobile.designsystem.SeedSwatch
import com.ikumemo.mobile.designsystem.ThemeChoice
import com.ikumemo.mobile.designsystem.ThemePreset
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import com.ikumemo.mobile.testing.testDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** BL-102 の完了条件「書き出したファイルを読み込むと、記録・メモ・設定・写真が元どおりに戻る。壊れたファイルは拒否する」。 */
@RunWith(RobolectricTestRunner::class)
class BackupRoundTripTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: IkumemoDatabase
    private val dataStore = testDataStore()
    private val appearance = AppearancePrefs(dataStore)
    private val operation = OperationPrefs(dataStore)
    private val reminder = ReminderPrefs(dataStore)
    private val photos by lazy { PhotoStore(app) }
    private val backgrounds by lazy { PhotoStore(app, PhotoStore.BACKGROUND_DIR) }
    private var importedCalls = 0

    @Before
    fun setUp() {
        db = inMemoryDatabase()
    }

    @After
    fun tearDown() {
        db.close()
        File(app.filesDir, PhotoStore.PHOTOS_DIR).deleteRecursively()
        File(app.filesDir, PhotoStore.BACKGROUND_DIR).deleteRecursively()
    }

    private fun settingsStore() = BackupSettingsStore(AppSettings(dataStore), appearance, operation, reminder)

    private fun exporter() = BackupExporter(db, settingsStore(), photos, backgrounds, fixedClock())

    private fun importer() = BackupImporter(app, db, settingsStore(), photos, backgrounds) { importedCalls++ }

    private suspend fun seed() {
        val dao = db.backupInsertDao()
        val now = Instant.parse("2026-10-25T01:00:00Z")
        dao.insertStores(listOf(StoreEntity(id = 3, name = "近所のスーパー", createdAt = now)))
        dao.insertProducts(listOf(ProductEntity(id = 5, name = "牛乳", normalizedName = "ぎゅうにゅう", createdAt = now)))
        dao.insertItems(
            listOf(
                ShoppingItemEntity(
                    id = 7,
                    productId = 5,
                    displayName = "牛乳",
                    quantity = "2本",
                    plannedStoreId = 3,
                    photoUri = "photos/a.jpg",
                    sortOrder = 0,
                    createdAt = now,
                ),
            ),
        )
        dao.insertTrips(listOf(ShoppingTripEntity(id = 11, date = LocalDate.of(2026, 10, 20), storeId = 3)))
        dao.insertRecords(
            listOf(
                PurchaseRecordEntity(
                    id = 13, tripId = 11, productId = 5, displayName = "牛乳", note = "特売",
                    photoUri = "photos/b.jpg", purchasedDate = LocalDate.of(2026, 10, 20),
                    recordedAt = now, storeId = 3,
                ),
            ),
        )
        dao.insertReminders(
            listOf(ReminderEntity(id = 17, purchaseRecordId = 13, productId = 5, dueDate = LocalDate.of(2026, 10, 27))),
        )
        File(app.filesDir, "photos").apply { mkdirs() }
        File(app.filesDir, "photos/a.jpg").writeText("photo-a")
        File(app.filesDir, "photos/b.jpg").writeText("photo-b")
        File(app.filesDir, "background").apply { mkdirs() }
        File(app.filesDir, "background/bg.jpg").writeText("background")
        appearance.setTheme(ThemePreset.COOL)
        appearance.setDarkMode(DarkMode.DARK)
        appearance.setBackgroundImage("background/bg.jpg")
        operation.setVibration(false)
        reminder.update(ReminderSettings(time = LocalTime.of(20, 30), notifyOnDueDate = false))
    }

    private suspend fun wipe() {
        db.backupDeleteDao().deleteAll()
        File(app.filesDir, "photos").deleteRecursively()
        File(app.filesDir, "background").deleteRecursively()
        appearance.setTheme(ThemePreset.DEFAULT)
        appearance.setBackgroundImage(null)
        operation.setVibration(true)
    }

    private suspend fun exportBytes(kind: ExportKind): ByteArray =
        ByteArrayOutputStream().also { exporter().export(kind, it) }.toByteArray()

    @Test
    fun zipRestoresDataSettingsAndPhotos() =
        runTest {
            seed()
            val bytes = exportBytes(ExportKind.ZIP)
            wipe()

            val result = importer().import(ByteArrayInputStream(bytes))

            assertEquals(ImportResult.Success, result)
            val dao = db.backupReadDao()
            assertEquals(listOf("近所のスーパー"), dao.stores().map { it.name })
            assertEquals("2本", dao.items().single().quantity)
            assertEquals("photos/a.jpg", dao.items().single().photoUri)
            assertEquals("特売", dao.records().single().note)
            assertEquals(LocalDate.of(2026, 10, 27), dao.reminders().single().dueDate)
            assertEquals("photo-a", File(app.filesDir, "photos/a.jpg").readText())
            assertEquals("photo-b", File(app.filesDir, "photos/b.jpg").readText())
            assertEquals("background", File(app.filesDir, "background/bg.jpg").readText())
            val look = appearance.appearance.first()
            assertEquals(ThemePreset.COOL, look.theme)
            assertEquals(DarkMode.DARK, look.darkMode)
            assertEquals("background/bg.jpg", look.backgroundImage)
            assertEquals(false, operation.settings.first().vibration)
            assertEquals(LocalTime.of(20, 30), reminder.currentSettings().time)
            assertEquals(1, importedCalls)
        }

    @Test
    fun jsonDropsPhotosButKeepsEverythingElse() =
        runTest {
            seed()
            val bytes = exportBytes(ExportKind.JSON)
            wipe()

            assertEquals(ImportResult.Success, importer().import(ByteArrayInputStream(bytes)))

            val dao = db.backupReadDao()
            assertNull(dao.items().single().photoUri)
            assertNull(dao.records().single().photoUri)
            assertEquals("牛乳", dao.products().single().name)
            assertNull(appearance.appearance.first().backgroundImage)
            assertEquals(ThemePreset.COOL, appearance.appearance.first().theme)
        }

    @Test
    fun importedProductsAreFoundInMemoByTheCurrentNormalization() =
        runTest {
            // BL-236: ファイルの正規化名が今の正規化と違っても（手で作ったファイル・辞書が変わる前のファイル）、
            // 読み込んだメモの品名は取り込みシート・確認カードで「メモにあります」になる
            val products =
                """[{"id":1,"name":"牛乳","normalizedName":"牛乳","createdAt":0},""" +
                    """{"id":2,"name":"バナナ","normalizedName":"バナナ","createdAt":0}]"""
            val items =
                """[{"id":1,"productId":1,"displayName":"牛乳","sortOrder":0,"createdAt":0},""" +
                    """{"id":2,"productId":2,"displayName":"バナナ","sortOrder":1,"createdAt":0}]"""
            val data =
                """{"format":"ikumemo-backup","version":1,"exportedAt":0,"products":$products,"items":$items}"""

            assertEquals(ImportResult.Success, importer().import(ByteArrayInputStream(data.toByteArray())))

            val shopping = ShoppingRepository(db, ProductNameNormalizer(), fixedClock())
            assertEquals(1L, shopping.findInMemo("牛乳")?.id)
            assertEquals(2L, shopping.findInMemo("ばなな")?.id)
            assertEquals(AddItemResult.AlreadyInMemo(1, "牛乳"), shopping.addItem("牛乳"))
            assertEquals(2, db.backupReadDao().products().size)
        }

    @Test
    fun clashingNamesAfterRenormalizingKeepTheFileValues() =
        runTest {
            // BL-236: 作り直すと同じ名前になる品目どうし（辞書が変わる前に別の品目だった「卵」と「玉子」など）は、
            // 一意の制約を破らないようファイルの正規化名のまま入れる。ほかの品目は作り直す
            val products =
                """[{"id":1,"name":"卵","normalizedName":"卵","createdAt":0},""" +
                    """{"id":2,"name":"玉子","normalizedName":"玉子","createdAt":0},""" +
                    """{"id":3,"name":"牛乳","normalizedName":"牛乳","createdAt":0}]"""
            val data = """{"format":"ikumemo-backup","version":1,"exportedAt":0,"products":$products}"""

            assertEquals(ImportResult.Success, importer().import(ByteArrayInputStream(data.toByteArray())))

            val names = db.backupReadDao().products().associate { it.id to it.normalizedName }
            assertEquals(mapOf(1L to "卵", 2L to "玉子", 3L to "ぎゅうにゅう"), names)
        }

    @Test
    fun wallpaperThemeRoundTrips() =
        runTest {
            // BL-176: おまかせ（壁紙の色）も、プリセットと同じ設定の項目（theme）の値として書き出し・読み込みできる
            seed()
            appearance.setTheme(ThemeChoice.Wallpaper)
            val bytes = exportBytes(ExportKind.JSON)
            wipe()

            assertEquals(ImportResult.Success, importer().import(ByteArrayInputStream(bytes)))

            assertEquals(ThemeChoice.Wallpaper, appearance.appearance.first().theme)
        }

    @Test
    fun seedThemeRoundTrips() =
        runTest {
            // BL-177: 見本の色も同じ設定の項目（theme）に "seed:#RRGGBB" で入り、書き出し・読み込みで戻る
            seed()
            appearance.setTheme(SeedSwatch.GRAPE.choice)
            val bytes = exportBytes(ExportKind.JSON)
            wipe()

            assertEquals(ImportResult.Success, importer().import(ByteArrayInputStream(bytes)))

            assertEquals(SeedSwatch.GRAPE.choice, appearance.appearance.first().theme)
        }

    @Test
    fun importReplacesTheCurrentData() =
        runTest {
            seed()
            val bytes = exportBytes(ExportKind.ZIP)
            db.backupInsertDao().insertStores(
                listOf(StoreEntity(id = 99, name = "あとで増えたお店", createdAt = Instant.EPOCH)),
            )

            importer().import(ByteArrayInputStream(bytes))

            assertEquals(listOf("近所のスーパー"), db.backupReadDao().stores().map { it.name })
        }

    @Test
    fun brokenFileIsRejectedAndLeavesDataUntouched() =
        runTest {
            seed()

            val notJson = importer().import(ByteArrayInputStream("これはJSONではない".toByteArray()))
            val other = """{"format":"other","version":1,"exportedAt":0}"""
            val wrongFormat = importer().import(ByteArrayInputStream(other.toByteArray()))

            assertTrue(notJson is ImportResult.Invalid)
            assertTrue(wrongFormat is ImportResult.Invalid)
            assertEquals(1, db.backupReadDao().records().size)
            assertEquals(0, importedCalls)
        }

    @Test
    fun brokenReferencesAreRejected() =
        runTest {
            seed()
            val item = """{"id":1,"productId":42,"displayName":"x","sortOrder":0,"createdAt":0}"""
            val data = """{"format":"ikumemo-backup","version":1,"exportedAt":0,"items":[$item]}"""

            val result = importer().import(ByteArrayInputStream(data.toByteArray()))

            assertTrue(result is ImportResult.Invalid)
            assertEquals(1, db.backupReadDao().items().size)
        }

    @Test
    fun zipEntriesOutsideThePhotoFoldersAreIgnored() =
        runTest {
            val json = """{"format":"ikumemo-backup","version":1,"exportedAt":0}"""
            val bytes =
                ByteArrayOutputStream().also { out ->
                    ZipOutputStream(out).run {
                        putNextEntry(ZipEntry("ikumemo.json"))
                        write(json.toByteArray())
                        closeEntry()
                        putNextEntry(ZipEntry("../../evil.txt"))
                        write("x".toByteArray())
                        closeEntry()
                        putNextEntry(ZipEntry("photos/../../evil2.jpg"))
                        write("x".toByteArray())
                        closeEntry()
                        finish()
                    }
                }.toByteArray()

            assertEquals(ImportResult.Success, importer().import(ByteArrayInputStream(bytes)))

            assertTrue(!File(app.filesDir, "../evil.txt").exists())
            assertTrue(File(app.filesDir, "photos").listFiles().orEmpty().isEmpty())
        }

    @Test
    fun csvListsPurchaseRecords() =
        runTest {
            seed()

            val csv = String(exportBytes(ExportKind.CSV), Charsets.UTF_8)

            assertTrue(csv.startsWith("買った日,品名,数量,お店,メモ\r\n"))
            assertTrue(csv.contains("2026-10-20,牛乳,,近所のスーパー,特売\r\n"))
        }
}
