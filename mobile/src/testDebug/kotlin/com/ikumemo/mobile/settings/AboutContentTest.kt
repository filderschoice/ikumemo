package com.ikumemo.mobile.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.testing.awaitCondition
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * BL-093 の完了条件「このアプリについて・ご意見・ご要望 が開け、ライセンス一覧が表示される」。
 * BL-143 で ご意見・ご要望 は このアプリについて の中から開くようにした。
 */
@RunWith(RobolectricTestRunner::class)
class AboutContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsListsEightItemsWithoutFeedback() {
        val opened = mutableStateListOf<SettingsItem>()
        composeRule.setContent { IkumemoTheme { SettingsScreen(onBack = {}, onOpen = { opened += it }) } }

        // BL-143: 操作とお知らせを 1 つにまとめ、ご意見・ご要望は このアプリについて の中へ移した
        assertEquals(
            listOf("HELP", "APPEARANCE", "OPERATION", "STORES", "PRODUCTS", "HOME_SCREEN", "DATA", "ABOUT"),
            SettingsItem.entries.map { it.name },
        )
        composeRule.onNodeWithTag("settings_feedback").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_operation").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_about").performScrollTo().performClick()

        assertEquals(listOf(SettingsItem.OPERATION, SettingsItem.ABOUT), opened.toList())
    }

    @Test
    fun aboutOpensFeedback() {
        var feedback = 0
        composeRule.setContent {
            IkumemoTheme {
                AboutContent(version = "1.2.3", onOpenLicenses = {}, onOpenLink = {}, onOpenFeedback = { feedback++ })
            }
        }

        composeRule.onNodeWithTag(AboutTestTags.FEEDBACK).performScrollTo().performClick()

        assertEquals(1, feedback)
    }

    @Test
    fun aboutRowsOpenLicensesAndLinks() {
        val links = mutableStateListOf<ExternalLink>()
        var licenses = 0
        composeRule.setContent {
            IkumemoTheme {
                AboutContent(version = "1.2.3", onOpenLicenses = { licenses++ }, onOpenLink = { links += it })
            }
        }

        composeRule.onNodeWithText("版 1.2.3").assertExists()
        composeRule.onNodeWithTag(AboutTestTags.LICENSES).performClick()
        composeRule.onNodeWithTag(AboutTestTags.link(ExternalLink.PRIVACY_POLICY)).performScrollTo().performClick()
        composeRule.onNodeWithTag(AboutTestTags.link(ExternalLink.GITHUB)).performScrollTo().performClick()
        composeRule.onNodeWithTag(AboutTestTags.link(ExternalLink.STORE)).performScrollTo().performClick()

        assertEquals(1, licenses)
        assertEquals(listOf(ExternalLink.PRIVACY_POLICY, ExternalLink.GITHUB, ExternalLink.STORE), links.toList())
    }

    @Test
    fun licensesListShowsLibraries() {
        composeRule.setContent { IkumemoTheme { LicensesScreen(onBack = {}) } }

        // ビルド時に作った一覧を読み込むまで待つ（名前順の先頭にある AboutLibraries 自身が出ればよい）
        composeRule.awaitCondition {
            composeRule.onAllNodes(hasText("AboutLibraries Core Library")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun faqOpensAnswerAndSendButtonsWork() {
        var form = 0
        var github = 0
        var copied = 0
        composeRule.setContent {
            IkumemoTheme {
                FeedbackContent(
                    deviceInfo = "アプリの版 1.0",
                    onCopyDeviceInfo = { copied++ },
                    onOpenForm = { form++ },
                    onOpenGitHub = { github++ },
                )
            }
        }

        composeRule.onNodeWithTag(FeedbackTestTags.answer(0), useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(FeedbackTestTags.faq(0)).performClick()
        composeRule.onNodeWithTag(FeedbackTestTags.answer(0), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(FeedbackTestTags.COPY).performScrollTo().performClick()
        composeRule.onNodeWithTag(FeedbackTestTags.FORM).performScrollTo().performClick()
        composeRule.onNodeWithTag(FeedbackTestTags.GITHUB).performScrollTo().performClick()

        assertEquals(1, copied)
        assertEquals(1, form)
        assertEquals(1, github)
    }
}
