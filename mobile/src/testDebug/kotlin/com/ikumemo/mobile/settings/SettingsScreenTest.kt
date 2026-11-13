package com.ikumemo.mobile.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** BL-139 の完了条件「設定の一覧に見出しと説明が出て、各行を押すと今までどおりの項目が開く」。 */
@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sectionsCoverEveryItemOnceWithHelpOnTop() {
        val ordered = SettingsSection.entries.flatMap { it.items }

        assertEquals(SettingsItem.entries.toSet(), ordered.toSet())
        assertEquals(SettingsItem.entries.size, ordered.size)
        assertEquals(SettingsItem.HELP, ordered.first())
        assertEquals(null, SettingsSection.TOP.title)
    }

    @Test
    fun headingsAndDetailsAreShown() {
        composeRule.setContent { IkumemoTheme { SettingsScreen(onBack = {}, onOpen = {}) } }

        val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        listOf("look", "registered", "app").forEach { name ->
            composeRule.onNodeWithTag("settings_section_$name").performScrollTo().assert(isHeading)
        }
        composeRule.onNodeWithText("見た目と操作").assertExists()
        composeRule.onNodeWithText("登録したもの").performScrollTo()
        composeRule.onNodeWithText("このアプリ").performScrollTo()
        composeRule.onNodeWithText("テーマ・文字の大きさ・背景の写真").performScrollTo()
        composeRule.onNodeWithText("○ ボタン・振動・お知らせの時刻").performScrollTo()
        composeRule.onNodeWithText("書き出し・読み込み").performScrollTo()
        composeRule.onNodeWithText("版・ライセンス・ご意見・ご要望").performScrollTo()
    }

    @Test
    fun everyRowOpensItsItem() {
        val opened = mutableStateListOf<SettingsItem>()
        composeRule.setContent { IkumemoTheme { SettingsScreen(onBack = {}, onOpen = { opened += it }) } }

        SettingsItem.entries.forEach { item ->
            composeRule.onNodeWithTag("settings_${item.name.lowercase()}").performScrollTo().performClick()
        }

        assertEquals(SettingsItem.entries.toList(), opened.toList())
    }
}
