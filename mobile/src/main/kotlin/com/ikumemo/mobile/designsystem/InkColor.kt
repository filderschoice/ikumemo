package com.ikumemo.mobile.designsystem

import androidx.compose.ui.graphics.Color

/**
 * 背景の写真の上に直接置く文字の色（ペンの色、F-61）。ボールペンのように黒・青・赤・白から選ぶ。
 * 色に合わせて写真の上の膜の色も変え（濃い文字は明るい膜、白は暗い膜）、どの組み合わせでも読めるようにする。
 * [AUTO] はテーマの色のまま。背景の写真が無いときは効かない（テーマの背景と文字の組み合わせはテーマが保証する）。
 */
enum class InkColor(
    val storageKey: String,
    private val ink: Long?,
    private val scrim: Long?,
) {
    AUTO("auto", null, null),
    BLACK("black", 0xFF1B1B1F, LIGHT_SCRIM),
    BLUE("blue", 0xFF1A3FA0, LIGHT_SCRIM),
    RED("red", 0xFFB3261E, LIGHT_SCRIM),
    WHITE("white", 0xFFFFFFFF, DARK_SCRIM),
    ;

    /** 文字の色。[AUTO] は null。 */
    val textColor: Color? get() = ink?.let(::Color)

    /** 膜の色。[AUTO] は null（テーマの背景色を使う）。 */
    val scrimColor: Color? get() = scrim?.let(::Color)

    companion object {
        fun fromStorageKey(key: String?): InkColor = entries.firstOrNull { it.storageKey == key } ?: AUTO
    }
}

private const val LIGHT_SCRIM = 0xFFFFFBFF
private const val DARK_SCRIM = 0xFF1C1B1F
