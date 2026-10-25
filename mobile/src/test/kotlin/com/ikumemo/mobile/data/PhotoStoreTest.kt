package com.ikumemo.mobile.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 写真の縮小と Exif の除去（BL-040 の完了条件）。実際に JPEG を読み書きするためネイティブ描画で動かす。 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = PhotoStore(context)

    /** 位置情報入りの JPEG を作る。 */
    private fun jpegWithLocation(
        width: Int,
        height: Int,
        orientation: Int = ExifInterface.ORIENTATION_NORMAL,
    ): Uri {
        val file = File(context.cacheDir, "source_${width}x$height.jpg")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "35/1,40/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "139/1,45/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
        assertNotNull(ExifInterface(file.absolutePath).getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        return Uri.fromFile(file)
    }

    private fun sizeOf(file: File): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth to options.outHeight
    }

    @Test
    fun largePhotoIsShrunkTo1600AndLocationIsRemoved() =
        runTest {
            val path = requireNotNull(store.import(jpegWithLocation(3200, 2400)))

            val saved = store.fileOf(path)
            assertTrue(path.startsWith("photos/"))
            assertEquals(1600 to 1200, sizeOf(saved))
            val exif = ExifInterface(saved.absolutePath)
            assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
            assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
        }

    @Test
    fun smallPhotoIsNotEnlarged() =
        runTest {
            val path = requireNotNull(store.import(jpegWithLocation(800, 600)))

            assertEquals(800 to 600, sizeOf(store.fileOf(path)))
        }

    @Test
    fun orientationIsAppliedToPixels() =
        runTest {
            val path = requireNotNull(store.import(jpegWithLocation(400, 200, ExifInterface.ORIENTATION_ROTATE_90)))

            assertEquals(200 to 400, sizeOf(store.fileOf(path)))
        }

    @Test
    fun unreadableFileIsIgnored() =
        runTest {
            val file = File(context.cacheDir, "broken.jpg").apply { writeText("not an image") }

            assertNull(store.import(Uri.fromFile(file)))
        }

    @Test
    fun unreferencedPhotosAreDeleted() =
        runTest {
            val kept = requireNotNull(store.import(jpegWithLocation(100, 100)))
            val removed = requireNotNull(store.import(jpegWithLocation(120, 120)))

            assertEquals(1, store.deleteUnreferenced(setOf(kept)))

            assertTrue(store.fileOf(kept).exists())
            assertTrue(!store.fileOf(removed).exists())
        }
}
