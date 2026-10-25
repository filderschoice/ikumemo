package com.ikumemo.mobile.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ikumemo.mobile.runAsync

/** 予約した通知時刻になった（アプリ内からだけ呼ぶため exported=false）。 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        runAsync(context) { it.reminderScheduler.notifyIfDue() }
    }
}

/** 端末の再起動・時刻変更・タイムゾーン変更・アプリ更新で予約し直す（R-24）。電源オフ中の分はここで補う（R-28）。 */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in HANDLED_ACTIONS) return
        runAsync(context) { it.reminderScheduler.notifyIfDue() }
    }

    private companion object {
        val HANDLED_ACTIONS =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
            )
    }
}

/** 通知のボタン（F-52）。「使い切った」「また買う」「あとで」。 */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L).takeIf { it > 0 } ?: return
        val action = intent.action ?: return
        runAsync(context) { container ->
            container.reminderActions.handle(action, reminderId)
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val USED_UP = "com.ikumemo.mobile.reminder.USED_UP"
        const val BUY_AGAIN = "com.ikumemo.mobile.reminder.BUY_AGAIN"
        const val LATER = "com.ikumemo.mobile.reminder.LATER"
    }
}
