package com.ikumemo.mobile.help

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.guide.GuideId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 使い方の正本（assets/help/）の読み込み（H-01・H-02、BL-081 の完了条件）。 */
@RunWith(RobolectricTestRunner::class)
class HelpContentTest {
    private val repository = HelpRepository(ApplicationProvider.getApplicationContext<Context>().assets)

    private val screens =
        setOf(
            HelpScreens.MEMO,
            HelpScreens.ADD_TEXT,
            HelpScreens.CALENDAR,
            HelpScreens.REMINDER,
            HelpScreens.APPEARANCE,
            HelpScreens.HOME_SCREEN,
            HelpScreens.FEEDBACK,
            HelpScreens.DATA,
        )

    @Test
    fun everyPageInIndexCanBeShown() {
        val chapters = repository.chapters()
        // PLAN §2.12 の目次の章（LINE などで送るは「メモに入れる」の send.md、BL-196）
        assertEquals(
            listOf("はじめに", "メモに入れる", "買ったとき", "見返す", "期限とお知らせ", "ホーム画面に置く", "見た目を変える", "困ったとき"),
            chapters.map { it.title },
        )
        chapters.flatMap { it.pages }.forEach { ref ->
            val page = requireNotNull(repository.page(ref.id)) { "${ref.id} が読めない" }
            assertEquals(ref.title, page.title)
            assertTrue("${ref.id} の本文が空", page.blocks.isNotEmpty())
            // 「やってみる」の行き先は開ける画面だけ
            assertTrue("${ref.id} の try: ${page.tryScreen}", page.tryScreen in screens)
        }
    }

    @Test
    fun everyGuideCanBeShownAgainFromSomePage() {
        // H-02: G-02〜G-08 のどれも、使い方のどこかから「ガイドをもう一度見る」で出し直せる
        val guides = repository.chapters().flatMap { it.pages }.mapNotNull { repository.page(it.id)?.guide }
        assertEquals(GuideId.entries.toSet(), guides.toSet())
    }

    @Test
    fun troubleEntryPagesExist() {
        // H-03 の入口から開くページ
        listOf(HelpPages.VOICE, HelpPages.NO_VOICE, HelpPages.NOT_ARRIVING).forEach {
            assertNotNull(it, repository.page(it))
        }
    }

    @Test
    fun parserReadsHeadingsParagraphsBulletsAndMeta() {
        val page =
            HelpParser.page(
                "sample",
                """
                # 題名

                <!-- try: calendar; guide: g07 -->

                はじめの
                段落。

                ## 小見出し

                - ひとつめ
                - ふたつめ
                """.trimIndent(),
            )

        assertEquals("題名", page.title)
        assertEquals(HelpScreens.CALENDAR, page.tryScreen)
        assertEquals(GuideId.G07_CALENDAR_DAY, page.guide)
        assertEquals(
            listOf(
                HelpBlock.Paragraph("はじめの段落。"),
                HelpBlock.Heading("小見出し"),
                HelpBlock.Bullets(listOf("ひとつめ", "ふたつめ")),
            ),
            page.blocks,
        )
    }

    @Test
    fun missingPageIsNullInsteadOfCrash() {
        assertEquals(null, repository.page("no-such-page"))
    }
}
