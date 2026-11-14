package com.ikumemo.mobile.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ikumemo.mobile.data.Appearance
import kotlinx.coroutines.flow.Flow

/** 背景画像を描いているか（F-61）。true なら画面の枠（Scaffold）は背景色を塗らずに透かす。 */
val LocalHasBackgroundImage = staticCompositionLocalOf { false }

/**
 * 保存した見た目（きせかえ）で [content] を描く。保存値を読み終えるまでは描かない
 * （起動直後に既定のテーマが一瞬見えるのを防ぐ）。アプリ本体と音声入力シートで共用する。
 * [showBackground] が true なら背景画像（F-61）も描く（他のアプリの上に出す音声入力シートでは描かない）。
 */
@Composable
fun AppThemeHost(
    appearance: Flow<Appearance>,
    showBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    val current by appearance.collectAsStateWithLifecycle(initialValue = null)
    val value = current ?: return
    val darkTheme = value.darkMode.isDark(isSystemInDarkTheme())
    IkumemoTheme(
        preset = value.theme,
        darkTheme = darkTheme,
        textSize = value.textSize,
    ) {
        val image = value.backgroundImage?.takeIf { showBackground }
        if (image == null) {
            content()
        } else {
            val ink = value.ink.textColor
            Box {
                AppBackground(
                    image = image,
                    scrimLevel = value.scrimLevel,
                    darkTheme = darkTheme,
                    scrimColor = value.ink.scrimColor,
                )
                // ペンの色は、写真の上に直接置く文字（LocalContentColor）だけに効かせる（BL-170）。
                // テーマの surface と background は同じ色のため、onBackground を差し替えると
                // Surface の既定の文字色（contentColorFor）が先に background に当たり、面の中の文字までペンの色になる。
                // 面の中の文字は ReadablePanel などが onSurface を明示して戻す
                CompositionLocalProvider(
                    LocalHasBackgroundImage provides true,
                    LocalContentColor provides (ink ?: LocalContentColor.current),
                    content = content,
                )
            }
        }
    }
}
