package com.ikumemo.mobile.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import com.ikumemo.mobile.testing.testDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** BL-103 の完了条件「DB・写真・背景画像・設定がすべて消える」を確かめる。 */
@RunWith(RobolectricTestRunner::class)
class DataEraserTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: IkumemoDatabase
    private val dataStore = testDataStore()
    private val photos by lazy { PhotoStore(app) }
    private val background by lazy { PhotoStore(app, PhotoStore.BACKGROUND_DIR) }
    private var erasedCalls = 0

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

    @Test
    fun erasesDatabaseSettingsAndPhotos() =
        runTest {
            val shopping = ShoppingRepository(db, ProductNameNormalizer(), fixedClock())
            shopping.addItem("牛乳")
            shopping.addItem("たまご")
            GuidePrefs(dataStore).finishOnboarding()
            File(app.filesDir, "${PhotoStore.PHOTOS_DIR}/a.jpg").apply { parentFile?.mkdirs() }.writeText("x")
            File(app.filesDir, "${PhotoStore.BACKGROUND_DIR}/b.jpg").apply { parentFile?.mkdirs() }.writeText("x")
            assertEquals(2, db.shoppingItemDao().getAll().size)

            DataEraser(db, dataStore, listOf(photos, background)) { erasedCalls++ }.eraseAll()

            assertTrue(db.shoppingItemDao().getAll().isEmpty())
            assertTrue(db.productDao().getAllNames().isEmpty())
            assertFalse(GuidePrefs(dataStore).state.first().onboardingDone)
            assertTrue(File(app.filesDir, PhotoStore.PHOTOS_DIR).listFiles().orEmpty().isEmpty())
            assertTrue(File(app.filesDir, PhotoStore.BACKGROUND_DIR).listFiles().orEmpty().isEmpty())
            assertEquals(1, erasedCalls)
        }

    @Test
    fun worksWhenNothingIsSaved() =
        runTest {
            DataEraser(db, dataStore, listOf(photos, background)) { erasedCalls++ }.eraseAll()

            assertEquals(1, erasedCalls)
        }
}
