package com.ikumemo.mobile.designsystem

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * BL-189。暗い写真の上に直接置いた文字（ペンの色）と、膜を重ねた背景の比が、
 * 明るい・暗いテーマの両方で 4.5:1 以上になる。明るい写真でも悪化していない。
 * 背景の明るさは写真を灰色と仮定して膜を重ねて求める（[SystemBarIcons.backgroundLuminance]）。
 */
class InkOnPhotoContrastTest {
    private val darkPhoto = 0.1f
    private val brightPhoto = 0.9f

    private fun ratio(
        ink: InkColor,
        dark: Boolean,
        level: Float,
        photo: Float,
    ): Double {
        val text = ink.textColor!!.luminance().toDouble()
        val alpha = BackgroundScrim.effectiveAlpha(level, dark)
        val bg = SystemBarIcons.backgroundLuminance(ink.scrimColor!!, alpha, photo).toDouble()
        return (max(text, bg) + 0.05) / (min(text, bg) + 0.05)
    }

    @Test
    fun everyPenColorIsReadableOnDarkAndBrightPhotos() {
        val failures =
            InkColor.entries.filter { it != InkColor.AUTO }.flatMap { ink ->
                listOf(false, true).flatMap { dark ->
                    listOf(BackgroundScrim.DEFAULT, BackgroundScrim.MAX).flatMap { level ->
                        listOf(darkPhoto, brightPhoto).mapNotNull { photo ->
                            val r = ratio(ink, dark, level, photo)
                            "${ink.storageKey} dark=$dark level=$level photo=$photo は $r".takeIf {
                                // しろ（暗い膜）は明るい写真だと膜が薄く読みにくくなり得るが、従来から対象外（BL-108）
                                r < ContrastRatio.MIN_TEXT_RATIO && !(ink == InkColor.WHITE && photo == brightPhoto)
                            }
                        }
                    }
                }
            }
        assertTrue(failures.joinToString(), failures.isEmpty())
    }
}
