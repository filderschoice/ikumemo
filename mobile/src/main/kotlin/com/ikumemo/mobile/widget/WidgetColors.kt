package com.ikumemo.mobile.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.ikumemo.mobile.IkumemoApplication
import com.ikumemo.mobile.data.Appearance
import com.ikumemo.mobile.designsystem.CornerSet
import com.ikumemo.mobile.designsystem.DarkMode
import com.ikumemo.mobile.designsystem.IkumemoPalette
import com.ikumemo.mobile.designsystem.ThemePreset
import com.ikumemo.mobile.designsystem.palette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * ウィジェットの色。アプリで選んだテーマ（プリセット 4 種・おまかせ）とダークモードの設定に合わせる（PLAN §2.11）。背景画像は使わない。
 * 「端末に合わせる」のときは昼と夜の色を両方渡し、端末の設定で切り替わるようにする。
 */
class WidgetColors(
    internal val day: IkumemoPalette,
    internal val night: IkumemoPalette,
    /** 本体と同じ、テーマの角の丸み。 */
    val corners: CornerSet = ThemePreset.DEFAULT.corners,
) {
    val background get() = pick { it.background }
    val onBackground get() = pick { it.onBackground }
    val primary get() = pick { it.primary }
    val onPrimary get() = pick { it.onPrimary }
    val surface get() = pick { it.surface }
    val onSurface get() = pick { it.onSurface }
    val onSurfaceVariant get() = pick { it.onSurfaceVariant }

    /** メモ帳の罫線（うすい線）。 */
    val rule get() = pick(alpha = RULE_ALPHA) { it.onSurfaceVariant }
    val secondaryContainer get() = pick { it.secondaryContainer }
    val onSecondaryContainer get() = pick { it.onSecondaryContainer }

    private fun pick(
        alpha: Float = 1f,
        role: (IkumemoPalette) -> Long,
    ): ColorProvider =
        ColorProvider(day = Color(role(day)).copy(alpha = alpha), night = Color(role(night)).copy(alpha = alpha))

    companion object {
        private const val RULE_ALPHA = 0.35f

        suspend fun load(context: Context): WidgetColors = observe(context).first()

        /** 見た目の設定が変わるたびに流す。ウィジェットの表示中に着せ替えを反映するために使う。 */
        fun observe(context: Context): Flow<WidgetColors> {
            val container = (context.applicationContext as IkumemoApplication).container
            val appContext = context.applicationContext
            return container.appearancePrefs.appearance.map { from(appContext, it) }
        }

        /** おまかせ（壁紙の色）のときも、アプリ本体と同じ [palette] から配色を取る。 */
        internal fun from(
            context: Context,
            appearance: Appearance,
        ): WidgetColors {
            val light = appearance.theme.palette(context, darkTheme = false)
            val dark = appearance.theme.palette(context, darkTheme = true)
            return when (appearance.darkMode) {
                DarkMode.SYSTEM -> WidgetColors(light, dark, appearance.theme.corners)
                DarkMode.LIGHT -> WidgetColors(light, light, appearance.theme.corners)
                DarkMode.DARK -> WidgetColors(dark, dark, appearance.theme.corners)
            }
        }
    }
}
