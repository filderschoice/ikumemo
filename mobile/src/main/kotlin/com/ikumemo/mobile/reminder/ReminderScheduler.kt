package com.ikumemo.mobile.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ikumemo.core.reminder.ReminderItem
import com.ikumemo.core.reminder.ReminderSchedule
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.data.ReminderRepository
import java.time.Clock
import java.time.LocalDateTime

/**
 * 通知の予約（PLAN §4.1）。正確でないアラーム（`setAndAllowWhileIdle`）で「次の通知時刻」を 1 件だけ予約し、
 * 発火したら DB からその日の分を集めて 1 通にまとめて出す（R-21）。SCHEDULE_EXACT_ALARM は使わない。
 */
class ReminderScheduler(
    context: Context,
    private val repository: () -> ReminderRepository,
    private val prefs: ReminderPrefs,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    /** 次の通知を予約し直す（期限の変更・再起動・時刻変更・アプリ更新のたび、R-24）。予約した日時を返す。 */
    suspend fun reschedule(): LocalDateTime? {
        val next =
            ReminderSchedule.nextNotificationAt(
                items = items(),
                settings = prefs.currentSettings(),
                now = LocalDateTime.now(clock),
                lastNotifiedDate = prefs.lastNotifiedDate(),
            )
        alarmManager.cancel(alarmIntent())
        if (next != null) {
            val millis = next.atZone(clock.zone).toInstant().toEpochMilli()
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, alarmIntent())
        }
        return next
    }

    /**
     * きょうの分を、まだ出していなくて通知時刻を過ぎていれば出す（アラームの発火・起動時、R-28）。そのあと次を予約する。
     */
    suspend fun notifyIfDue(): Boolean {
        val now = LocalDateTime.now(clock)
        val today = now.toLocalDate()
        val settings = prefs.currentSettings()
        val shown =
            if (prefs.lastNotifiedDate() != today && now.toLocalTime() >= settings.time) {
                ReminderSchedule.notificationFor(today, items(), settings)?.let { notification ->
                    notifier.post(notification).also { posted -> if (posted) prefs.setLastNotifiedDate(today) }
                } ?: false
            } else {
                false
            }
        reschedule()
        return shown
    }

    private suspend fun items(): List<ReminderItem> =
        repository().pending().map {
            ReminderItem(it.reminder.id, it.displayName, it.reminder.dueDate, it.reminder.snoozedUntil)
        }

    private fun alarmIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            REQUEST_ALARM,
            Intent(appContext, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val REQUEST_ALARM = 1
    }
}
