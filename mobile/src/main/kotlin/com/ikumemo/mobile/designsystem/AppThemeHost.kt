package com.ikumemo.mobile.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
            Box {
                AppBackground(image = image, scrimLevel = value.scrimLevel, darkTheme = darkTheme)
                CompositionLocalProvider(LocalHasBackgroundImage provides true, content = content)
            }
        }
    }
}
