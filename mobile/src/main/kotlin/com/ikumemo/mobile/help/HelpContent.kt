package com.ikumemo.mobile.help

import android.content.res.AssetManager
import com.ikumemo.mobile.guide.GuideId

/** 使い方の目次の章（PLAN §2.12）。 */
data class HelpChapter(
    val title: String,
    val pages: List<HelpPageRef>,
)

/** 目次に並ぶページ。[id] はファイル名（`.md` を除く）。 */
data class HelpPageRef(
    val id: String,
    val title: String,
)

/** ページの中身の 1 かたまり。正本の Markdown は見出し・段落・箇条書きだけを使う。 */
sealed interface HelpBlock {
    data class Heading(
        val text: String,
    ) : HelpBlock

    data class Paragraph(
        val text: String,
    ) : HelpBlock

    data class Bullets(
        val items: List<String>,
    ) : HelpBlock
}

/**
 * 使い方の 1 ページ（H-01）。[tryScreen] は「やってみる」で開く画面、[guide] は「ガイドをもう一度見る」で出し直すガイド（H-02）。
 */
data class HelpPage(
    val id: String,
    val title: String,
    val blocks: List<HelpBlock>,
    val tryScreen: String? = null,
    val guide: GuideId? = null,
)

/**
 * 使い方の正本（`assets/help/` の Markdown）を読む。外部ライブラリを増やさないよう、見出し（`#` `##`）・段落・箇条書き（`- `）・
 * ページの書き込み（`<!-- try: 画面; guide: g02 -->`）だけを扱う。Web 版は scripts/make-user-guide.py が同じ正本から作る。
 */
object HelpParser {
    private val pageLink = Regex("""^- \[(.+)]\((.+)\.md\)$""")
    private val comment = Regex("""<!--(.*?)-->""", RegexOption.DOT_MATCHES_ALL)

    fun index(markdown: String): List<HelpChapter> {
        val chapters = mutableListOf<HelpChapter>()
        comment.replace(markdown, "").lines().map(String::trim).forEach { line ->
            when {
                line.startsWith("## ") -> chapters += HelpChapter(line.removePrefix("## "), emptyList())
                else ->
                    pageLink.matchEntire(line)?.let { match ->
                        val (title, id) = match.destructured
                        val last = chapters.removeLastOrNull() ?: HelpChapter("", emptyList())
                        chapters += last.copy(pages = last.pages + HelpPageRef(id, title))
                    }
            }
        }
        return chapters
    }

    fun page(
        id: String,
        markdown: String,
    ): HelpPage {
        val meta = comment.findAll(markdown).flatMap { it.groupValues[1].split(';') }.associate(::metaEntry)
        val lines = comment.replace(markdown, "").lines()
        val title = lines.firstOrNull { it.startsWith("# ") }?.removePrefix("# ")?.trim().orEmpty()
        return HelpPage(
            id = id,
            title = title,
            blocks = blocks(lines.filterNot { it.startsWith("# ") }),
            tryScreen = meta["try"],
            guide = meta["guide"]?.let(GuideId::fromStorageKey),
        )
    }

    private fun metaEntry(entry: String): Pair<String, String> {
        val key = entry.substringBefore(':').trim()
        return key to entry.substringAfter(':', "").trim()
    }

    /** 空行で区切ったかたまりごとに、見出し・箇条書き・段落へ分ける。 */
    private fun blocks(lines: List<String>): List<HelpBlock> {
        val result = mutableListOf<HelpBlock>()
        val chunk = mutableListOf<String>()

        fun flush() {
            if (chunk.isEmpty()) return
            result +=
                when {
                    chunk.first().startsWith("## ") -> HelpBlock.Heading(chunk.joinToString(" ").removePrefix("## "))
                    chunk.all { it.startsWith("- ") } -> HelpBlock.Bullets(chunk.map { it.removePrefix("- ") })
                    else -> HelpBlock.Paragraph(chunk.joinToString(""))
                }
            chunk.clear()
        }
        lines.map(String::trim).forEach { line -> if (line.isEmpty()) flush() else chunk += line }
        flush()
        return result
    }
}

/** 使い方の正本を assets から読む（通信なしで見られる、H-01）。 */
class HelpRepository(
    private val assets: AssetManager,
) {
    fun chapters(): List<HelpChapter> = HelpParser.index(read("index"))

    /** ページが無ければ null（目次と食い違ったときに落とさない）。 */
    fun page(id: String): HelpPage? = runCatching { HelpParser.page(id, read(id)) }.getOrNull()

    private fun read(name: String): String = assets.open("$DIR/$name.md").bufferedReader().use { it.readText() }

    private companion object {
        const val DIR = "help"
    }
}
