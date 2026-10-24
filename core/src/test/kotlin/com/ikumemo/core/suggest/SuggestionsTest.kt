package com.ikumemo.core.suggest

import com.ikumemo.core.text.ProductNameNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SuggestionsTest {
    private val normalizer = ProductNameNormalizer()
    private val suggestions = Suggestions(normalizer)

    private fun product(
        id: Long,
        name: String,
        count: Int,
        last: LocalDate? = null,
        hidden: Boolean = false,
    ) = ProductSummary(id, name, normalizer.normalize(name), count, last, hidden)

    private val day = LocalDate.of(2026, 10, 1)

    @Test
    fun usualIsOrderedByCountThenRecency() {
        val products =
            listOf(
                product(1, "牛乳", 5, day),
                product(2, "たまご", 8, day),
                product(3, "パン", 5, day.plusDays(3)),
                product(4, "洗剤", 0),
            )

        assertEquals(listOf("たまご", "パン", "牛乳"), suggestions.usual(products).map { it.name })
    }

    @Test
    fun usualExcludesHiddenAndItemsInMemoAndRespectsLimit() {
        val products =
            listOf(
                product(1, "牛乳", 5, day),
                product(2, "たまご", 8, day),
                product(3, "パン", 3, day),
                product(4, "ラップ", 9, day, hidden = true),
            )

        assertEquals(listOf("牛乳"), suggestions.usual(products, excludeIds = setOf(2), limit = 1).map { it.name })
    }

    @Test
    fun candidatesAbsorbKanaAndKanjiVariants() {
        val products = listOf(product(1, "卵", 3, day), product(2, "玉ねぎ", 1, day), product(3, "牛乳", 9, day))

        assertEquals(listOf("卵"), suggestions.candidates("タマゴ", products).map { it.name })
        assertEquals(listOf("卵"), suggestions.candidates("たまご", products).map { it.name })
        assertEquals(listOf("卵", "玉ねぎ"), suggestions.candidates("たま", products).map { it.name })
    }

    @Test
    fun prefixMatchesComeBeforeContainsMatches() {
        val products =
            listOf(
                product(1, "低脂肪牛乳", 9, day),
                product(2, "牛乳", 1, day),
                product(3, "牛肉", 2, day),
            )

        assertEquals(listOf("牛肉", "牛乳", "低脂肪牛乳"), suggestions.candidates("ぎゅう", products).map { it.name })
    }

    @Test
    fun blankQueryHasNoCandidates() {
        assertEquals(emptyList<ProductSummary>(), suggestions.candidates(" ", listOf(product(1, "牛乳", 1, day))))
    }

    @Test
    fun candidatesExcludeHiddenAndInMemo() {
        val products = listOf(product(1, "牛乳", 1, day), product(2, "牛肉", 1, day, hidden = true))

        assertEquals(emptyList<ProductSummary>(), suggestions.candidates("ぎゅう", products, excludeIds = setOf(1)))
    }
}
