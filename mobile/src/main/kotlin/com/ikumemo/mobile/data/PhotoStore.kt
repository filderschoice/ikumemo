package com.ikumemo.mobile.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 写真の保存（F-07、PLAN §4.3）。他アプリから受け取った写真はアプリ専用領域（`filesDir/photos/`）へコピーし、
 * 元の URI は保持しない（PLAN §5）。保存時に長辺 [MAX_LONG_SIDE]px へ縮小し、Exif（撮影位置など）を持たない JPEG にする。
 *
 * DB には `photos/<ファイル名>` の相対パスを保存する。
 */
class PhotoStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val photosDir get() = File(appContext.filesDir, PHOTOS_DIR).apply { mkdirs() }

    /** 写真を取り込み、保存した相対パスを返す。読めない画像なら null。 */
    suspend fun import(source: Uri): String? =
        withContext(Dispatchers.IO) {
            val resolver = appContext.contentResolver
            val orientation =
                resolver.openInputStream(source)?.use(::readOrientation) ?: ExifInterface.ORIENTATION_NORMAL
            val bounds = resolver.openInputStream(source)?.use(::readBounds) ?: return@withContext null
            val sample = PhotoSizing.sampleSize(bounds.first, bounds.second, MAX_LONG_SIDE)
            val decoded =
                resolver.openInputStream(source)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                } ?: return@withContext null
            save(PhotoSizing.scaleAndRotate(decoded, orientation, MAX_LONG_SIDE))
        }

    fun fileOf(relativePath: String): File = File(appContext.filesDir, relativePath)

    /** カメラアプリに撮ってもらう一時ファイル（`TakePicture` に渡す content URI）。取り込んだら消す。 */
    fun newCameraUri(): Uri {
        val dir = File(appContext.cacheDir, CAMERA_DIR).apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
    }

    fun deleteCameraFiles() {
        File(appContext.cacheDir, CAMERA_DIR).listFiles()?.forEach { it.delete() }
    }

    /** 参照されていない写真を消す（R-14）。消した数を返す。 */
    fun deleteUnreferenced(referenced: Set<String>): Int =
        photosDir
            .listFiles()
            .orEmpty()
            .filter { "$PHOTOS_DIR/${it.name}" !in referenced }
            .count { it.delete() }

    private fun save(bitmap: Bitmap): String {
        val name = "${UUID.randomUUID()}.jpg"
        val target = File(photosDir, name)
        // 書き込み途中で落ちても壊れた写真が残らないよう、一時ファイルに書いてから置き換える
        val temp = File(photosDir, "$name.tmp")
        temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        check(temp.renameTo(target)) { "写真を保存できませんでした" }
        return "$PHOTOS_DIR/$name"
    }

    private fun readBounds(stream: InputStream): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(stream, null, options)
        return if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
    }

    private fun readOrientation(stream: InputStream): Int =
        runCatching {
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    companion object {
        const val PHOTOS_DIR = "photos"
        const val MAX_LONG_SIDE = 1600
        private const val CAMERA_DIR = "camera"
        private const val JPEG_QUALITY = 85
    }
}

/** 写真の縮小の計算。 */
object PhotoSizing {
    /** 長辺が [maxLongSide] の 2 倍を下回らない範囲で、2 のべき乗で間引く（読み込み時のメモリを抑える）。 */
    fun sampleSize(
        width: Int,
        height: Int,
        maxLongSide: Int,
    ): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxLongSide) sample *= 2
        return sample
    }

    /** 長辺を [maxLongSide] 以下にしたときの大きさ。小さい画像は拡大しない。 */
    fun targetSize(
        width: Int,
        height: Int,
        maxLongSide: Int,
    ): Pair<Int, Int> {
        val longSide = max(width, height)
        if (longSide <= maxLongSide) return width to height
        val scale = maxLongSide.toDouble() / longSide
        return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
    }

    /** 縮小し、Exif の向きを画素へ反映する（保存する JPEG には Exif を書かないため）。 */
    fun scaleAndRotate(
        source: Bitmap,
        orientation: Int,
        maxLongSide: Int,
    ): Bitmap {
        val (width, height) = targetSize(source.width, source.height, maxLongSide)
        val matrix =
            Matrix().apply {
                postScale(width.toFloat() / source.width, height.toFloat() / source.height)
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(QUARTER_TURN)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(HALF_TURN)
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(THREE_QUARTER_TURN)
                }
            }
        val result = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (result != source) source.recycle()
        return result
    }

    private const val QUARTER_TURN = 90f
    private const val HALF_TURN = 180f
    private const val THREE_QUARTER_TURN = 270f
}
