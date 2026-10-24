package com.ikumemo.mobile

import android.content.Intent

/** 通知・ウィジェット・ショートカットから開く画面（[screen] は `MainActivity` の `TAB_*` / `OPEN_*`）。 */
data class ScreenRequest(
    val screen: String,
    val itemId: Long? = null,
) {
    companion object {
        fun from(intent: Intent): ScreenRequest? {
            val screen = intent.getStringExtra(MainActivity.EXTRA_OPEN_TAB) ?: return null
            val itemId = intent.getLongExtra(MainActivity.EXTRA_ITEM_ID, NO_ITEM).takeIf { it != NO_ITEM }
            return ScreenRequest(screen, itemId)
        }

        private const val NO_ITEM = -1L
    }
}
