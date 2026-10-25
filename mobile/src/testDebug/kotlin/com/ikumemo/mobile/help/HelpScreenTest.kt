package com.ikumemo.mobile.help

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.IkumemoApp
import com.ikumemo.mobile.calendar.CalendarTestTags
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.guide.GuideId
import com.ikumemo.mobile.shoppinglist.TestTags
import com.ikumemo.mobile.testing.TestIkumemoApplication
import com.ikumemo.mobile.testing.awaitTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 使い方の画面（H-01〜H-03）。 */
@RunWith(RobolectricTestRunner::class)
class HelpScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<TestIkumemoApplication>()

    @Test
    fun pageButtonsShowGuideAgainAndTry() {
        val events = mutableStateListOf<String>()
        val page =
            HelpPage(
                id = "calendar",
                title = "カレンダー",
                blocks = listOf(HelpBlock.Paragraph("日付ごとに見られます。")),
                tryScreen = HelpScreens.CALENDAR,
                guide = GuideId.G07_CALENDAR_DAY,
            )
        composeRule.setContent {
            IkumemoTheme {
                HelpPageContent(page, onTry = { events += "try:$it" }, onShowGuide = {
                        g,
                        s,
                    ->
                    events += "guide:${g.storageKey}:$s"
                })
            }
        }

        composeRule.onNodeWithText("日付ごとに見られます。").performScrollTo()
        composeRule.onNodeWithTag(HelpTestTags.SHOW_GUIDE).performClick()
        composeRule.onNodeWithTag(HelpTestTags.TRY).performClick()

        assertEquals(listOf("guide:g07:calendar", "try:calendar"), events.toList())
    }

    @Test
    fun pageWithoutGuideHasOnlyTry() {
        val page = HelpPage("search", "探す", listOf(HelpBlock.Paragraph("本文")), tryScreen = HelpScreens.CALENDAR)
        composeRule.setContent { IkumemoTheme { HelpPageContent(page, onTry = {}, onShowGuide = { _, _ -> }) } }

        composeRule.onNodeWithTag(HelpTestTags.SHOW_GUIDE).assertDoesNotExist()
        composeRule.onNodeWithTag(HelpTestTags.TRY).performScrollTo()
    }

    @Test
    fun settingsHelpPageTryOpensCalendarAndShowGuideAgainResetsIt() {
        runBlocking {
            app.container.guidePrefs.finishOnboarding()
            app.container.guidePrefs.markShown(GuideId.G07_CALENDAR_DAY)
        }
        composeRule.setContent { IkumemoTheme { IkumemoApp() } }

        composeRule.awaitTag(TestTags.SETTINGS)
        composeRule.onNodeWithTag(TestTags.SETTINGS).performClick()
        composeRule.onNodeWithTag("settings_help").performClick()
        composeRule.awaitTag(HelpTestTags.page("calendar"))
        composeRule.onNodeWithTag(HelpTestTags.page("calendar")).performScrollTo().performClick()
        composeRule.awaitTag(HelpTestTags.SHOW_GUIDE)
        composeRule.onNodeWithTag(HelpTestTags.SHOW_GUIDE).performScrollTo().performClick()

        // カレンダーが開き、G-07 が未表示に戻っている
        composeRule.awaitTag(CalendarTestTags.SEARCH)
        assertFalse(GuideId.G07_CALENDAR_DAY in runBlocking { app.container.guidePrefs.state.first().shown })
    }

    @Test
    fun emptyMemoLinksToVoicePage() {
        // H-03: メモが空の画面から「話して入れる」のページへ
        runBlocking {
            app.container.guidePrefs.finishOnboarding()
            app.container.guidePrefs.setDisabled(true)
        }
        composeRule.setContent { IkumemoTheme { IkumemoApp() } }

        composeRule.awaitTag(TestTags.EMPTY_HELP)
        composeRule.onNodeWithTag(TestTags.EMPTY_HELP).performClick()

        composeRule.awaitTag(HelpTestTags.TRY)
        composeRule.onNodeWithText("話して入れる").assertExists()
    }
}
