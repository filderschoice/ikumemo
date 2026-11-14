package com.ikumemo.mobile.designsystem

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 背景の写真の上でペンの色（[InkColor]）が選ばれているときのその文字色。写真が無い・おまかせのときは null。
 * 写真（膜）の上に直接置く文字は、テーマの固定色（onSurfaceVariant・primary 等）ではなくこの色にする（BL-174）。
 */
val LocalPhotoInk = staticCompositionLocalOf<Color?> { null }

/** 写真の上でペンの色があるときだけそれを返し、無ければ [themeColor]（今の見た目）を返す。 */
fun resolvePhotoInk(
    ink: Color?,
    themeColor: Color,
): Color = ink ?: themeColor

/** 面に載らず写真の上に直接出る文字の色。写真が無いときは [themeColor] のまま。 */
@Composable
fun photoInk(themeColor: Color): Color = resolvePhotoInk(LocalPhotoInk.current, themeColor)

/** 写真の上の未選択のチップの文字色をペンの色にする。写真が無いときは既定のまま。 */
@Composable
fun photoFilterChipColors(): SelectableChipColors {
    val ink = LocalPhotoInk.current ?: return FilterChipDefaults.filterChipColors()
    return FilterChipDefaults.filterChipColors(labelColor = ink)
}

/** 写真の上の文字ボタン（写真を選ぶ・外す）の文字色をペンの色にする。写真が無いときは既定のまま。 */
@Composable
fun photoOutlinedButtonColors(): ButtonColors {
    val ink = LocalPhotoInk.current ?: return ButtonDefaults.outlinedButtonColors()
    return ButtonDefaults.outlinedButtonColors(contentColor = ink)
}

@Composable
fun photoTextButtonColors(): ButtonColors {
    val ink = LocalPhotoInk.current ?: return ButtonDefaults.textButtonColors()
    return ButtonDefaults.textButtonColors(contentColor = ink)
}
