package com.ikumemo.core.voice

import com.ikumemo.core.text.DefaultReadings
import com.ikumemo.core.text.ProductNameNormalizer
import java.text.Normalizer

/**
 * 音声認識の結果を品物ごとに分ける（F-01・F-02、PLAN §4.4）。
 *
 * - 区切り: 「と」「、」「。」「それから」「あと」「それと」、半角/全角スペース
 * - 品名の中の「と」（「いとこんにゃく」「とうふ」等）は分けない。過去に入れた品名と既定の辞書を最長一致で守る
 * - 品名の後ろの数量を取り出す（[QuantityParser]）
 * - 「を買う」「が欲しい」「買っといて」などの不要な言葉を取り除く（区切りとしても扱う）
 *
 * @param knownNames 過去に入れた品名（品目の表示名）。品名の中の「と」を守るのに使う
 */
class VoiceTextParser(
    knownNames: Collection<String> = emptyList(),
    private val normalizer: ProductNameNormalizer = ProductNameNormalizer(),
) {
    private val protectedWords: List<String> =
        (knownNames.map(::nfkc) + DEFAULT_PROTECTED + QuantityParser.spokenQuantities.map { it.first })
            .filter { it.length >= 2 && it.contains('と') }
            .distinct()
            .sortedByDescending { it.length }

    private val knownNormalized: Set<String> = knownNames.map(normalizer::normalize).toSet()

    fun parse(text: String): List<NameAndQuantity> =
        splitSegments(nfkc(text))
            .map(::removeFillers)
            .filter { it.isNotEmpty() }
            .map(QuantityParser::split)
            .filter { it.name.isNotEmpty() }

    /**
     * 認識候補（N-best）のうち、過去の品名と一致する品物が最も多いものを採用する（PLAN §4.4）。
     * 同点なら認識エンジンの順位（先頭）を優先する。
     */
    fun parseBest(candidates: List<String>): List<NameAndQuantity> =
        candidates
            .map(::parse)
            .withIndex()
            .maxWithOrNull(
                compareBy<IndexedValue<List<NameAndQuantity>>> { (_, items) ->
                    items.count { normalizer.normalize(it.name) in knownNormalized }
                }.thenByDescending { it.index },
            )?.value
            .orEmpty()

    private fun splitSegments(text: String): List<String> {
        val segments = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < text.length) {
            val protectedWord = protectedWords.firstOrNull { text.startsWith(it, i) }
            if (protectedWord != null) {
                current.append(protectedWord)
                i += protectedWord.length
                continue
            }
            // 不要な言葉（「買っといて」の「と」など）は区切りの「と」より先に照合し、区切りとして扱う
            val delimiter = (FILLER_SUFFIXES + DELIMITER_WORDS).firstOrNull { text.startsWith(it, i) }
            when {
                delimiter != null -> {
                    segments += current.toString()
                    current.clear()
                    i += delimiter.length
                }
                text[i] in DELIMITER_CHARS || text[i].isWhitespace() -> {
                    segments += current.toString()
                    current.clear()
                    i++
                }
                else -> {
                    current.append(text[i])
                    i++
                }
            }
        }
        segments += current.toString()
        return segments.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun removeFillers(segment: String): String {
        var result = segment
        var changed = true
        while (changed) {
            changed = false
            FILLER_SUFFIXES.firstOrNull { result.length > it.length && result.endsWith(it) }?.let {
                result = result.dropLast(it.length).trimEnd()
                changed = true
            }
        }
        return result.trimEnd('を', 'も').trim()
    }

    private fun nfkc(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)

    private companion object {
        // 「それと」は「と」より先に照合する（長い語から）
        val DELIMITER_WORDS = listOf("それから", "それと", "あと", "と")
        val DELIMITER_CHARS = setOf('、', '。', ',', '.', '・')

        /** 品物の後ろに付く不要な言葉（長いものから照合する）。 */
        val FILLER_SUFFIXES =
            listOf(
                "を買わなきゃ", "買わなきゃ", "を買わないと", "を買っといて", "買っといて", "を買っておいて", "買っておいて",
                "を買ってきて", "買ってきて", "を買います", "を買う", "買う", "がほしい", "が欲しい", "ほしい", "欲しい",
                "をください", "ください", "がない", "がなくなった", "が切れた", "切れた", "かな",
            ).sortedByDescending { it.length }

        /** 既定の辞書の読みのうち「と」を含む品名（「とうふ」「なっとう」など）。 */
        val DEFAULT_PROTECTED: List<String> =
            (DefaultReadings.entries.values + listOf("とまと", "おとうふ", "ところてん", "とんかつ"))
                .filter { it.contains('と') }
    }
}
