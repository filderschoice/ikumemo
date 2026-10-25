package com.ikumemo.mobile.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

/** 表計算用 CSV（F-72）。引用符・改行・式として実行される先頭文字の扱い。 */
class RecordCsvTest {
    @Test
    fun plainFieldsAreNotQuoted() {
        assertEquals("牛乳", RecordCsv.cell("牛乳"))
        assertEquals("", RecordCsv.cell(""))
    }

    @Test
    fun commaQuoteAndNewlineAreQuoted() {
        assertEquals("\"a,b\"", RecordCsv.cell("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", RecordCsv.cell("say \"hi\""))
        assertEquals("\"1\n2\"", RecordCsv.cell("1\n2"))
    }

    @Test
    fun formulaStartsAreDefused() {
        assertEquals("'=SUM(A1)", RecordCsv.cell("=SUM(A1)"))
        assertEquals("'+1", RecordCsv.cell("+1"))
        assertEquals("'@x", RecordCsv.cell("@x"))
    }

    @Test
    fun buildsHeaderAndRowsInDateOrder() {
        val stores = listOf(BackupStore(id = 1, name = "店", createdAt = 0))
        val records =
            listOf(
                BackupRecord(
                    id = 2,
                    tripId = 1,
                    productId = 1,
                    displayName = "後",
                    purchasedDate = 20_001,
                    recordedAt = 0,
                ),
                BackupRecord(
                    id = 1, tripId = 1, productId = 1, displayName = "先", quantity = "2本", purchasedDate = 20_000,
                    recordedAt = 0, storeId = 1, note = "a,b",
                ),
            )

        val csv = RecordCsv.build(records, stores)

        assertEquals(
            "買った日,品名,数量,お店,メモ\r\n2024-10-04,先,2本,店,\"a,b\"\r\n2024-10-05,後,,,\r\n",
            csv,
        )
    }
}
