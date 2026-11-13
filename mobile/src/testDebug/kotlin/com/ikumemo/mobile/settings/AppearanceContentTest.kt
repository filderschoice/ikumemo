package com.ikumemo.mobile.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ikumemo.mobile.data.Appearance
import com.ikumemo.mobile.designsystem.DarkMode
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.designsystem.TextSizeSetting
import com.ikumemo.mobile.designsystem.ThemePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppearanceContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cardsAndChipsSendSelections() {
        val events = mutableStateListOf<AppearanceEvent>()
        composeRule.setContent {
            IkumemoTheme {
                AppearanceContent(appearance = Appearance(theme = ThemePreset.GENTLE), onEvent = { events += it })
            }
        }

        composeRule.onNodeWithTag(AppearanceTestTags.theme(ThemePreset.GENTLE)).assertIsSelected()
        composeRule.onNodeWithTag(AppearanceTestTags.theme(ThemePreset.SIMPLE)).performScrollTo().performClick()
        composeRule.onNodeWithTag(AppearanceTestTags.darkMode(DarkMode.LIGHT)).performScrollTo().performClick()
        composeRule.onNodeWithTag(AppearanceTestTags.textSize(TextSizeSetting.LARGE)).performScrollTo().performClick()

        assertEquals(
            listOf(
                AppearanceEvent.SelectTheme(ThemePreset.SIMPLE),
                AppearanceEvent.SelectDarkMode(DarkMode.LIGHT),
                AppearanceEvent.SelectTextSize(TextSizeSetting.LARGE),
            ),
            events.toList(),
        )
    }

    @Test
    fun textSizeComesFirstAndBackgroundLast() {
        composeRule.setContent {
            IkumemoTheme {
                AppearanceContent(appearance = Appearance(), onEvent = {})
            }
        }

        // 画面外の部品も含めて並びを比べるため、切り取られない positionInRoot の縦位置を使う
        val textSize = topOf(AppearanceTestTags.textSize(TextSizeSetting.NORMAL))
        val theme = topOf(AppearanceTestTags.theme(ThemePreset.entries.first()))
        val darkMode = topOf(AppearanceTestTags.darkMode(DarkMode.entries.first()))
        val background = topOf(AppearanceTestTags.BACKGROUND_PICK)

        assertTrue("文字の大きさがテーマの見本より上", textSize < theme)
        assertTrue("テーマの見本が暗い画面より上", theme < darkMode)
        assertTrue("暗い画面が背景の写真より上", darkMode < background)
    }

    private fun topOf(tag: String): Float = composeRule.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y
}
