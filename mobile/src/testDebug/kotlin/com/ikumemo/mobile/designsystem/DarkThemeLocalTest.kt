package com.ikumemo.mobile.designsystem

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * きせかえの「暗い画面」が、テーマの外の色（カレンダーの曜日の色など）にも伝わる。端末が明るい設定のまま
 * アプリだけ暗くしたときに、カレンダーが明るい画面用の濃い赤・濃い青を出していた不具合の再発防止（BL-062 で見つけた）。
 */
@RunWith(RobolectricTestRunner::class)
class DarkThemeLocalTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appDarkSettingWinsOverLightSystem() {
        // Robolectric の端末は明るい設定
        var seen: Boolean? = null
        composeRule.setContent { IkumemoTheme(darkTheme = true) { seen = LocalDarkTheme.current } }
        composeRule.waitForIdle()

        assertEquals(true, seen)
    }
}
