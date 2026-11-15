package com.ikumemo.mobile.widget

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ikumemo.mobile.IkumemoApplication
import com.ikumemo.mobile.MainActivity
import com.ikumemo.mobile.R
import kotlinx.coroutines.delay

/**
 * メモ一覧ウィジェット（W-02、4×2〜4×4、表示専用）。高さ 2 マスでは先頭の品名を横に数件、3 マス以上では
 * 選んだお店と品名の縦一覧を出す。追加・「買った」・編集はせず、押すとアプリの該当画面を開く（D-18・R-40）。
 */
class MemoListWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(NARROW_SMALL, SMALL, NARROW_LARGE, LARGE))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val container = (context.applicationContext as IkumemoApplication).container
        // 描き直しの要求で provideGlance は呼び直されないことがある（表示中のセッションが再利用される）。
        // 内容は最初に 1 回読むだけでなく、表示している間じゅう流れてくる最新の値を使う
        val firstColors = WidgetColors.load(context)
        val firstState = MemoWidgetState.load(container)
        provideContent {
            val colors by WidgetColors.observe(context).collectAsState(firstColors)
            val state by MemoWidgetState.observe(container).collectAsState(firstState)
            val refreshing by WidgetRefreshState.refreshing.collectAsState()
            MemoListContent(state, colors, refreshing)
        }
    }

    companion object {
        val NARROW_SMALL = DpSize(180.dp, 110.dp)
        val SMALL = DpSize(250.dp, 110.dp)
        val NARROW_LARGE = DpSize(180.dp, 180.dp)
        val LARGE = DpSize(250.dp, 180.dp)

        /** 小さい表示で横に並べる品名の数（幅が狭いときは減らす）。 */
        fun namesInRow(wide: Boolean): Int = if (wide) WIDE_NAMES else NARROW_NAMES

        private const val WIDE_NAMES = 3
        private const val NARROW_NAMES = 2
    }
}

@Composable
fun MemoListContent(
    state: MemoWidgetState,
    colors: WidgetColors,
    refreshing: Boolean = false,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val wide = size.width >= MemoListWidget.SMALL.width
    val large = size.height >= MemoListWidget.LARGE.height
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(colors.background)
                .cornerRadius(colors.corners.large.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clickable(actionStartActivity(MainActivity.intent(context, MainActivity.OPEN_MEMO))),
    ) {
        MemoHeader(state, colors, refreshing)
        // 見出しの下の太めの線（メモ帳の上端）
        Rule(colors.primary, HEADER_RULE)
        when {
            state.items.isEmpty() -> EmptyMemo(colors)
            large -> MemoColumn(state, colors, wide)
            else -> MemoRow(state, colors, wide)
        }
    }
}

/** 見出し（いくメモ・メモの件数・期限が近い件数）。期限が近い件数を押すとおしらせ画面。 */
@Composable
private fun MemoHeader(
    state: MemoWidgetState,
    colors: WidgetColors,
    refreshing: Boolean,
) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(TAP_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            context.getString(R.string.widget_memo_title),
            style = TextStyle(color = colors.onBackground, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            context.getString(R.string.widget_memo_count, state.items.size),
            style = TextStyle(color = colors.onBackground),
            maxLines = 1,
        )
        if (state.dueSoonCount > 0) {
            Box(
                // 幅が狭いときは、この見出しの文字だけを縮めて更新ボタンを残す（BL-186）
                modifier =
                    GlanceModifier
                        .defaultWeight()
                        .height(TAP_HEIGHT)
                        .padding(start = 8.dp)
                        .clickable(actionStartActivity(MainActivity.intent(context, MainActivity.TAB_REMINDER))),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    context.getString(R.string.widget_memo_due_soon, state.dueSoonCount),
                    style = TextStyle(color = colors.primary, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                )
            }
        }
        if (state.dueSoonCount <= 0) Spacer(GlanceModifier.defaultWeight())
        RefreshButton(colors, refreshing)
    }
}

/** 更新ボタン。メモを足した直後に反映されないときのため、押すとその場で描き直す。 */
@Composable
private fun RefreshButton(
    colors: WidgetColors,
    refreshing: Boolean,
) {
    val description = LocalContext.current.getString(R.string.widget_memo_refresh)
    Box(
        modifier =
            GlanceModifier
                .size(TAP_HEIGHT)
                .clickable(actionRunCallback<RefreshMemoAction>())
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (refreshing) {
            // 描き直している間は、矢印の代わりに回る表示を出す
            CircularProgressIndicator(modifier = GlanceModifier.size(REFRESH_ICON), color = colors.primary)
        } else {
            Image(
                ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.primary),
                modifier = GlanceModifier.size(REFRESH_ICON),
            )
        }
    }
}

