package com.ikumemo.mobile.shoppinglist

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

/** スワイプで確定する距離（行の幅の 40%、R-05）。 */
internal const val SWIPE_THRESHOLD_FRACTION = 0.4f

/**
 * 横スワイプの確定を守る小さな記録（BL-193）。material3 1.3.1 の SwipeToDismissBox は、しきい値に届かなくても
 * 素早く払う速度（fling）で確定してしまい、速度のしきい値は変えられない。そこで確定の直前に、位置と縦横の動きをここで確かめる。
 * 値の読み書きは UI スレッドだけなので、状態（State）にはしない。
 */
internal class SwipeGuard {
    private var totalDx = 0f
    private var totalDy = 0f
    private var fired = false

    /** 行の幅（px）。レイアウトのたびに更新する。 */
    var widthPx = 0f

    /** 指を置いてから今までの動きの合計を、触れたまま観察して貯める（イベントは消費しない）。 */
    fun observe(): Modifier =
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                totalDx = 0f
                totalDy = 0f
                fired = false
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.firstOrNull()?.let {
                        val move = it.positionChange()
                        totalDx += move.x
                        totalDy += move.y
                    }
                } while (event.changes.any { it.pressed })
            }
        }

    /** 1 回の指の動きで、確定を 1 度だけ通す（SwipeToDismissBox は確定の確認を続けて 2 回呼ぶことがある）。 */
    fun firstConfirmation(): Boolean = !fired.also { fired = true }

    /** 幅の 40% 以上引いていて、縦より横の動きが大きいときだけ確定してよい。 */
    fun allows(offsetPx: Float): Boolean = shouldConfirmSwipe(offsetPx, widthPx, totalDx, totalDy)
}

/** [offsetPx] は行のずれ（符号付き）、[dx]・[dy] は指の動きの合計。速度だけでは確定しない（BL-193）。 */
internal fun shouldConfirmSwipe(
    offsetPx: Float,
    widthPx: Float,
    dx: Float,
    dy: Float,
): Boolean = widthPx > 0f && abs(offsetPx) >= widthPx * SWIPE_THRESHOLD_FRACTION && abs(dy) <= abs(dx)
