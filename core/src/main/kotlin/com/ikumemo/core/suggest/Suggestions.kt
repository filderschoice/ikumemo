package com.ikumemo.core.suggest

import com.ikumemo.core.text.ProductNameNormalizer
import java.time.LocalDate

/** 候補を選ぶための品目の要約（DB の品目から作る）。 */
data class ProductSummary(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val purchaseCount: Int,
    val lastPurchasedAt: LocalDate?,
    val hidden: Boolean = false,
)

/**
 * 「いつもの」と入力中の候補（F-20・F-21）。
 *
 * - いつもの: 1 回以上買った物を、購入回数の多い順 → 最近買った順 → 品名順に並べる
 * - 入力中の候補: 正規化名（ひらがな・カタカナ・漢字のゆれを吸収、R-02）が入力で始まる物を先に、含む物を後に並べ、
 *   それぞれの中は「いつもの」と同じ順にする
 *
 * どちらも候補に出さない印（[ProductSummary.hidden]）の物と、[excludeIds]（メモにある物）を除く。
 */
class Suggestions(
    private val normalizer: ProductNameNormalizer = ProductNameNormalizer(),
) {
    fun usual(
        products: List<ProductSummary>,
        excludeIds: Set<Long> = emptySet(),
        limit: Int = DEFAULT_USUAL_LIMIT,
    ): List<ProductSummary> =
        products
            .filter { !it.hidden && it.purchaseCount > 0 && it.id !in excludeIds }
            .sortedWith(usualOrder)
            .take(limit)

    fun candidates(
        query: String,
        products: List<ProductSummary>,
        excludeIds: Set<Long> = emptySet(),
        limit: Int = DEFAULT_CANDIDATE_LIMIT,
    ): List<ProductSummary> {
        val normalizedQuery = normalizer.normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()
        val visible = products.filter { !it.hidden && it.id !in excludeIds }
        val (prefix, others) = visible.partition { it.normalizedName.startsWith(normalizedQuery) }
        val contains = others.filter { it.normalizedName.contains(normalizedQuery) }
        return (prefix.sortedWith(usualOrder) + contains.sortedWith(usualOrder)).take(limit)
    }

    companion object {
        const val DEFAULT_USUAL_LIMIT = 12
        const val DEFAULT_CANDIDATE_LIMIT = 5

        private val usualOrder: Comparator<ProductSummary> =
            compareByDescending<ProductSummary> { it.purchaseCount }
                .thenByDescending { it.lastPurchasedAt ?: LocalDate.MIN }
                .thenBy { it.normalizedName }
    }
}
