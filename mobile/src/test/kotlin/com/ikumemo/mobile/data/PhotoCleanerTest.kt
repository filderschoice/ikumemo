package com.ikumemo.mobile.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.data.db.ProductEntity
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.data.db.ShoppingTripEntity
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
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
import java.time.Duration
import java.time.LocalDate

/** 使われなくなった写真の掃除（R-14、BL-041 の完了条件）。 */
@RunWith(RobolectricTestRunner::class)
class PhotoCleanerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = fixedClock()
    private val photoStore = PhotoStore(context)
    private lateinit var db: IkumemoDatabase
    private lateinit var cleaner: PhotoCleaner

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        cleaner = PhotoCleaner(db, photoStore, clock)
        File(context.filesDir, PhotoStore.PHOTOS_DIR).deleteRecursively()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** 写真のファイルを置き、書いた時刻を [age] だけ前にする。 */
    private fun photo(
        name: String,
        age: Duration = Duration.ofDays(2),
    ): String {
        val path = "${PhotoStore.PHOTOS_DIR}/$name.jpg"
        photoStore.fileOf(path).apply {
            parentFile?.mkdirs()
            writeText("jpeg")
            setLastModified(clock.instant().minus(age).toEpochMilli())
        }
        return path
    }

    private fun exists(path: String) = photoStore.fileOf(path).exists()

    @Test
    fun onlyUnreferencedPhotosAreDeleted() =
        runTest {
            val itemPhoto = photo("item")
            val tripPhoto = photo("trip")
            val recordPhoto = photo("record")
            val orphan = photo("orphan")
            val productId =
                db.productDao().insert(
                    ProductEntity(name = "牛乳", normalizedName = "牛乳", createdAt = clock.instant()),
                )
            db.shoppingItemDao().insert(
                ShoppingItemEntity(
                    productId = productId,
                    displayName = "牛乳",
                    photoUri = itemPhoto,
                    sortOrder = 0,
                    createdAt = clock.instant(),
                ),
            )
            val tripId =
                db.shoppingTripDao().insert(
                    ShoppingTripEntity(date = LocalDate.of(2026, 10, 25), photoUri = tripPhoto),
                )
            db.purchaseRecordDao().insert(
                PurchaseRecordEntity(
                    tripId = tripId,
                    productId = productId,
                    displayName = "牛乳",
                    photoUri = recordPhoto,
                    purchasedDate = LocalDate.of(2026, 10, 25),
                    recordedAt = clock.instant(),
                ),
            )

            assertEquals(1, cleaner.clean())

            assertTrue(exists(itemPhoto))
            assertTrue(exists(tripPhoto))
            assertTrue(exists(recordPhoto))
            assertFalse(exists(orphan))
        }

    @Test
    fun photoSharedByMemoAndRecordIsKeptWhileEitherUsesIt() =
        runTest {
            // 「買った」にしたメモの写真は記録が同じファイルを参照する（R-15）
            val shared = photo("shared")
            val productId =
                db.productDao().insert(
                    ProductEntity(name = "卵", normalizedName = "卵", createdAt = clock.instant()),
                )
            val tripId = db.shoppingTripDao().insert(ShoppingTripEntity(date = LocalDate.of(2026, 10, 25)))
            db.purchaseRecordDao().insert(
                PurchaseRecordEntity(
                    tripId = tripId,
                    productId = productId,
                    displayName = "卵",
                    photoUri = shared,
                    purchasedDate = LocalDate.of(2026, 10, 25),
                    recordedAt = clock.instant(),
                ),
            )

            assertEquals(0, cleaner.clean())
            assertTrue(exists(shared))
        }

    @Test
    fun recentlyImportedPhotoIsKeptEvenIfNotSavedYet() =
        runTest {
            // 編集シートで取り込んだばかりで、まだ閉じて保存していない写真
            val fresh = photo("fresh", age = Duration.ofHours(1))

            assertEquals(0, cleaner.clean())
            assertTrue(exists(fresh))
        }
}
