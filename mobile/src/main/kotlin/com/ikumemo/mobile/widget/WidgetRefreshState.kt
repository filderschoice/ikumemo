package com.ikumemo.mobile.widget

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * メモ一覧ウィジェットの更新ボタンを押してから描き直しが終わるまでの間 true（回る表示を出すため）。
 * ウィジェットの表示は同じプロセスの中で描き直すため、プロセスの中で 1 つ持てば足りる。
 */
object WidgetRefreshState {
    private val mutableRefreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = mutableRefreshing

    fun set(value: Boolean) {
        mutableRefreshing.value = value
    }
}
