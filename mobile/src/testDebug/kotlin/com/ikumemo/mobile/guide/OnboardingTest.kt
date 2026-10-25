package com.ikumemo.mobile.guide

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.IkumemoApp
import com.ikumemo.mobile.MainActivity
import com.ikumemo.mobile.ScreenRequest
import com.ikumemo.mobile.TopLevelTabTags
import com.ikumemo.mobile.calendar.CalendarTestTags
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.testing.TestIkumemoApplication
import com.ikumemo.mobile.testing.awaitCondition
import com.ikumemo.mobile.testing.awaitTag
import com.ikumemo.mobile.widget.PinnableWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** はじめての案内（G-01、BL-080 の完了条件）。 */
@RunWith(RobolectricTestRunner::class)
class OnboardingTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun exists(tag: String) = composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun pagesGoToHomeScreenPageWherePinAndLaterWork() {
        val pinned = mutableStateListOf<PinnableWidget>()
        val finished = mutableIntStateOf(0)
        composeRule.setContent {
            IkumemoTheme {
                OnboardingContent(pinSupported = true, onPin = { pinned += it }, onFinish = { finished.intValue++ })
            }
        }

        composeRule.onNodeWithTag(OnboardingTestTags.page(OnboardingPage.VOICE)).assertIsDisplayed()
        repeat(OnboardingPage.entries.size - 1) { composeRule.onNodeWithTag(OnboardingTestTags.NEXT).performClick() }
        composeRule.onNodeWithTag(OnboardingTestTags.page(OnboardingPage.HOME_SCREEN)).assertIsDisplayed()

        composeRule.onNodeWithTag(OnboardingTestTags.pin(PinnableWidget.VOICE)).performClick()
        composeRule.onNodeWithTag(OnboardingTestTags.pin(PinnableWidget.MEMO_LIST)).performClick()
        // 「置く」では閉じない
        assertEquals(0, finished.intValue)
        assertEquals(listOf(PinnableWidget.VOICE, PinnableWidget.MEMO_LIST), pinned.toList())

        composeRule.onNodeWithText("あとで").performClick()
        assertEquals(1, finished.intValue)
    }

    @Test
    fun skipIsAvailableFromFirstPage() {
        val finished = mutableIntStateOf(0)
        composeRule.setContent {
            IkumemoTheme { OnboardingContent(pinSupported = true, onPin = {}, onFinish = { finished.intValue++ }) }
        }

        composeRule.onNodeWithTag(OnboardingTestTags.SKIP).performClick()

        assertEquals(1, finished.intValue)
    }

    @Test
    fun unsupportedHomeAppExplainsHowToPlace() {
        composeRule.setContent { IkumemoTheme { OnboardingContent(pinSupported = false, onPin = {}, onFinish = {}) } }
        repeat(OnboardingPage.entries.size - 1) { composeRule.onNodeWithTag(OnboardingTestTags.NEXT).performClick() }

        composeRule.onNodeWithTag(OnboardingTestTags.pin(PinnableWidget.VOICE)).assertDoesNotExist()
        composeRule.onNodeWithText("ここから置く操作に対応していません", substring = true).assertIsDisplayed()
    }

    @Test
    fun appShowsOnboardingOnFirstLaunchOnlyAndSkipOpensMemo() {
        composeRule.setContent { IkumemoTheme { IkumemoApp() } }

        composeRule.awaitTag(OnboardingTestTags.SKIP)
        composeRule.onNodeWithTag(OnboardingTestTags.SKIP).performClick()

        // とばしたらメモ画面（下タブ）になり、見終えたことが保存される（次の起動では出ない）
        composeRule.awaitCondition { !exists(OnboardingTestTags.SKIP) && exists(TopLevelTabTags.MEMO) }
        val app = ApplicationProvider.getApplicationContext<TestIkumemoApplication>()
        composeRule.awaitCondition { runBlocking { app.container.guidePrefs.state.first().onboardingDone } }
    }

    @Test
    fun appSkipsOnboardingWhenAlreadyDone() {
        val app = ApplicationProvider.getApplicationContext<TestIkumemoApplication>()
        runBlocking { app.container.guidePrefs.finishOnboarding() }

        composeRule.setContent { IkumemoTheme { IkumemoApp() } }

        composeRule.awaitTag(TopLevelTabTags.MEMO)
        composeRule.onNodeWithTag(OnboardingTestTags.SKIP).assertDoesNotExist()
    }

    @Test
    fun appOpenedFromShortcutGoesStraightToThatScreen() {
        // 通知・ウィジェット・ショートカットからの起動では案内を挟まない
        composeRule.setContent {
            IkumemoTheme { IkumemoApp(requestedScreen = ScreenRequest(MainActivity.TAB_CALENDAR)) }
        }

        composeRule.awaitTag(CalendarTestTags.SEARCH)
        composeRule.onNodeWithTag(OnboardingTestTags.SKIP).assertDoesNotExist()
    }
}
