package com.ikumemo.mobile.data.backup

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** 外から来たファイルを読むときの大きさの上限つきコピー（F-72）。上限を超えたら [IOException] で止める。 */
internal object LimitedIo {
    private const val BUFFER_SIZE = 8192

    fun readText(
        input: InputStream,
        limit: Long,
    ): String {
        val buffer = ByteArrayOutputStream()
        copy(input, buffer, limit)
        return buffer.toString(Charsets.UTF_8.name())
    }

    fun copy(
        input: InputStream,
        output: OutputStream,
        limit: Long,
    ) {
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) return
            total += read
            if (total > limit) throw IOException("ファイルが大きすぎます")
            output.write(buffer, 0, read)
        }
    }
}
