package com.ikumemo.mobile

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.shoppinglist.TestTags
import com.ikumemo.mobile.testing.TestIkumemoApplication
import com.ikumemo.mobile.testing.awaitCondition
import com.ikumemo.mobile.testing.awaitTag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 設定を開いた後に下タブを押すと、設定が残らずそのタブの画面へ戻る（BL-168）。 */
@RunWith(RobolectricTestRunner::class)
class BottomTabSettingsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<TestIkumemoApplication>()

    private fun exists(tag: String) = composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun start() {
        runBlocking { app.container.guidePrefs.finishOnboarding() }
        composeRule.setContent { IkumemoTheme { IkumemoApp() } }
        composeRule.awaitTag(TestTags.SETTINGS)
    }

    private fun openSettings() {
        composeRule.onNodeWithTag(TestTags.SETTINGS).performClick()
        composeRule.awaitTag(SETTINGS_ROW)
    }

    @Test
    fun sameTabPressReturnsFromSettings() {
        start()
        openSettings()
        composeRule.onNodeWithTag(TopLevelTabTags.MEMO).performClick()
        composeRule.awaitTag(TestTags.SETTINGS)
        assertFalse(exists(SETTINGS_ROW))
    }

    @Test
    fun settingsIsNotRestoredViaAnotherTab() {
        start()
        openSettings()
        composeRule.onNodeWithTag(TopLevelTabTags.REMINDER).performClick()
        composeRule.awaitCondition { !exists(SETTINGS_ROW) }
        composeRule.onNodeWithTag(TopLevelTabTags.MEMO).performClick()
        composeRule.awaitTag(TestTags.SETTINGS)
        assertFalse(exists(SETTINGS_ROW))
    }

    private companion object {
        const val SETTINGS_ROW = "settings_help"
    }
}
