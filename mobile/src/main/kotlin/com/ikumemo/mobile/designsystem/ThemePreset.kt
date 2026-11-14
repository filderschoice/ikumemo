package com.ikumemo.mobile.designsystem

import androidx.annotation.StringRes
import com.ikumemo.mobile.R

/** 角の丸みの大きさ（dp）。テーマの個性は色・角の丸み・余白で出す（PLAN §2.7 デザインの共通ルール）。 */
data class CornerSet(
    val small: Int,
    val medium: Int,
    val large: Int,
)

/**
 * プリセットテーマ 4 種（PLAN D-8・§2.7）。各テーマはライト／ダークの両方を持つ。
 * 「おまかせ」（Dynamic Color、F-64）は v1.x のため含めない。
 *
 * [storageKey] は設定の保存に使う値で、表示名や並び順を変えても保存済みの選択が壊れないよう固定する。
 */
enum class ThemePreset(
    val storageKey: String,
    @StringRes val labelRes: Int,
    val light: IkumemoPalette,
    val dark: IkumemoPalette,
    val corners: CornerSet,
) {
    /** 明るく元気。コーラルオレンジ × アンバー × ミント。いちばん大きい角丸。 */
    POP(
        storageKey = "pop",
        labelRes = R.string.theme_pop,
        light =
            IkumemoPalette(
                primary = 0xFFB5381C,
                onPrimary = 0xFFFFFFFF,
                primaryContainer = 0xFFFFDBD0,
                onPrimaryContainer = 0xFF3A0A00,
                secondary = 0xFF8A5100,
                onSecondary = 0xFFFFFFFF,
                secondaryContainer = 0xFFFFDDB8,
                onSecondaryContainer = 0xFF2C1600,
                tertiary = 0xFF006B5A,
                onTertiary = 0xFFFFFFFF,
                tertiaryContainer = 0xFFB8F5E4,
                onTertiaryContainer = 0xFF00201A,
                background = 0xFFFFF4EC,
                onBackground = 0xFF261813,
                surface = 0xFFFFF4EC,
                onSurface = 0xFF261813,
                surfaceVariant = 0xFFF7DDD0,
                onSurfaceVariant = 0xFF55433B,
                outline = 0xFF88726A,
            ),
        dark =
            IkumemoPalette(
                primary = 0xFFFFB4A0,
                onPrimary = 0xFF5F1500,
                primaryContainer = 0xFF862200,
                onPrimaryContainer = 0xFFFFDBD0,
                secondary = 0xFFFFB86C,
                onSecondary = 0xFF4A2800,
                secondaryContainer = 0xFF6A3B00,
                onSecondaryContainer = 0xFFFFDDB8,
                tertiary = 0xFF9CD8C8,
                onTertiary = 0xFF00382E,
                tertiaryContainer = 0xFF005144,
                onTertiaryContainer = 0xFFB8F5E4,
                background = 0xFF1F1410,
                onBackground = 0xFFF2DFD7,
                surface = 0xFF1F1410,
                onSurface = 0xFFF2DFD7,
                surfaceVariant = 0xFF55433B,
                onSurfaceVariant = 0xFFDAC2B8,
                outline = 0xFFA28C83,
            ).withDarkError(),
        corners = CornerSet(small = 16, medium = 28, large = 36),
    ),

    /** ふんわり・ナチュラル。セージグリーン × ベージュ × くすみピンク。大きめの角丸。 */
    GENTLE(
        storageKey = "gentle",
        labelRes = R.string.theme_gentle,
        light =
            IkumemoPalette(
                primary = 0xFF4A6B4F,
                onPrimary = 0xFFFFFFFF,
                primaryContainer = 0xFFCBEACD,
                onPrimaryContainer = 0xFF082010,
                secondary = 0xFF6E5B40,
                onSecondary = 0xFFFFFFFF,
                secondaryContainer = 0xFFF2E1C6,
                onSecondaryContainer = 0xFF261904,
                tertiary = 0xFF8E4A5C,
                onTertiary = 0xFFFFFFFF,
                tertiaryContainer = 0xFFF6D9DF,
                onTertiaryContainer = 0xFF3A0B1A,
                background = 0xFFEEF5EB,
                onBackground = 0xFF181D18,
                surface = 0xFFEEF5EB,
                onSurface = 0xFF181D18,
                surfaceVariant = 0xFFDDE6DA,
                onSurfaceVariant = 0xFF424941,
                outline = 0xFF727971,
            ),
        dark =
            IkumemoPalette(
                primary = 0xFFAFCFB2,
                onPrimary = 0xFF1B3621,
                primaryContainer = 0xFF324D37,
                onPrimaryContainer = 0xFFCBEACD,
                secondary = 0xFFDCC3A1,
                onSecondary = 0xFF3D2E16,
                secondaryContainer = 0xFF55442B,
                onSecondaryContainer = 0xFFF9E0BC,
                tertiary = 0xFFF2B5C4,
                onTertiary = 0xFF551D2E,
                tertiaryContainer = 0xFF723345,
                onTertiaryContainer = 0xFFFFD9E1,
                background = 0xFF131A14,
                onBackground = 0xFFE0E4DC,
                surface = 0xFF131A14,
                onSurface = 0xFFE0E4DC,
                surfaceVariant = 0xFF424941,
                onSurfaceVariant = 0xFFC2C9BF,
                outline = 0xFF8C938A,
            ).withDarkError(),
        corners = CornerSet(small = 12, medium = 20, large = 28),
    ),

    /** すっきり・都会的。ネイビー × アイスブルー × グレー。小さめの角丸。 */
    COOL(
        storageKey = "cool",
        labelRes = R.string.theme_cool,
        light =
            IkumemoPalette(
                primary = 0xFF1F3A68,
                onPrimary = 0xFFFFFFFF,
                primaryContainer = 0xFFD6E3FF,
                onPrimaryContainer = 0xFF001B3E,
                secondary = 0xFF4A6072,
                onSecondary = 0xFFFFFFFF,
                secondaryContainer = 0xFFCDE5F7,
                onSecondaryContainer = 0xFF041E2C,
                tertiary = 0xFF5C5F66,
                onTertiary = 0xFFFFFFFF,
                tertiaryContainer = 0xFFE0E2EA,
                onTertiaryContainer = 0xFF191C22,
                background = 0xFFEDF2FD,
                onBackground = 0xFF181C24,
                surface = 0xFFEDF2FD,
                onSurface = 0xFF181C24,
                surfaceVariant = 0xFFD9E3F3,
                onSurfaceVariant = 0xFF424750,
                outline = 0xFF707887,
            ),
        dark =
            IkumemoPalette(
                primary = 0xFFA9C7FF,
                onPrimary = 0xFF003063,
                primaryContainer = 0xFF1F3A68,
                onPrimaryContainer = 0xFFD6E3FF,
                secondary = 0xFFB1C9DB,
                onSecondary = 0xFF1C3342,
                secondaryContainer = 0xFF334A5A,
                onSecondaryContainer = 0xFFCDE5F7,
                tertiary = 0xFFC4C6CE,
                onTertiary = 0xFF2E3037,
                tertiaryContainer = 0xFF44474E,
                onTertiaryContainer = 0xFFE0E2EA,
                background = 0xFF0E1522,
                onBackground = 0xFFE0E6F2,
                surface = 0xFF0E1522,
                onSurface = 0xFFE0E6F2,
                surfaceVariant = 0xFF404859,
                onSurfaceVariant = 0xFFC2C8D6,
                outline = 0xFF8A91A0,
            ).withDarkError(),
        corners = CornerSet(small = 4, medium = 8, large = 12),
    ),

    /** 白基調・情報重視。白 × チャコール × アクセント 1 色（青）。標準の角丸。 */
    SIMPLE(
        storageKey = "simple",
        labelRes = R.string.theme_simple,
        light =
            IkumemoPalette(
                primary = 0xFF2F3133,
                onPrimary = 0xFFFFFFFF,
                primaryContainer = 0xFFE3E3E3,
                onPrimaryContainer = 0xFF1A1C1E,
                secondary = 0xFF00658F,
                onSecondary = 0xFFFFFFFF,
                secondaryContainer = 0xFFC7E7FF,
                onSecondaryContainer = 0xFF001E2E,
                tertiary = 0xFF5D5E61,
                onTertiary = 0xFFFFFFFF,
                tertiaryContainer = 0xFFE2E2E6,
                onTertiaryContainer = 0xFF1A1B1F,
                background = 0xFFFFFFFF,
                onBackground = 0xFF1A1C1E,
                surface = 0xFFFFFFFF,
                onSurface = 0xFF1A1C1E,
                surfaceVariant = 0xFFF0F0F3,
                onSurfaceVariant = 0xFF44474A,
                outline = 0xFF75777A,
            ),
        dark =
            IkumemoPalette(
                primary = 0xFFE3E3E3,
                onPrimary = 0xFF2F3133,
                primaryContainer = 0xFF45474A,
                onPrimaryContainer = 0xFFE3E3E3,
                secondary = 0xFF85CFFF,
                onSecondary = 0xFF00344C,
                secondaryContainer = 0xFF004C6C,
                onSecondaryContainer = 0xFFC7E7FF,
                tertiary = 0xFFC6C6CA,
                onTertiary = 0xFF2F3033,
                tertiaryContainer = 0xFF45474A,
                onTertiaryContainer = 0xFFE2E2E6,
                background = 0xFF121315,
                onBackground = 0xFFE3E2E6,
                surface = 0xFF121315,
                onSurface = 0xFFE3E2E6,
                surfaceVariant = 0xFF44474A,
                onSurfaceVariant = 0xFFC5C6CA,
                outline = 0xFF8F9194,
            ).withDarkError(),
        corners = CornerSet(small = 8, medium = 12, large = 16),
    ),
    ;

    fun palette(darkTheme: Boolean): IkumemoPalette = if (darkTheme) dark else light

    companion object {
        /**
         * 初期テーマ。PLAN に既定値の指定が無いため、D-8 の並びの先頭（ポップ）とする。
         * きせかえ画面（F-60、BL-060）でいつでも変えられ、安全性に影響しない。
         */
        val DEFAULT = POP

        fun fromStorageKey(key: String?): ThemePreset = entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}
