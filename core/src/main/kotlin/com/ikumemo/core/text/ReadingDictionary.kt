package com.ikumemo.core.text

/**
 * 漢字表記と読み（ひらがな）の小規模辞書（PLAN §4.4）。日常の買い物でよく出る品名から始める自作の辞書で、
 * 既存の辞書データや他社の商品リストは写さない。利用者が入れた品名は DB の品目（正規化名）として残るため、
 * ここへ追加する必要はない。
 */
class ReadingDictionary(
    private val readings: Map<String, String>,
) {
    // 長い語から先に置き換える（「牛乳」より「低脂肪乳」、「肉」より「豚肉」を優先）
    private val keysLongestFirst = readings.keys.sortedByDescending { it.length }

    fun replaceWithReadings(text: String): String {
        if (text.none { it.isKanjiLike() }) return text
        val result = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val key = keysLongestFirst.firstOrNull { text.startsWith(it, i) }
            if (key != null) {
                result.append(readings.getValue(key))
                i += key.length
            } else {
                result.append(text[i])
                i++
            }
        }
        return result.toString()
    }

    private fun Char.isKanjiLike(): Boolean = Character.UnicodeScript.of(code) == Character.UnicodeScript.HAN

    companion object {
        val DEFAULT = ReadingDictionary(DefaultReadings.entries)
    }
}
