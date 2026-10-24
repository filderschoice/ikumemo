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
}
