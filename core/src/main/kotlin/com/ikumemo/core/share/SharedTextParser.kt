package com.ikumemo.core.share

import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.core.voice.NameAndQuantity
import com.ikumemo.core.voice.QuantityParser
import com.ikumemo.core.voice.VoiceTextParser

/**
 * LINE などから共有された文を品物の一覧に分ける（F-08・D-9、BL-197）。
 *
 * 受け取る文は信頼できない外部入力として扱う。長さ（[MAX_TEXT_LENGTH]）・行数（[MAX_LINES]）・品物の数（[MAX_ITEMS]）・
 * 品名と数量の長さに上限を設け、制御文字と文字の向きを変える書式文字を取り除く。どんな文でも例外を投げず、空の一覧を返すだけにする。
 *
 * - いくメモから送った文（先頭行が [HEADER]）は、行頭が [BULLET] の行を 1 行 1 品物として元の品物と数量に戻す
 *   （送る側は「・品物 数量」で、数量は最後の半角空白の後ろ）
 * - それ以外の行は、行頭の記号（・ - * 数字と点など）を外して、話して追加と同じ区切り・数量の取り出し（[VoiceTextParser]）で分ける
 * - 正規化した品名が同じ物は、最初の 1 つだけ残す
 *
 * @param knownNames 過去に入れた品名。品名の中の「と」を守るのに使う（[VoiceTextParser] と同じ）
 */
