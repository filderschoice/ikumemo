package com.ikumemo.mobile.designsystem

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG 2.x の定義によるコントラスト比の計算。
 * 透明度は扱わない（配色はすべて不透明色で定義する）。
 */
object ContrastRatio {
    /** 文字に求めるコントラスト比の下限（PLAN §1.3-7、WCAG AA の通常文字）。 */
    const val MIN_TEXT_RATIO = 4.5

    fun of(
        foreground: Long,
        background: Long,
    ): Double {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    fun relativeLuminance(argb: Long): Double {
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun channel(value: Long): Double {
        val c = value / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
}
