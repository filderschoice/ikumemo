package com.ikumemo.core.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 共有された文の取り込み（BL-197）。 */
class SharedTextParserTest {
    private val parser = SharedTextParser()

    private fun parse(text: String?) = parser.parse(text).map { it.name to it.quantity }

    @Test
    fun ikumemoTextReturnsOriginalItemsAndQuantities() {
        val text = "🛒いくメモ\n・牛乳 2本\n・たまご\n・ポテト チップス\n・豚こま 300g\n・洗剤 1.5L"
        assertEquals(
            listOf(
                "牛乳" to "2本",
                "たまご" to null,
                "ポテト チップス" to null,
                "豚こま" to "300g",
                "洗剤" to "1.5L",
            ),
            parse(text),
        )
    }

    @Test
    fun ikumemoLineIsNotSplitByVoiceDelimiters() {
        // 「と」や「・」を含む品名も、いくメモの行は 1 行 1 品物のまま戻す
        assertEquals(listOf("ごまとうふ" to null, "ハム・チーズ" to "1パック"), parse("🛒いくメモ\n・ごまとうふ\n・ハム・チーズ 1パック"))
    }

    @Test
    fun plainTextIsSplitByLinesWithLeadingMarksRemoved() {
        val text = "買ってきて\n- 牛乳 2本\n* パン\n1. バナナ\n2) りんご3個\n・卵\n☐ 納豆\n① 豆腐"
        assertEquals(
            listOf(
                // 「買ってきて」だけの行は、話して追加と同じく不要な言葉として除く
                "牛乳" to "2本",
                "パン" to null,
                "バナナ" to null,
                "りんご" to "3個",
                "卵" to null,
                "納豆" to null,
                "豆腐" to null,
            ),
            parse(text),
        )
    }

    @Test
    fun plainLineUsesVoiceDelimiters() {
        assertEquals(listOf("牛乳" to "2本", "たまご" to null, "パン" to null), parse("牛乳2本とたまご、パン"))
    }

    @Test
    fun decimalsAreNeitherListMarksNorDelimiters() {
        assertEquals(listOf("1.5L" to null, "洗剤" to "1.5L"), parse("1.5L\n洗剤 1.5L"))
    }

    @Test
    fun duplicatesKeepOnlyTheFirst() {
        assertEquals(listOf("牛乳" to "2本", "パン" to null), parse("牛乳 2本\nパン\n牛乳 1本\n牛乳"))
    }

    @Test
    fun emptyAndBlankTextGiveNothing() {
        assertEquals(emptyList<Any>(), parse(null))
        assertEquals(emptyList<Any>(), parse(""))
        assertEquals(emptyList<Any>(), parse(" \n\t\r\n "))
        assertEquals(emptyList<Any>(), parse("🛒いくメモ"))
    }

    @Test
    fun controlAndBidiCharactersAreRemoved() {
        assertEquals(
            listOf("牛乳" to null, "パン" to null),
            parse("\u0000牛\u202E乳\u0007\r\nパン\u2066"),
        )
    }

    @Test
    fun longTextIsLimitedWithoutCrashing() {
        val many = (1..5_000).joinToString("\n") { "品物$it" }
        val items = parser.parse(many)
        assertEquals(SharedTextParser.MAX_ITEMS, items.size)

        val longLine = "あ".repeat(100_000)
        val single = parser.parse(longLine)
        assertEquals(1, single.size)
        assertTrue(single[0].name.length <= SharedTextParser.MAX_NAME_LENGTH)
    }

    @Test
    fun truncationNeverSplitsSurrogatePairs() {
        val name = "あ".repeat(SharedTextParser.MAX_NAME_LENGTH - 1) + "🥛"
        val result = parser.parse("🛒いくメモ\n・$name")
        // 61 文字目までかかる絵文字は半分にせず、まるごと落とす
        assertEquals("あ".repeat(SharedTextParser.MAX_NAME_LENGTH - 1), result.single().name)
    }
}
