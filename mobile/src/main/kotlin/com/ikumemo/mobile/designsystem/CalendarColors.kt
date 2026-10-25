package com.ikumemo.mobile.designsystem

/**
 * カレンダーの色（PLAN §2.7）。日曜は赤・土曜は青の文字色で示し、曜日名も必ず併記する（色だけで伝えない、§1.3-7）。
 * お店のドットの色はお店ごとに決まった順で割り当てる（お店の色を選ぶ F-32 は v1.x）。
 *
 * どのテーマの surface・background の上でも、曜日の文字は 4.5:1、ドットは 3:1 以上になる値にする（[CalendarColorsTest]）。
 */
object CalendarColors {
    fun sunday(darkTheme: Boolean): Long = if (darkTheme) 0xFFFF8A80 else 0xFFB71C1C

    fun saturday(darkTheme: Boolean): Long = if (darkTheme) 0xFF90CAF9 else 0xFF0D47A1

    private val lightDots = listOf(0xFF2E7D32, 0xFF6A1B9A, 0xFFE65100, 0xFF00695C, 0xFF283593, 0xFFAD1457)
    private val darkDots = listOf(0xFF81C784, 0xFFCE93D8, 0xFFFFB74D, 0xFF4DB6AC, 0xFF9FA8DA, 0xFFF48FB1)

    /** お店のドットの色。お店なしは [noStore]。 */
    fun storeDot(
        storeId: Long?,
        darkTheme: Boolean,
    ): Long {
        val dots = if (darkTheme) darkDots else lightDots
        return if (storeId == null) noStore(darkTheme) else dots[(storeId % dots.size).toInt()]
    }

    fun noStore(darkTheme: Boolean): Long = if (darkTheme) 0xFFBDBDBD else 0xFF616161

    /** 検査用に全部の色を返す。 */
    internal fun allDots(darkTheme: Boolean): List<Long> = (if (darkTheme) darkDots else lightDots) + noStore(darkTheme)
}
