package com.ikumemo.mobile.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.ikumemo.mobile.IkumemoApplication
import com.ikumemo.mobile.designsystem.DarkMode
import com.ikumemo.mobile.designsystem.IkumemoPalette
import kotlinx.coroutines.flow.first

/**
 * ウィジェットの色。アプリで選んだテーマ（4 種）とダークモードの設定に合わせる（PLAN §2.11）。背景画像は使わない。
 * 「端末に合わせる」のときは昼と夜の色を両方渡し、端末の設定で切り替わるようにする。
 */
class WidgetColors(
    private val day: IkumemoPalette,
    private val night: IkumemoPalette,
) {
    val primary get() = pick { it.primary }
    val onPrimary get() = pick { it.onPrimary }
    val surface get() = pick { it.surface }
    val onSurface get() = pick { it.onSurface }
    val onSurfaceVariant get() = pick { it.onSurfaceVariant }
    val secondaryContainer get() = pick { it.secondaryContainer }
    val onSecondaryContainer get() = pick { it.onSecondaryContainer }

    private fun pick(role: (IkumemoPalette) -> Long): ColorProvider =
        ColorProvider(day = Color(role(day)), night = Color(role(night)))

    companion object {
        suspend fun load(context: Context): WidgetColors {
            val container = (context.applicationContext as IkumemoApplication).container
            val appearance = container.appearancePrefs.appearance.first()
            val light = appearance.theme.palette(darkTheme = false)
            val dark = appearance.theme.palette(darkTheme = true)
            return when (appearance.darkMode) {
                DarkMode.SYSTEM -> WidgetColors(light, dark)
                DarkMode.LIGHT -> WidgetColors(light, light)
                DarkMode.DARK -> WidgetColors(dark, dark)
            }
        }
    }
}
