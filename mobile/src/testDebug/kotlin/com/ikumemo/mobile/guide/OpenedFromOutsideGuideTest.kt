package com.ikumemo.mobile.guide

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.IkumemoApp
import com.ikumemo.mobile.MainActivity
import com.ikumemo.mobile.ScreenRequest
import com.ikumemo.mobile.TopLevelTabTags
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.UndoableAction
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.reminder.ReminderTestTags
import com.ikumemo.mobile.shoppinglist.TestTags
import com.ikumemo.mobile.testing.TestIkumemoApplication
import com.ikumemo.mobile.testing.awaitTag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 通知から開いた直後はその場のガイドを出さず、次に自分で開いたときに出す（R-52、BL-099 の完了条件）。 */
@RunWith(RobolectricTestRunner::class)
class OpenedFromOutsideGuideTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<TestIkumemoApplication>()

    private fun exists(tag: String) = composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun reminderOpenedFromNotificationHidesGuideUntilOpenedAgain() {
        val recordId =
            runBlocking {
                app.container.guidePrefs.finishOnboarding()
                // メモ画面のガイドは見終えたことにして、おしらせ画面のガイド G-08 だけを確かめる
                listOf(
                    GuideId.G02_VOICE_BUTTON,
                    GuideId.G03_SWIPE_PRACTICE,
                    GuideId.G04_BOUGHT_BUTTON,
                    GuideId.G05_UNDO,
                    GuideId.G06_STORE_CHIP,
                )
                    .forEach { app.container.guidePrefs.markShown(it) }
                val shopping = app.container.shoppingRepository
                val bought = shopping.markBought((shopping.addItem("牛乳") as AddItemResult.Added).itemId)
                (bought as UndoableAction.Bought).recordId
            }
        val setDue = ReminderTestTags.setDue(recordId)
        val g08 = GuideTestTags.bubble(GuideId.G08_DUE_DATE)
        composeRule.setContent {
            IkumemoTheme { IkumemoApp(requestedScreen = ScreenRequest(MainActivity.TAB_REMINDER)) }
        }

        // 通知から開いたおしらせ画面では出さない
        composeRule.awaitTag(setDue)
        composeRule.waitForIdle()
        assertTrue(!exists(g08))

        // メモへ移ってから自分でおしらせを開くと出る
        composeRule.onNodeWithTag(TopLevelTabTags.MEMO).performClick()
        composeRule.awaitTag(TestTags.SETTINGS)
        composeRule.onNodeWithTag(TopLevelTabTags.REMINDER).performClick()
        composeRule.awaitTag(g08)
    }
}
