package com.ikumemo.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductNameNormalizerTest {
    private val normalizer = ProductNameNormalizer()

    @Test
    fun eggVariantsBecomeSameName() {
        val names = listOf("たまご", "タマゴ", "卵", "玉子", "ﾀﾏｺﾞ", " たまご ")
        names.forEach { assertEquals(it, "たまご", normalizer.normalize(it)) }
    }

    @Test
    fun kanjiAndKanaOfMilkAreSame() {
        assertTrue(normalizer.isSame("牛乳", "ぎゅうにゅう"))
        assertTrue(normalizer.isSame("牛乳", "ギュウニュウ"))
    }

    @Test
    fun longestDictionaryWordIsPreferred() {
        assertEquals("いとこんにゃく", normalizer.normalize("糸蒟蒻"))
        assertEquals("ぶたにく", normalizer.normalize("豚肉"))
        assertEquals("ていしぼうにゅう", normalizer.normalize("低脂肪乳"))
    }

    @Test
    fun wordsOutsideDictionaryKeepKanji() {
        assertEquals("特選ぎゅうにゅう", normalizer.normalize("特選牛乳"))
    }

    @Test
    fun fullWidthAlphanumericAndCaseAreUnified() {
        assertEquals("abc123", normalizer.normalize("ＡＢＣ１２３"))
        assertTrue(normalizer.isSame("Ｔシャツ", "tしゃつ"))
    }

    @Test
    fun longVowelMarkIsKept() {
        assertEquals("といれっとぺーぱー", normalizer.normalize("トイレットペーパー"))
        assertEquals("こーひー", normalizer.normalize("珈琲"))
    }

    @Test
    fun differentProductsAreNotSame() {
        assertFalse(normalizer.isSame("牛乳", "豆乳"))
        assertFalse(normalizer.isSame("たまご", "たまねぎ"))
    }

    @Test
    fun blankNameBecomesEmpty() {
        assertEquals("", normalizer.normalize("   "))
    }

    @Test
    fun toHiraganaConvertsOnlyKatakanaRange() {
        assertEquals("ぁあゔゖー・", ProductNameNormalizer.toHiragana("ァアヴヶー・"))
    }
}