class SharedTextParser(
    knownNames: Collection<String> = emptyList(),
    private val normalizer: ProductNameNormalizer = ProductNameNormalizer(),
) {
    private val voiceParser = VoiceTextParser(knownNames, normalizer)

    fun parse(text: String?): List<NameAndQuantity> {
        val lines =
            sanitize(text.orEmpty().safeTake(MAX_TEXT_LENGTH))
                .split('\n')
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(MAX_LINES)
                .toList()
        val fromIkumemo = lines.firstOrNull() == HEADER
        val body = if (fromIkumemo) lines.drop(1) else lines
        val seen = mutableSetOf<String>()
        return body
            .asSequence()
            .flatMap { line ->
                if (fromIkumemo && line.startsWith(BULLET)) listOf(ikumemoLine(line)) else plainLine(line)
            }
            .mapNotNull(::limit)
            .filter { seen.add(normalizer.normalize(it.name)) }
            .take(MAX_ITEMS)
            .toList()
    }

    /** 「・品物 数量」。最後の空白の後ろが数量らしいときだけ数量にし、そうでなければ行全体を品名にする。 */
    private fun ikumemoLine(line: String): NameAndQuantity {
        val content = line.removePrefix(BULLET).trim()
        val space = content.lastIndexOf(' ')
        if (space <= 0) return NameAndQuantity(content, null)
        val tail = content.substring(space + 1)
        return if (looksLikeQuantity(tail)) {
            NameAndQuantity(content.substring(0, space).trim(), tail)
        } else {
            NameAndQuantity(content, null)
        }
    }

    private fun looksLikeQuantity(tail: String): Boolean =
        tail.any { it.isDigit() } || QuantityParser.split(PROBE + tail).let { it.name == PROBE && it.quantity != null }

    /** 普通の文の 1 行。行頭の記号を外し、品名と数量の間の空白（「牛乳 2本」）は詰めてから話して追加と同じ規則で分ける。 */
    private fun plainLine(line: String): List<NameAndQuantity> {
        var content = line
        while (true) {
            val mark = LEADING_MARK.find(content) ?: break
            content = content.substring(mark.range.last + 1)
        }
        // 「1.5L」の小数点で分けないよう、数字に挟まれた点は一時的に別の文字にしてから分ける
        val prepared = content.replace(SPACE_BEFORE_DIGIT, "").replace(DECIMAL_POINT, DECIMAL_PLACEHOLDER.toString())
        return voiceParser.parse(prepared).map { item ->
            val joined = item.name + item.quantity.orEmpty()
            // 小数の途中で品名と数量に分かれていることがあるため、元に戻してから数量を取り出し直す
            if (DECIMAL_PLACEHOLDER in joined) QuantityParser.split(joined.replace(DECIMAL_PLACEHOLDER, '.')) else item
        }
    }

    private fun limit(item: NameAndQuantity): NameAndQuantity? {
        val name = item.name.trim().safeTake(MAX_NAME_LENGTH).trim()
        if (name.isEmpty()) return null
        val quantity = item.quantity?.trim()?.safeTake(MAX_QUANTITY_LENGTH)?.trim()?.ifEmpty { null }
        return NameAndQuantity(name, quantity)
    }

    companion object {
        /** いくメモから送った文の先頭行（送る側の `ShareText.HEADER` と同じ）。 */
        const val HEADER = "🛒いくメモ"

        /** いくメモから送った文の品物の行頭の印（送る側の `ShareText.BULLET` と同じ）。 */
        const val BULLET = "・"

        /** 受け取る文の長さの上限（文字数）。超えた分は読まない。 */
        const val MAX_TEXT_LENGTH = 10_000

        /** 読む行数の上限（空行を除く）。 */
        const val MAX_LINES = 200

        /** 取り出す品物の数の上限。 */
        const val MAX_ITEMS = 100

        /** 品名の長さの上限（文字数）。超えた分は切り捨てる。 */
        const val MAX_NAME_LENGTH = 60

        /** 数量の長さの上限（文字数）。 */
        const val MAX_QUANTITY_LENGTH = 20

        private const val PROBE = "品"

        /** 行頭の記号。箇条書きの印・チェック欄・「1.」「(1)」「①」など。「1.5L」のように数字が続くときは外さない。 */
        private val LEADING_MARK =
            Regex(
                listOf(
                    """[・･•●○◯◆◇■□▪▫☐☑✓✔\-－ー*＊+＋]""",
                    """\[[ xX]?]""",
                    """[0-9０-９]{1,3}[.)、．）:：](?![0-9０-９])""",
                    """[(（][0-9０-９]{1,3}[)）]""",
                    """[①-⑳]""",
                ).joinToString("|", prefix = """^\s*(?:""", postfix = """)\s*"""),
            )

        private val SPACE_BEFORE_DIGIT = Regex("""[ \u3000]+(?=[0-9０-９])""")

        private val DECIMAL_POINT = Regex("""(?<=[0-9０-９])[.．](?=[0-9０-９])""")

        /** 私用領域の文字（共有された文の中の同じ文字は [sanitize] で除く）。 */
        private const val DECIMAL_PLACEHOLDER = '\uE000'

        /** 改行の仲間（CR・NEL・行区切り・段落区切り）を \n にそろえ、ほかの制御文字と向きを変える書式文字を除く。 */
        internal fun sanitize(text: String): String {
            val unified = text.replace("\r\n", "\n").replace(LINE_BREAKS, "\n")
            return buildString(unified.length) {
                for (c in unified) {
                    when {
                        c == '\n' -> append(c)
                        c == '\t' -> append(' ')
                        Character.isISOControl(c) || c in BIDI_AND_INVISIBLE -> Unit
                        else -> append(c)
                    }
                }
            }
        }

        private val LINE_BREAKS = Regex("[\r\u0085\u2028\u2029]")

        /** 文字の向きを変える書式文字（見た目の偽装に使われる）・BOM・小数点の一時置き換えに使う文字。絵文字の結合（ZWJ）は残す。 */
        private val BIDI_AND_INVISIBLE: Set<Char> =
            (
                "\u200E\u200F\u061C\uFEFF$DECIMAL_PLACEHOLDER" +
                    ('\u202A'..'\u202E').joinToString("") +
                    ('\u2066'..'\u2069').joinToString("")
            ).toSet()

        /** 先頭から [max] 文字まで。サロゲートペア（絵文字など）の途中では切らない。 */
        private fun String.safeTake(max: Int): String {
            if (length <= max) return this
            val end = if (Character.isHighSurrogate(this[max - 1])) max - 1 else max
            return substring(0, end)
        }
    }
}
