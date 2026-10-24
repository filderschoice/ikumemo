package com.ikumemo.mobile.reminder

import com.ikumemo.mobile.data.ReminderRepository
import com.ikumemo.mobile.data.ShoppingRepository

/** 通知のボタンの処理（F-52・R-23・R-26）。押したら通知を閉じる。 */
class ReminderActions(
    private val reminders: ReminderRepository,
    private val shopping: ShoppingRepository,
    private val notifier: ReminderNotifier,
) {
    suspend fun handle(
        action: String,
        reminderId: Long,
    ) {
        when (action) {
            ReminderActionReceiver.USED_UP -> reminders.markUsedUp(reminderId)
            // また買う: メモに入れて（R-01 の重複判定あり）、そのおしらせは「使い切った」として片付ける（R-26）
            ReminderActionReceiver.BUY_AGAIN -> {
                reminders.getRow(reminderId)?.let { shopping.addItem(it.displayName) }
                reminders.markUsedUp(reminderId)
            }
            ReminderActionReceiver.LATER -> reminders.snooze(reminderId)
            else -> return
        }
        notifier.cancel()
    }
}
