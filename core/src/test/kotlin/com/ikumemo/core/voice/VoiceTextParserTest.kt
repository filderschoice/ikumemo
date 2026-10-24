package com.ikumemo.core.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceTextParserTest {
    private val parser = VoiceTextParser()

    private fun parse(text: String) = parser.parse(text).map { it.name to it.quantity }

    @Test
    fun splitsByToAndExtractsQuantity() {
        assertEquals(listOf("牛乳" to "2本", "たまご" to null), parse("牛乳2本とたまご"))
    }

    @Test
    fun splitsThreeItemsWithTo() {
        assertEquals(
            listOf("牛乳" to null, "卵" to null, "トイレットペーパー" to null),
            parse("牛乳と卵とトイレットペーパー"),
        )
    }

    @Test
    fun splitsByPunctuationSpacesAndConnectives() {
        assertEquals(
            listOf("お茶" to null, "パン" to null, "洗剤" to null, "バナナ" to null, "ラップ" to null),
            parse("お茶、それからパン　洗剤 あとバナナ。それとラップ"),
        )
    }

    @Test
    fun keepsToInsideDictionaryWords() {
        assertEquals(listOf("いとこんにゃく" to null, "とうふ" to null), parse("いとこんにゃくととうふ"))
        assertEquals(listOf("なっとう" to "3パック"), parse("なっとう3パック"))
        assertEquals(listOf("トマト" to null, "牛乳" to null), parse("トマトと牛乳"))
    }

    @Test
    fun keepsToInsideKnownNames() {
        val withKnown = VoiceTextParser(knownNames = listOf("ほうとう"))

        assertEquals(listOf("ほうとう" to null, "みそ" to null), withKnown.parse("ほうとうとみそ").map { it.name to it.quantity })
    }

    @Test
    fun extractsKanjiNumeralsAndCounters() {
        assertEquals(listOf("卵" to "1パック"), parse("卵一パック"))
        assertEquals(listOf("トイレットペーパー" to "12ロール"), parse("トイレットペーパー十二ロール"))
        assertEquals(listOf("豚肉" to "300g"), parse("豚肉300グラム"))
        assertEquals(listOf("水" to "2L"), parse("水2リットル"))
        assertEquals(listOf("みかん" to "10個"), parse("みかん10こ"))
    }

    @Test
    fun normalizesFullWidthDigits() {
        assertEquals(listOf("牛乳" to "2本"), parse("牛乳２本"))
    }

    @Test
    fun spokenQuantitiesAreUnified() {
        assertEquals(listOf("たまご" to "1個"), parse("たまごいっこ"))
        assertEquals(listOf("たまご" to "1個"), parse("たまご1個"))
        assertEquals(listOf("たまご" to "1個"), parse("たまご一個"))
        assertEquals(listOf("レモン" to "1つ"), parse("レモンひとつ"))
    }

    @Test
    fun correctsMisrecognizedNihon() {
        assertEquals(listOf("牛乳" to "2本"), parse("牛乳日本"))
        assertEquals(listOf("牛乳" to "2本"), parse("牛乳二本"))
    }

    @Test
    fun removesFillerWords() {
        assertEquals(listOf("牛乳" to null), parse("牛乳を買わなきゃ"))
        assertEquals(listOf("洗剤" to null), parse("洗剤が欲しい"))
        assertEquals(listOf("パン" to null, "たまご" to null), parse("パンとたまご買っといて"))
        assertEquals(listOf("ラップ" to null), parse("ラップが切れた"))
    }

    @Test
    fun namesEndingWithKanaCounterAreNotSplit() {
        assertEquals(listOf("いちご" to null, "ねこ" to null), parse("いちごとねこ"))
    }

    @Test
    fun emptyAndDelimiterOnlyTextGivesNothing() {
        assertEquals(emptyList<Pair<String, String?>>(), parse(""))
        assertEquals(emptyList<Pair<String, String?>>(), parse("、 と 。"))
    }

    @Test
    fun parseBestPrefersCandidateMatchingKnownNames() {
        val withKnown = VoiceTextParser(knownNames = listOf("牛乳", "食パン"))

        val best = withKnown.parseBest(listOf("ぎゅうにゅうと職パン", "牛乳と食パン", "牛乳とパン"))

        assertEquals(listOf("牛乳", "食パン"), best.map { it.name })
    }

    @Test
    fun parseBestKeepsFirstCandidateOnTie() {
        assertEquals(listOf("みかん"), parser.parseBest(listOf("みかん", "蜜柑")).map { it.name })
        assertEquals(emptyList<NameAndQuantity>(), parser.parseBest(emptyList()))
    }
}
