package com.ikumemo.core.text

import java.text.Normalizer

/**
 * 品名の正規化（R-02）。正規化した名前が一致すれば同じ物とみなす。
 *
 * 1. NFKC で全角英数・半角カナ・互換文字をそろえ、英字を小文字にし、空白を除く
 * 2. 漢字表記を [ReadingDictionary] の読みへ置き換える（長い語から優先）
 * 3. カタカナをひらがなへ変換する（文字コードの差分）
 *
 * 「たまご」「タマゴ」「卵」「玉子」はすべて「たまご」になる。数量は比較しない。
 */
class ProductNameNormalizer(
    private val dictionary: ReadingDictionary = ReadingDictionary.DEFAULT,
) {
    fun normalize(name: String): String {
        val nfkc = Normalizer.normalize(name, Normalizer.Form.NFKC).lowercase()
        val compact = nfkc.filterNot { it.isWhitespace() }
        return toHiragana(dictionary.replaceWithReadings(compact))
    }

    /** 同じ物かどうか（R-02）。 */
    fun isSame(
        a: String,
        b: String,
    ): Boolean = normalize(a) == normalize(b)

    companion object {
        private const val KATAKANA_START = 'ァ'
        private const val KATAKANA_END = 'ヶ'
        private const val KANA_OFFSET = 0x60

        /** カタカナ（ァ〜ヶ）をひらがなへ変換する。長音「ー」などそれ以外の文字はそのまま。 */
        fun toHiragana(text: String): String =
            buildString(text.length) {
                text.forEach { c ->
                    append(if (c in KATAKANA_START..KATAKANA_END) c - KANA_OFFSET else c)
                }
            }
    }
}
