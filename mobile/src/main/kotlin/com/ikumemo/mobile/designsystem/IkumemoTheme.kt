package com.ikumemo.mobile.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * いくメモのテーマ。プリセット [preset] の配色と角の丸みを適用し、文字の大きさ [textSize] を端末設定に
 * 掛け合わせる（上限 200%）。フォントは端末標準を使い、同梱しない（PLAN §2.7）。
 */
@Composable
fun IkumemoTheme(
    preset: ThemePreset = ThemePreset.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    textSize: TextSizeSetting = TextSizeSetting.NORMAL,
    content: @Composable () -> Unit,
) {
    IkumemoTheme(
        palette = preset.palette(darkTheme),
        corners = preset.corners,
        darkTheme = darkTheme,
        textSize = textSize,
        content = content,
    )
}

/**
 * きせかえで選んだテーマ [choice]（プリセットまたはおまかせ）で描く。おまかせの配色は端末の壁紙から作るため、
 * [ThemeChoice.palette] で Context から取り出す。
 */
@Composable
fun IkumemoTheme(
    choice: ThemeChoice,
    darkTheme: Boolean = isSystemInDarkTheme(),
    textSize: TextSizeSetting = TextSizeSetting.NORMAL,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val palette = remember(choice, darkTheme, context) { choice.palette(context, darkTheme) }
    IkumemoTheme(
        palette = palette,
        corners = choice.corners,
        darkTheme = darkTheme,
        textSize = textSize,
        content = content,
    )
}

@Composable
private fun IkumemoTheme(
    palette: IkumemoPalette,
    corners: CornerSet,
    darkTheme: Boolean,
    textSize: TextSizeSetting,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scaledDensity =
        Density(
            density = density.density,
            fontScale = TextSizeSetting.effectiveFontScale(density.fontScale, textSize),
        )
    CompositionLocalProvider(LocalDensity provides scaledDensity, LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(darkTheme),
            shapes = corners.toShapes(),
            content = content,
        )
    }
}

/**
 * いま暗い画面で描いているか（きせかえの「暗い画面」を反映した値）。テーマの外の色（カレンダーの曜日の色など）を選ぶときは、
 * 端末の設定（`isSystemInDarkTheme`）ではなくこれを見る。
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

internal fun IkumemoPalette.toColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        outline = Color(outline),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
    )
}

internal fun CornerSet.toShapes(): Shapes =
    Shapes(
        small = RoundedCornerShape(small.dp),
        medium = RoundedCornerShape(medium.dp),
        large = RoundedCornerShape(large.dp),
    )
