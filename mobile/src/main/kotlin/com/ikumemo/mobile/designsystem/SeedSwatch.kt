package com.ikumemo.mobile.designsystem

import androidx.annotation.StringRes
import com.ikumemo.mobile.R

/**
 * 着せ替えの「見本の色から選ぶ」に並べる色（BL-177）。選んだ色を種に [SeedPalette] で配色を作る。
 *
 * 自由なカラーピッカーにしないのは、読めない組み合わせを生みやすいため（オーナー判断、2026-11-14）。
 * 色相が重ならないよう 10 色を選び、読み上げ用に身近なものの名前を付ける。保存値は色そのもの（`seed:#RRGGBB`）なので、
 * 見本の並びや名前を変えても保存済みの選択は壊れない。色を変えると、その色を選んでいた人の配色も変わる点に注意する。
 */
enum class SeedSwatch(
    val argb: Long,
    @StringRes val labelRes: Int,
) {
    STRAWBERRY(0xFFE53950, R.string.seed_strawberry),
    SAKURA(0xFFE86A9A, R.string.seed_sakura),
    ORANGE(0xFFF28C28, R.string.seed_orange),
    SUNFLOWER(0xFFE8B828, R.string.seed_sunflower),
    LEAF(0xFF7CB342, R.string.seed_leaf),
    FOREST(0xFF2E9D5B, R.string.seed_forest),
    SKY(0xFF3A9AD9, R.string.seed_sky),
    SEA(0xFF2F5FC4, R.string.seed_sea),
    GRAPE(0xFF7E57C2, R.string.seed_grape),
    COCOA(0xFF8D6E63, R.string.seed_cocoa),
    ;

    val choice: ThemeChoice.Seed get() = ThemeChoice.seed(argb)

    companion object {
        /** [choice] に当たる見本。見本に無い色（バックアップから読んだ色など）は null。 */
        fun of(choice: ThemeChoice.Seed): SeedSwatch? = entries.firstOrNull { it.choice == choice }
    }
}
