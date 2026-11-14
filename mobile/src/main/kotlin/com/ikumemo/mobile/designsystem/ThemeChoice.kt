package com.ikumemo.mobile.designsystem

import android.content.Context
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb

/**
 * きせかえで選んだテーマ（F-60・F-64）。プリセット 4 種（[ThemePreset]）と「おまかせ（壁紙の色）」（[Wallpaper]）のどれか。
 *
 * 設定とバックアップには [storageKey] の文字列 1 つで保存する（プリセットの値は従来のまま）。種類を足すときは、
 * ここへ実装を足して [fromStorageKey] で読めるようにし、既存の値の意味は変えない（保存済みの選択を壊さないため）。
 * 配色はテーマによっては実行時に決まるため、[palette]（Context を受け取る版）で取り出す。
 */
sealed interface ThemeChoice {
    /** 設定の保存に使う値。表示名や並び順を変えても変えない。 */
    val storageKey: String

    /** 角の丸み。 */
    val corners: CornerSet

    /**
     * おまかせ（壁紙の色、F-64）。Android 12 以降の Dynamic Color で壁紙から配色を作る。
     * それより前の端末では選べず、保存値として残っていても既定のテーマで描く。
     * 角の丸みは標準（シンプルと同じ）にする（PLAN に指定が無いため、壁紙の配色に寄せすぎない無難な値を選んだ）。
     */
    data object Wallpaper : ThemeChoice {
        override val storageKey: String = WALLPAPER_KEY
        override val corners: CornerSet get() = ThemePreset.SIMPLE.corners
    }

    companion object {
        const val WALLPAPER_KEY = "wallpaper"

        val DEFAULT: ThemeChoice get() = ThemePreset.DEFAULT

        /** 保存値から読む。未知の値（削除したテーマ、新しい版の値）は既定のテーマへ戻す。 */
        fun fromStorageKey(key: String?): ThemeChoice =
            when (key) {
                WALLPAPER_KEY -> Wallpaper
                else -> ThemePreset.fromStorageKey(key)
            }
    }
}

/** Dynamic Color（壁紙の色）が使える端末か。Android 12（API 31）以降。 */
object DynamicColor {
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S, parameter = 0)
    fun isAvailable(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= Build.VERSION_CODES.S
}

/** [choice] がこの端末で選べるか。選べないものは既定のテーマに置き換えて扱う。 */
fun ThemeChoice.availableOr(
    fallback: ThemeChoice,
    wallpaperAvailable: Boolean,
): ThemeChoice = if (this == ThemeChoice.Wallpaper && !wallpaperAvailable) fallback else this

/**
 * 実際に描く配色。アプリ本体・ウィジェット（BL-112）・写真の上のペンの色（BL-170・BL-174）は、どれもこの配色から色を取る。
 * おまかせを選べない端末では既定のテーマの配色を返す。
 */
fun ThemeChoice.palette(
    context: Context,
    darkTheme: Boolean,
): IkumemoPalette =
    when (this) {
        is ThemePreset -> palette(darkTheme)
        ThemeChoice.Wallpaper ->
            if (DynamicColor.isAvailable()) {
                wallpaperPalette(context, darkTheme)
            } else {
                ThemePreset.DEFAULT.palette(darkTheme)
            }
    }

@RequiresApi(Build.VERSION_CODES.S)
private fun wallpaperPalette(
    context: Context,
    darkTheme: Boolean,
): IkumemoPalette = (if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).toPalette()

private const val ARGB_MASK = 0xFFFFFFFFL

private fun androidx.compose.ui.graphics.Color.argb(): Long = toArgb().toLong() and ARGB_MASK

/** Material の配色（Dynamic Color など）を、いくメモの配色の形へ写す。 */
internal fun ColorScheme.toPalette(): IkumemoPalette =
    IkumemoPalette(
        primary = primary.argb(),
        onPrimary = onPrimary.argb(),
        primaryContainer = primaryContainer.argb(),
        onPrimaryContainer = onPrimaryContainer.argb(),
        secondary = secondary.argb(),
        onSecondary = onSecondary.argb(),
        secondaryContainer = secondaryContainer.argb(),
        onSecondaryContainer = onSecondaryContainer.argb(),
        tertiary = tertiary.argb(),
        onTertiary = onTertiary.argb(),
        tertiaryContainer = tertiaryContainer.argb(),
        onTertiaryContainer = onTertiaryContainer.argb(),
        background = background.argb(),
        onBackground = onBackground.argb(),
        surface = surface.argb(),
        onSurface = onSurface.argb(),
        surfaceVariant = surfaceVariant.argb(),
        onSurfaceVariant = onSurfaceVariant.argb(),
        outline = outline.argb(),
        error = error.argb(),
        onError = onError.argb(),
        errorContainer = errorContainer.argb(),
        onErrorContainer = onErrorContainer.argb(),
    )
