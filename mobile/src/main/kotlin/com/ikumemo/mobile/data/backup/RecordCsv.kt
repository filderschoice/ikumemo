package com.ikumemo.mobile.data.backup

import java.time.LocalDate

/** 表計算ソフトで開く「買った記録」の CSV（F-72）。読み込みには使わない。 */
object RecordCsv {
    private const val BOM = ""
    private const val NEWLINE = "\r\n"
    private val header = listOf("買った日", "品名", "数量", "お店", "メモ")

    /** UTF-8 の BOM 付き（Excel で文字化けしないように）、改行は CRLF。 */
    fun build(
        records: List<BackupRecord>,
        stores: List<BackupStore>,
    ): String {
        val storeNames = stores.associate { it.id to it.name }
        val rows =
            records.sortedWith(compareBy({ it.purchasedDate }, { it.id })).map { record ->
                listOf(
                    LocalDate.ofEpochDay(record.purchasedDate).toString(),
                    record.displayName,
                    record.quantity.orEmpty(),
                    record.storeId?.let(storeNames::get).orEmpty(),
                    record.note.orEmpty(),
                )
            }
        return BOM +
            (listOf(header) + rows).joinToString(NEWLINE, postfix = NEWLINE) {
                it.joinToString(",", transform = ::cell)
            }
    }

    /**
     * 1 つ分の欄。カンマ・引用符・改行を含むときは引用符で囲む。表計算ソフトが式として実行しないよう、
     * `=` `+` `-` `@` で始まる欄は先頭に `'` を足す。
     */
    internal fun cell(value: String): String {
        val safe = if (value.firstOrNull() in FORMULA_STARTS) "'$value" else value
        return if (safe.any { it in SPECIAL }) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else {
            safe
        }
    }

    private val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t')
    private val SPECIAL = setOf(',', '"', '\n', '\r')
}
