package com.ikumemo.mobile.data.backup

import com.ikumemo.core.text.ProductNameNormalizer

/**
 * 読み込む品目の正規化名を、品名から今の正規化（R-02）で作り直す（BL-236）。
 *
 * ファイルの正規化名をそのまま入れると、手で作ったファイルや読みの辞書が変わる前に書き出したファイルでは
 * 今の正規化と食い違い、メモにある品名が「メモにあります」にならない（`ShoppingRepository.findInMemo`）。
 * 作り直すと同じ名前になる品目どうし（辞書が変わる前は別の品目だった「卵」と「玉子」など）と、
 * 作り直した名前がファイルの名前のまま入れる品目と重なる品目は、正規化名の一意の制約を破らないようファイルの値のまま入れる
 * （品目をまとめると記録・おしらせの参照先が変わるため、まとめない）。ファイルの正規化名は [BackupValidator] で重なりが無いことを確かめ済み。
 */
internal fun renormalized(
    products: List<BackupProduct>,
    normalizer: ProductNameNormalizer,
): List<BackupProduct> {
    val fresh = products.associate { it.id to normalizer.normalize(it.name) }
    val clashing = fresh.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    val keepFileValue = products.filter { fresh.getValue(it.id) in clashing }.mapTo(HashSet()) { it.id }
    while (true) {
        val taken = products.filter { it.id in keepFileValue }.mapTo(HashSet()) { it.normalizedName }
        val more = products.filter { it.id !in keepFileValue && fresh.getValue(it.id) in taken }
        if (more.isEmpty()) break
        more.mapTo(keepFileValue) { it.id }
    }
    return products.map { if (it.id in keepFileValue) it else it.copy(normalizedName = fresh.getValue(it.id)) }
}