/** 更新ボタンが押された。ウィジェット全体を描き直す（アプリが止まっていても動く）。 */
class RefreshMemoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        // 押した直後に回る表示を出し、描き直しが終わっても一瞬は見えるよう最低限の時間は回す
        WidgetRefreshState.set(true)
        try {
            MemoListWidget().update(context, glanceId)
            val started = SystemClock.elapsedRealtime()
            (context.applicationContext as IkumemoApplication).container.widgetUpdater.refresh()
            val rest = MIN_SPIN_MILLIS - (SystemClock.elapsedRealtime() - started)
            if (rest > 0) delay(rest)
        } finally {
            WidgetRefreshState.set(false)
        }
    }
}

@Composable
private fun EmptyMemo(colors: WidgetColors) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            LocalContext.current.getString(R.string.widget_memo_empty),
            style = TextStyle(color = colors.onSurfaceVariant),
        )
    }
}

/** 小さい表示: 先頭の品名を横に数件と「ほか N 件」。 */
@Composable
private fun MemoRow(
    state: MemoWidgetState,
    colors: WidgetColors,
    wide: Boolean,
) {
    val context = LocalContext.current
    val shown = state.items.take(MemoListWidget.namesInRow(wide))
    val names = shown.joinToString(NAME_SEPARATOR) { context.getString(R.string.widget_memo_bullet, it.label()) }
    val rest = state.items.size - shown.size
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            names,
            style = TextStyle(color = colors.onBackground),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (rest > 0) {
            Text(
                context.getString(R.string.widget_memo_more, rest),
                style = TextStyle(color = colors.onSurfaceVariant),
                maxLines = 1,
                modifier = GlanceModifier.padding(start = 8.dp),
            )
        }
    }
}

/** 大きい表示: 選んだお店と品名の縦一覧（数量付き、縦スクロール）。行を押すとその項目へ（R-40）。 */
@Composable
private fun MemoColumn(
    state: MemoWidgetState,
    colors: WidgetColors,
    wide: Boolean,
) {
    val context = LocalContext.current
    state.storeName?.let { store ->
        Text(
            context.getString(R.string.widget_memo_store, store),
            style = TextStyle(color = colors.onSurfaceVariant),
            maxLines = 1,
            modifier = GlanceModifier.padding(bottom = 4.dp),
        )
    }
    // 左の余白の縦線と、行ごとの横罫線（メモ帳風）
    Row(modifier = GlanceModifier.fillMaxSize()) {
        Box(modifier = GlanceModifier.width(MARGIN_RULE).fillMaxHeight().background(colors.rule)) {}
        LazyColumn(modifier = GlanceModifier.fillMaxSize().padding(start = 6.dp)) {
            items(state.items, itemId = { it.id }) { item ->
                // 1 項目につき根は 1 つにする（行と罫線をまとめる）
                Column(modifier = GlanceModifier.fillMaxWidth()) { MemoLine(item, colors, wide) }
            }
        }
    }
}

/** 横線 1 本（[thickness] の高さ）。 */
@Composable
private fun Rule(
    color: ColorProvider,
    thickness: Dp = LINE_RULE,
) {
    Box(modifier = GlanceModifier.fillMaxWidth().height(thickness).background(color)) {}
}

@Composable
private fun MemoLine(
    item: MemoWidgetItem,
    colors: WidgetColors,
    wide: Boolean,
) {
    val context = LocalContext.current
    val name = context.getString(R.string.widget_memo_bullet, item.name)
    val modifier =
        GlanceModifier
            .fillMaxWidth()
            .height(TAP_HEIGHT)
            .clickable(actionStartActivity(MainActivity.itemIntent(context, item.id)))
    val nameStyle = TextStyle(color = colors.onBackground)
    val quantityStyle = TextStyle(color = colors.onSurfaceVariant)
    if (wide || item.quantity == null) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = nameStyle, maxLines = 1, modifier = GlanceModifier.defaultWeight())
            item.quantity?.let { Text(it, style = quantityStyle, maxLines = 1) }
        }
    } else {
        // 幅が狭いときは数量を品名の下に回す（PLAN §2.11）
        Column(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = nameStyle, maxLines = 1)
            Text(item.quantity, style = quantityStyle, maxLines = 1, modifier = GlanceModifier.padding(start = 12.dp))
        }
    }
    Rule(colors.rule)
}

private fun MemoWidgetItem.label(): String = quantity?.let { "$name $it" } ?: name

/** タップできる部分の高さ（48dp 以上、PLAN §2.11）。 */
private val TAP_HEIGHT = 48.dp

private val REFRESH_ICON = 24.dp

/** 更新ボタンの回る表示を出す最低限の時間（ミリ秒）。描き直しが一瞬で終わっても回ったと分かるように。 */
private const val MIN_SPIN_MILLIS = 800L

/** メモ帳の線の太さ（見出しの下・行の下・左の余白）。 */
private val HEADER_RULE = 2.dp
private val LINE_RULE = 1.dp
private val MARGIN_RULE = 2.dp

private const val NAME_SEPARATOR = "　"

class MemoListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MemoListWidget()
}
