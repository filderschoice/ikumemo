package com.ikumemo.mobile.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.widget.PinnableWidget
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeScreenContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pinButtonsRequestEachWidget() {
        val pinned = mutableStateListOf<PinnableWidget>()
        composeRule.setContent { IkumemoTheme { HomeScreenContent(pinSupported = true, onPin = { pinned += it }) } }

        composeRule.onNodeWithTag(HomeScreenTestTags.pin(PinnableWidget.VOICE)).performScrollTo().performClick()
        composeRule.onNodeWithTag(HomeScreenTestTags.pin(PinnableWidget.MEMO_LIST)).performScrollTo().performClick()

        assertEquals(listOf(PinnableWidget.VOICE, PinnableWidget.MEMO_LIST), pinned.toList())
    }

    @Test
    fun unsupportedHomeAppShowsHowToInsteadOfButtons() {
        composeRule.setContent { IkumemoTheme { HomeScreenContent(pinSupported = false, onPin = {}) } }

        composeRule.onNodeWithTag(HomeScreenTestTags.pin(PinnableWidget.VOICE)).assertDoesNotExist()
        composeRule.onNodeWithTag(HomeScreenTestTags.pin(PinnableWidget.MEMO_LIST)).assertDoesNotExist()
        composeRule.onNodeWithText("ここから置く操作に対応していません", substring = true).performScrollTo()
    }
}
