package com.ikumemo.mobile.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val PANEL_CORNER = 12.dp
private val PANEL_SIDE = 8.dp
private val PANEL_OUTSIDE = 8.dp

/**
 * 面のどこを描くか。LazyColumn の行ごとに面を敷くとき、続けて 1 枚の面に見えるよう、
 * 先頭（上の角を丸める）・途中（角なし）・末尾（下の角を丸める）を分ける。1 つで完結するなら [SINGLE]。
 */
enum class PanelEdge {
    SINGLE,
    TOP,
    MIDDLE,
    BOTTOM,
    ;

    internal val roundsTop get() = this == SINGLE || this == TOP
    internal val roundsBottom get() = this == SINGLE || this == BOTTOM
}

/** [count] 件並べる行の [index] 番目の [PanelEdge]。 */
fun panelEdge(
    index: Int,
    count: Int,
): PanelEdge =
    when {
        count <= 1 -> PanelEdge.SINGLE
        index == 0 -> PanelEdge.TOP
        index == count - 1 -> PanelEdge.BOTTOM
        else -> PanelEdge.MIDDLE
    }

/**
 * 背景の写真（F-61）の上でも文字が読めるよう、[LocalHasBackgroundImage] が true のときだけ
 * テーマの surface を不透明に敷いて [content] を描く（BL-130）。面の中の文字色は onSurface に戻るため、
 * onSurfaceVariant・error・primary・CalendarColors もテーマの面の上と同じ見え方になる。
 * 写真が無いときは面を敷かず、[content] を縦に並べるだけにする（今の見た目を変えない）。
 */
@Composable
fun ReadablePanel(
    edge: PanelEdge = PanelEdge.SINGLE,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!LocalHasBackgroundImage.current) {
        Column(content = content)
        return
    }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = PANEL_SIDE,
                    end = PANEL_SIDE,
                    top = if (edge.roundsTop) PANEL_OUTSIDE else 0.dp,
                    bottom = if (edge.roundsBottom) PANEL_OUTSIDE else 0.dp,
                ).testTag(ReadablePanelTestTags.PANEL),
        shape = edge.shape(),
        color = MaterialTheme.colorScheme.surface,
        // surface と background が同じ色のテーマでも、面の中の文字はペンの色にせず onSurface にする（BL-170）
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(content = content)
    }
}

private fun PanelEdge.shape(): Shape =
    when (this) {
        PanelEdge.MIDDLE -> RectangleShape
        else ->
            RoundedCornerShape(
                topStart = if (roundsTop) PANEL_CORNER else 0.dp,
                topEnd = if (roundsTop) PANEL_CORNER else 0.dp,
                bottomStart = if (roundsBottom) PANEL_CORNER else 0.dp,
                bottomEnd = if (roundsBottom) PANEL_CORNER else 0.dp,
            )
    }

object ReadablePanelTestTags {
    /** 背景の写真の上に敷いた面。 */
    const val PANEL = "readable_panel"
}
