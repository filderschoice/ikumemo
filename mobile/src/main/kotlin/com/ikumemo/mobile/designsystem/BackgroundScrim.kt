package com.ikumemo.mobile.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import java.io.File

/**
 * 背景画像の上に敷く半透明の膜（F-61）。膜はテーマの背景色で、濃さは利用者がスライダーで選ぶ。
 * 暗い画面では写真の明るい部分で文字が読みにくくならないよう、選んだ濃さに [DARK_BOOST] を足す。
 * 明るい画面でも、暗い写真の上で赤・青のペンの文字が灰色に埋もれないよう [LIGHT_BOOST] を足す（BL-189）。
 */
object BackgroundScrim {
    const val MIN = 0.3f
    const val MAX = 0.9f
    const val DEFAULT = 0.6f
    const val DARK_BOOST = 0.15f
    const val LIGHT_BOOST = 0.1f

    /** 写真が少しは見えるよう、足しても膜は [MAX_EFFECTIVE] までにする。 */
    const val MAX_EFFECTIVE = 0.95f

    fun clamp(level: Float): Float = level.coerceIn(MIN, MAX)

    /** 実際に敷く膜の不透明度。 */
    fun effectiveAlpha(
        level: Float,
        darkTheme: Boolean,
    ): Float = (clamp(level) + if (darkTheme) DARK_BOOST else LIGHT_BOOST).coerceAtMost(MAX_EFFECTIVE)
}

/**
 * 背景画像（`filesDir` からの相対パス [image]）と膜を画面いっぱいに描く。画面端までの表示（PLAN §4.2）のため、
 * システムバーの下まで回り込ませる。読めない画像なら膜だけになり、テーマの背景色が透けて見える。
 * [scrimColor] はペンの色（[InkColor]）に合わせた膜の色で、null ならテーマの背景色。
 */
@Composable
fun AppBackground(
    image: String,
    scrimLevel: Float,
    darkTheme: Boolean,
    scrimColor: Color? = null,
) {
    val context = LocalContext.current
    val file = remember(image) { File(context.filesDir, image) }
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(colors.background)) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    (scrimColor ?: colors.background).copy(
                        alpha = BackgroundScrim.effectiveAlpha(scrimLevel, darkTheme),
                    ),
                ),
        )
    }
}
