package com.ikumemo.mobile.designsystem

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * BL-202。メモが空のときの「使い方を見る」（EMPTY_HELP・NO_VOICE_HELP）が使う [photoTextButtonColors] は、
 * 写真とペンの色があるときはペンの色で描き、無いときは既定の文字ボタンの色のままにする。
 */
@RunWith(RobolectricTestRunner::class)
class PhotoTextButtonColorsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun usesPenColorWhenPhotoInkIsSet() {
        val ink = Color(0xFF123456)
        var seen: ButtonColors? = null
        var expected: ButtonColors? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalPhotoInk provides ink) { seen = photoTextButtonColors() }
            expected = ButtonDefaults.textButtonColors(contentColor = ink)
        }
        composeRule.waitForIdle()

        assertEquals(expected, seen)
    }

    @Test
    fun keepsDefaultWhenNoPhotoInk() {
        var seen: ButtonColors? = null
        var expected: ButtonColors? = null
        composeRule.setContent {
            seen = photoTextButtonColors()
            expected = ButtonDefaults.textButtonColors()
        }
        composeRule.waitForIdle()

        assertEquals(expected, seen)
    }
}
