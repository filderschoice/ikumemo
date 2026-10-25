package com.ikumemo.mobile.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/** アプリから置けるウィジェット（W-03）。メモ一覧は 4×4 で置かれ、置いた後に長押しで 4×2 などへ小さくできる。 */
enum class PinnableWidget(
    internal val receiver: Class<*>,
) {
    VOICE(VoiceWidgetReceiver::class.java),
    MEMO_LIST(MemoListWidgetReceiver::class.java),
}

/**
 * アプリからホーム画面へウィジェットを置く（W-03、`requestPinAppWidget`）。ホーム画面アプリが対応していない場合は
 * [isSupported] が false になり、画面では置き方の説明を出す。置く大きさは指定できないため、各ウィジェットの既定の大きさで置かれる。
 */
class WidgetPinner(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val manager get() = AppWidgetManager.getInstance(appContext)

    val isSupported: Boolean get() = manager.isRequestPinAppWidgetSupported

    /** 追加の確認をホーム画面アプリへ求める。求められたら true。 */
    fun pin(widget: PinnableWidget): Boolean =
        isSupported && manager.requestPinAppWidget(ComponentName(appContext, widget.receiver), null, null)
}
