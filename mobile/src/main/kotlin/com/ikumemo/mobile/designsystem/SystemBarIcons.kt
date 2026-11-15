package com.ikumemo.mobile.designsystem

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 状態バー・ナビゲーションバーのアイコン（時刻・電波・電池など）を暗い色にするかの判定（BL-187）。
 * 背景の写真の上の膜は、ペンの色と暗さの設定で明るくも暗くもなり、テーマ（明るい・暗い）だけでは決まらない。
 * 膜の色と濃さ、写真の明るさの仮定から、バーの下に見える背景の明るさを求めて決める。
 */
object SystemBarIcons {
    /**
     * 写真の明るさの仮定（sRGB の灰色の度合い）。写真の中身は読まないため、明るい写真でも暗い写真でも
     * 外れにくい中間にする。膜が濃ければ写真の明るさはほとんど効かず、薄いときだけ判定に効く。
     */
    const val ASSUMED_PHOTO_GRAY = 0.5f

    /** 白いアイコンと黒いアイコンのコントラストが等しくなる、背景の相対輝度（これより明るければ暗いアイコン）。 */
    const val LUMINANCE_THRESHOLD = 0.179f

    /** 膜（色 [scrim]・不透明度 [alpha]）を写真の上に敷いたときの背景の相対輝度。 */
    fun backgroundLuminance(
        scrim: Color,
        alpha: Float,
        photoGray: Float = ASSUMED_PHOTO_GRAY,
    ): Float {
        val photo = Color(photoGray, photoGray, photoGray)
        return scrim.copy(alpha = alpha.coerceIn(0f, 1f)).compositeOver(photo).luminance()
    }

    /** 背景が明るくて、アイコンを暗い色にするほうが読めるか。 */
    fun useDarkIconsOnScrim(
        scrim: Color,
        alpha: Float,
    ): Boolean = backgroundLuminance(scrim, alpha) > LUMINANCE_THRESHOLD

    /** 背景の写真が無いとき。バーの下はテーマの背景色なので、暗いテーマなら明るいアイコン、明るいテーマなら暗いアイコン。 */
    fun useDarkIconsWithoutImage(darkTheme: Boolean): Boolean = !darkTheme
}

/**
 * [darkIcons] が true なら状態バーとナビゲーションバーのアイコンを暗い色にする。
 * 端末のダークモードではなく、アプリの見た目（テーマ・膜）に合わせるため、起動時の既定（端末に合わせる）を上書きする。
 */
@Composable
fun SystemBarIconsEffect(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons
    }
}
