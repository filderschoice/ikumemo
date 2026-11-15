package com.ikumemo.mobile.share

import android.content.Context
import android.content.Intent
import com.ikumemo.core.share.SharedTextParser
import com.ikumemo.mobile.data.db.ShoppingItemEntity

/**
 * メモを文字にして送る（F-08・D-9、BL-196）。同期ではなく、その時点の内容を送るだけ。
 *
 * 送る文は「🛒いくメモ」の行の下に、1 行ずつ「・品物 数量」を並べる。取り込み側（BL-197、[SharedTextParser]）が
 * 先頭行 [HEADER] と行頭の [BULLET] を手がかりに元の品物と数量へ戻せる形で、数量は品物名との間に半角空白 1 つで続ける。
 */
object ShareText {
    /** 送る文の先頭行。取り込みでこの行があれば、いくメモから送られた文と見分ける。 */
    const val HEADER = SharedTextParser.HEADER

    /** 品物の行頭の印。 */
    const val BULLET = SharedTextParser.BULLET

    private val WHITESPACE = Regex("""\s+""")

    /** 送れる物があるか。空のメモでは共有シートを開かない。 */
    fun isShareable(items: List<ShoppingItemEntity>): Boolean = items.any { it.displayName.isNotBlank() }

    /** メモの並び順のまま送る文を組み立てる。品物の改行は空白に直し、1 品物を必ず 1 行にする。 */
    fun build(items: List<ShoppingItemEntity>): String =
        buildString {
            append(HEADER)
            for (item in items) {
                val name = oneLine(item.displayName)
                if (name.isEmpty()) continue
                append('\n').append(BULLET).append(name)
                oneLine(item.quantity.orEmpty()).takeIf { it.isNotEmpty() }?.let { append(' ').append(it) }
            }
        }

    /** 共有シート（Android 標準）を開く Intent。LINE の SDK や外部通信は使わない。 */
    fun chooserIntent(
        text: String,
        title: String,
    ): Intent {
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        return Intent.createChooser(send, title)
    }

    /** 共有シートを開く。開けない端末では false。 */
    fun launch(
        context: Context,
        text: String,
        title: String,
    ): Boolean =
        try {
            context.startActivity(chooserIntent(text, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: android.content.ActivityNotFoundException) {
            false
        }

    private fun oneLine(value: String): String = value.replace(WHITESPACE, " ").trim()
}
