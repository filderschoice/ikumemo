package com.ikumemo.core.voice

/** 品名と数量に分けた結果。数量が無ければ [quantity] は null。 */
data class NameAndQuantity(
    val name: String,
    val quantity: String?,
)

/**
 * 品名の後ろに付いた数量を取り出す（F-02、PLAN §4.4）。
 *
 * - 算用数字・漢数字＋助数詞（「2本」「二本」「1パック」「500g」）
 * - 読みの数え方（「いっこ」「ひとつ」「にほん」）
 * - 誤変換の補正（「牛乳日本」→ 牛乳 2本）
 *
 * 数量は「算用数字＋助数詞」にそろえる（「いっこ」「1個」「一個」はすべて「1個」）。分けられない場合は全体を品名にする。
 */
object QuantityParser {
    // 長い表記から照合する（「パック」と「パ」などの取り違えを防ぐ）
    private val counters =
        listOf(
            "リットル", "ミリリットル", "キログラム", "グラム", "キロ", "パック", "ロール", "ふくろ", "セット",
            "kg", "ml", "本", "個", "袋", "箱", "枚", "缶", "束", "玉", "尾", "房", "株", "丁", "斤", "足", "切れ",
            "つ", "こ", "g", "l", "L", "まい", "はこ", "かん", "たま", "ほん", "ぽん", "ぼん", "そく",
        ).sortedByDescending { it.length }

    /** 読みの数量（数字の読み＋助数詞）。音声認識がひらがなで返すことがある。 */
    internal val spokenQuantities: List<Pair<String, String>> =
        listOf(
            "ひとつ" to "1つ", "ふたつ" to "2つ", "みっつ" to "3つ", "よっつ" to "4つ", "いつつ" to "5つ",
            "むっつ" to "6つ", "ななつ" to "7つ", "やっつ" to "8つ", "ここのつ" to "9つ",
            "いっこ" to "1個", "にこ" to "2個", "さんこ" to "3個", "よんこ" to "4個", "ごこ" to "5個",
            "ろっこ" to "6個", "じゅっこ" to "10個",
            "いっぽん" to "1本", "にほん" to "2本", "さんぼん" to "3本", "よんほん" to "4本", "ごほん" to "5本",
            "ろっぽん" to "6本",
            "ひとふくろ" to "1袋", "ふたふくろ" to "2袋", "ひとはこ" to "1箱", "ふたはこ" to "2箱",
            "いちまい" to "1枚", "にまい" to "2枚", "さんまい" to "3枚",
            "ひとパック" to "1パック", "ふたパック" to "2パック",
        ).sortedByDescending { it.first.length }

    /** 数字の読みが品名の後ろで別の語に変換されたもの（「牛乳日本」）。 */
    private val misrecognized: List<Pair<String, String>> =
        listOf(
            "日本" to "2本",
            "二本" to "2本",
            "一個" to "1個",
            "二個" to "2個",
            "一本" to "1本",
            "三本" to "3本",
        )

    private val counterAliases =
        mapOf(
            "こ" to "個", "ほん" to "本", "ぽん" to "本", "ぼん" to "本", "まい" to "枚", "はこ" to "箱", "ふくろ" to "袋",
            "かん" to "缶", "たま" to "玉", "そく" to "束", "キロ" to "kg", "キログラム" to "kg", "グラム" to "g",
            "リットル" to "L", "l" to "L", "ミリリットル" to "ml",
        )

    fun split(text: String): NameAndQuantity {
        val trimmed = text.trim()
        return splitNumeric(trimmed) ?: splitSpoken(trimmed) ?: NameAndQuantity(trimmed, null)
    }

    private fun splitSpoken(text: String): NameAndQuantity? =
        (spokenQuantities + misrecognized)
            .firstOrNull { (spoken, _) -> text.length > spoken.length && text.endsWith(spoken) }
            ?.let { (spoken, quantity) -> NameAndQuantity(text.dropLast(spoken.length).trim(), quantity) }

    /**
     * 末尾の「数字＋助数詞」（数字は算用数字または漢数字）。ひらがなの助数詞（「こ」「つ」等）は品名の末尾と紛れやすいため、
     * 数字が付いているときだけ数量として扱う。
     */
    private fun splitNumeric(text: String): NameAndQuantity? {
        val counter = counters.firstOrNull { text.endsWith(it) } ?: return null
        val beforeCounter = text.dropLast(counter.length)
        val digits = beforeCounter.takeLastWhile { it.isAsciiDigit() || it == '.' }
        val kanji = beforeCounter.takeLastWhile { it in KanjiNumber.CHARS }
        val number = digits.ifEmpty { KanjiNumber.parse(kanji)?.toString() }
        val numberLength = if (digits.isNotEmpty()) digits.length else kanji.length
        val name = beforeCounter.dropLast(numberLength).trim()
        return if (number != null && name.isNotEmpty()) {
            NameAndQuantity(name, number + (counterAliases[counter] ?: counter))
        } else {
            null
        }
    }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
}

/** 漢数字（〜999）を数値にする。 */
internal object KanjiNumber {
    const val CHARS = "〇一二三四五六七八九十百"

    private val digits = "〇一二三四五六七八九".withIndex().associate { (value, c) -> c to value }

    fun parse(text: String): Int? {
        if (text.isEmpty() || text.any { it !in CHARS }) return null
        var total = 0
        var current = 0
        for (c in text) {
            when (c) {
                '百' -> {
                    total += (if (current == 0) 1 else current) * 100
                    current = 0
                }
                '十' -> {
                    total += (if (current == 0) 1 else current) * 10
                    current = 0
                }
                else -> current = current * 10 + digits.getValue(c)
            }
        }
        return total + current
    }
}
