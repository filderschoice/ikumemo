package com.ikumemo.mobile.widget

import com.ikumemo.core.reminder.ReminderItem
import com.ikumemo.core.reminder.ReminderList
import com.ikumemo.mobile.AppContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** メモ一覧ウィジェットの 1 行（品名と数量）。 */
data class MemoWidgetItem(
    val id: Long,
    val name: String,
    val quantity: String?,
)

/** メモ一覧ウィジェット（W-02）に出す内容。並び順はアプリのメモと同じ。 */
data class MemoWidgetState(
    val items: List<MemoWidgetItem> = emptyList(),
    /** 期限が近い（前日・当日・期限切れ）おしらせの件数。0 件なら見出しに出さない。 */
    val dueSoonCount: Int = 0,
    /** 選んでいるいつものお店の名前。選んでいなければ null。 */
    val storeName: String? = null,
) {
    companion object {
        suspend fun load(container: AppContainer): MemoWidgetState = observe(container).first()

        /** 変わるたびに流す（お店の解除を 1 分ごとに見直すため、同じ内容は流さない）。 */
        fun observe(container: AppContainer): Flow<MemoWidgetState> =
            combine(
                container.shoppingRepository.observeItems(),
                container.reminderRepository.observePending(),
                container.storeRepository.observeSelectedStore(),
            ) { items, reminders, store ->
                val today = LocalDate.now(container.clock)
                MemoWidgetState(
                    items = items.map { MemoWidgetItem(it.id, it.displayName, it.quantity) },
                    dueSoonCount =
                        reminders.count {
                            ReminderList.isNear(
                                ReminderItem(it.reminder.id, it.displayName, it.reminder.dueDate),
                                today,
                            )
                        },
                    storeName = store?.name,
                )
            }.distinctUntilChanged()
    }
}
