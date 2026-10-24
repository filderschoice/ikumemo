package com.ikumemo.mobile.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ikumemo.mobile.data.Appearance
import kotlinx.coroutines.flow.Flow

/**
 * 保存した見た目（きせかえ）で [content] を描く。保存値を読み終えるまでは描かない
 * （起動直後に既定のテーマが一瞬見えるのを防ぐ）。アプリ本体と音声入力シートで共用する。
 */
@Composable
fun AppThemeHost(
    appearance: Flow<Appearance>,
    content: @Composable () -> Unit,
) {
    val current by appearance.collectAsStateWithLifecycle(initialValue = null)
    val value = current ?: return
    IkumemoTheme(
        preset = value.theme,
        darkTheme = value.darkMode.isDark(isSystemInDarkTheme()),
        textSize = value.textSize,
        content = content,
    )
}
