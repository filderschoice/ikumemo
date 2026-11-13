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
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 写真の保存（F-07、PLAN §4.3）。他アプリから受け取った写真はアプリ専用領域（`filesDir/photos/`）へコピーし、
 * 元の URI は保持しない（PLAN §5）。保存時に長辺 [MAX_LONG_SIDE]px へ縮小し、Exif（撮影位置など）を持たない JPEG にする。
 *
 * DB には `photos/<ファイル名>` の相対パスを保存する。背景画像（F-61）は [dirName] に [BACKGROUND_DIR] を渡して
 * `filesDir/background/` へ同じ形で保存する（写真と分けて、自動バックアップの対象に残すため。F-71）。
 */
class PhotoStore(
    context: Context,
    private val dirName: String = PHOTOS_DIR,
) {
    private val appContext = context.applicationContext
    private val photosDir get() = File(appContext.filesDir, dirName).apply { mkdirs() }

    /**
     * 写真を取り込み、保存した相対パスを返す。読めない画像なら null。
     * 元の写真が消えた・権限が切れた・保存に失敗したときも、アプリを落とさず null を返す（BL-113）。
     */
    suspend fun import(source: Uri): String? =
        withContext(Dispatchers.IO) {
            val resolver = appContext.contentResolver
            try {
                val orientation =
                    resolver.openInputStream(source)?.use(::readOrientation) ?: ExifInterface.ORIENTATION_NORMAL
                val sample =
                    resolver.openInputStream(source)?.use(::readBounds)?.let { (width, height) ->
                        PhotoSizing.sampleSize(width, height, MAX_LONG_SIDE)
                    }
                val decoded =
                    sample?.let { size ->
                        val options = BitmapFactory.Options().apply { inSampleSize = size }
                        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
                    }
                decoded?.let { save(PhotoSizing.scaleAndRotate(it, orientation, MAX_LONG_SIDE)) }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalStateException) {
                null
            }
        }

    fun fileOf(relativePath: String): File = File(appContext.filesDir, relativePath)

    /** 保存した写真を 1 枚消す。このストアのフォルダの外は消さない。 */
    fun delete(relativePath: String) {
        if (relativePath.startsWith("$dirName/")) fileOf(relativePath).delete()
    }

    /** カメラアプリに撮ってもらう一時ファイル（`TakePicture` に渡す content URI）。取り込んだら消す。 */
    fun newCameraUri(): Uri {
        val dir = File(appContext.cacheDir, CAMERA_DIR).apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
    }

    /** このフォルダの写真をすべて消す（データをすべて消す、F-76）。 */
    fun deleteAll() {
        photosDir.listFiles()?.forEach { it.delete() }
    }

    fun deleteCameraFiles() {
        File(appContext.cacheDir, CAMERA_DIR).listFiles()?.forEach { it.delete() }
    }

    /**
     * 参照されていない写真を消す（R-14）。消した数を返す。[modifiedBeforeMillis] より後に書いた写真は、
     * 編集シートで取り込んだばかりでまだ保存されていない可能性があるため残す。
     */
    fun deleteUnreferenced(
        referenced: Set<String>,
        modifiedBeforeMillis: Long = Long.MAX_VALUE,
    ): Int =
        photosDir
            .listFiles()
            .orEmpty()
            .filter { "$dirName/${it.name}" !in referenced && it.lastModified() < modifiedBeforeMillis }
            .count { it.delete() }

    private fun save(bitmap: Bitmap): String {
        val name = "${UUID.randomUUID()}.jpg"
        val target = File(photosDir, name)
        // 書き込み途中で落ちても壊れた写真が残らないよう、一時ファイルに書いてから置き換える
        val temp = File(photosDir, "$name.tmp")
        temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        check(temp.renameTo(target)) { "写真を保存できませんでした" }
        return "$dirName/$name"
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
        const val BACKGROUND_DIR = "background"
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
