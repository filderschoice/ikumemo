package com.ikumemo.mobile

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.UndoableAction
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.designsystem.TextSizeSetting
import com.ikumemo.mobile.guide.GuideId
import com.ikumemo.mobile.guide.GuideTestTags
import com.ikumemo.mobile.reminder.ReminderTestTags
import com.ikumemo.mobile.settings.AppearanceTestTags
import com.ikumemo.mobile.shoppinglist.TestTags
import com.ikumemo.mobile.testing.TestIkumemoApplication
import com.ikumemo.mobile.testing.awaitCondition
import com.ikumemo.mobile.testing.awaitTag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun tabPressFromTwoLevelsDeepShowsThatTabsList() {
        start()
        openSettings()
        // 設定の下の画面（着せ替え）まで 2 段積んだ状態から下タブを押す
        composeRule.onNodeWithTag(SETTINGS_APPEARANCE).performClick()
        composeRule.awaitTag(APPEARANCE_MARK)
        composeRule.onNodeWithTag(TopLevelTabTags.MEMO).performClick()
        composeRule.awaitTag(TestTags.SETTINGS)
        assertFalse(exists(APPEARANCE_MARK))
        assertFalse(exists(SETTINGS_ROW))
    }

    @Test
    fun outsideLaunchFromSettingsKeepsOutsideRouteThroughIntermediateScreens() {
        val recordId =
            runBlocking {
                app.container.guidePrefs.finishOnboarding()
                listOf(
                    GuideId.G02_VOICE_BUTTON,
                    GuideId.G03_SWIPE_PRACTICE,
                    GuideId.G04_BOUGHT_BUTTON,
                    GuideId.G05_UNDO,
                    GuideId.G06_STORE_CHIP,
                ).forEach { app.container.guidePrefs.markShown(it) }
                val shopping = app.container.shoppingRepository
                val bought = shopping.markBought((shopping.addItem("牛乳") as AddItemResult.Added).itemId)
                (bought as UndoableAction.Bought).recordId
            }
        val request = mutableStateOf<ScreenRequest?>(null)
        composeRule.setContent { IkumemoTheme { IkumemoApp(requestedScreen = request.value) } }
        composeRule.awaitTag(TestTags.SETTINGS)
        openSettings()
        composeRule.onNodeWithTag(SETTINGS_APPEARANCE).performClick()
        composeRule.awaitTag(APPEARANCE_MARK)

        // 設定の下の画面を開いた状態へ、ウィジェット等からおしらせを開く要求が来る
        request.value = ScreenRequest(MainActivity.TAB_REMINDER)
        composeRule.awaitTag(ReminderTestTags.setDue(recordId))
        composeRule.waitForIdle()
        // pop の途中で経由した画面で外から開いた印が外れていれば、ここでガイドが出てしまう
        assertTrue(!exists(GuideTestTags.bubble(GuideId.G08_DUE_DATE)))
    }

    private companion object {
        const val SETTINGS_ROW = "settings_help"
        const val SETTINGS_APPEARANCE = "settings_appearance"
        val APPEARANCE_MARK = AppearanceTestTags.textSize(TextSizeSetting.NORMAL)
    }
}
