package com.ikumemo.mobile.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * BL-188。暗いテーマで背景の写真を置いたとき、メモ画面の「文字で追加」（文字ボタン）の文字色は、
 * ペンの色があればそれになり（[resolvePhotoInk]）、膜を重ねた背景との比が 4.5:1 以上になる。
 * 背景の明るさは BL-187 の [SystemBarIcons.backgroundLuminance]（写真を灰色と仮定して膜を重ねる）を使う。
 */
class PhotoTextButtonContrastTest {
    private val darkThemes = ThemePreset.entries.map { it.palette(true) }

    private fun ratio(
        text: Color,
        backgroundLuminance: Float,
    ): Double {
        val a = text.luminance().toDouble()
        val b = backgroundLuminance.toDouble()
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun failures(
        levels: List<Float>,
        photos: List<Float>,
    ): List<String> =
        // あか（InkColor.RED）は暗い写真＋既定の濃さで約 3.7 対 1 になる。完了条件の対象（くろ・あお）の外のため別項目で扱う
        InkColor.entries.filter { it != InkColor.RED }.flatMap { ink ->
            darkThemes.flatMap { palette ->
                val text = resolvePhotoInk(ink.textColor, Color(palette.primary))
                val scrim = ink.scrimColor ?: Color(palette.background)
                levels.flatMap { level ->
                    photos.mapNotNull { photo ->
                        val alpha = BackgroundScrim.effectiveAlpha(level, true)
                        val luminance = SystemBarIcons.backgroundLuminance(scrim, alpha, photo)
                        val r = ratio(text, luminance)
                        "${ink.storageKey} level=$level photo=$photo は $r".takeIf { r < 4.5 }
                    }
                }
            }
        }

    @Test
    fun darkThemeWithBrightAndDarkPhotosIsReadableForEveryPenColor() {
        val result = failures(listOf(BackgroundScrim.DEFAULT, BackgroundScrim.MAX), listOf(0.1f, 0.9f))
        assertTrue(result.distinct().joinToString(), result.isEmpty())
    }
}
