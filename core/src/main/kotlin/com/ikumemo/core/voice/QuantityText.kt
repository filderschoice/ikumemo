package com.ikumemo.core.voice

/**
 * 手で入れた数量の表記をそろえる（数量の欄の保存時）。
 *
 * - 全角の数字は半角にする（「２本」→「2本」）
 * - 先頭の漢数字は、漢数字だけか、助数詞（[QuantityParser] と同じ一覧）が続くときだけ算用数字にする
 *   （「二本」→「2本」「三」→「3」「十個」→「10個」。「十分」「一部」「一杯」は助数詞でないため変えない）
 * - 単位（「本」「パック」「g」など）や、それ以外の文字はそのまま残す。空白だけなら null
 *
 * 音声から取り出した数量は [QuantityParser] が同じ形（算用数字＋助数詞）にそろえている。
 */
object QuantityText {
    fun normalize(raw: String?): String? =
        raw
            ?.trim()
            ?.map { it.toHalfWidthDigit() }
            ?.joinToString("")
            ?.ifEmpty { null }
            ?.let(::leadingKanjiToDigits)

    private fun leadingKanjiToDigits(text: String): String {
        val kanji = text.takeWhile { it in KanjiNumber.CHARS }
        val rest = text.drop(kanji.length)
        if (rest.isNotEmpty() && rest !in QuantityParser.counters) return text
        return KanjiNumber.parse(kanji)?.let { it.toString() + rest } ?: text
    }

    private fun Char.toHalfWidthDigit(): Char = if (this in '０'..'９') '0' + (this - '０') else this
}
