package com.ikumemo.mobile.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ikumemo.core.reminder.DailyNotification
import com.ikumemo.core.reminder.DueKind
import com.ikumemo.mobile.MainActivity
import com.ikumemo.mobile.R

/**
 * 期限の通知を出す（F-51・F-52）。通知の種類は「期限のおしらせ」の 1 つだけ（R-25）。
 * 対象が 1 品目のときだけ「使い切った」「また買う」「あとで」のボタンを出し、複数品目はタップでおしらせ一覧を開く（R-27）。
 * PendingIntent はすべて変更不可にする（PLAN §5）。
 */
class ReminderNotifier(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun ensureChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notification_channel_due),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = appContext.getString(R.string.notification_channel_due_description) }
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** 通知を出した場合 true。許可が無ければ出さない（期限の設定と一覧は使える、R-29）。 */
    fun post(notification: DailyNotification): Boolean {
        if (!canNotify()) return false
        ensureChannel()
        val builder =
            NotificationCompat
                .Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(titleOf(notification))
                .setContentText(appContext.getString(R.string.notification_due_body))
                .setContentIntent(openReminderTab())
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
        if (notification.hasActions) {
            val reminderId = notification.lead.id
            builder
                .addAction(
                    0,
                    appContext.getString(R.string.reminder_used_up),
                    action(ReminderActionReceiver.USED_UP, reminderId),
                )
                .addAction(
                    0,
                    appContext.getString(R.string.notification_buy_again),
                    action(ReminderActionReceiver.BUY_AGAIN, reminderId),
                )
                .addAction(
                    0,
                    appContext.getString(R.string.notification_later),
                    action(ReminderActionReceiver.LATER, reminderId),
                )
        }
        NotificationManagerCompat.from(appContext).notifySafely(builder)
        return true
    }

    fun cancel() {
        NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
    }

    private fun NotificationManagerCompat.notifySafely(builder: NotificationCompat.Builder) {
        if (canNotify()) {
            @Suppress("MissingPermission") // 直前の canNotify で確かめている
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun titleOf(notification: DailyNotification): String {
        val name = notification.lead.name
        val others = notification.othersCount
        val kind = notification.kind
        return when {
            others == 0 -> appContext.getString(singleTitle(kind), name)
            notification.mixed -> appContext.getString(R.string.notification_due_many_mixed, name, others)
            else -> appContext.getString(manyTitle(kind), name, others)
        }
    }

    private fun singleTitle(kind: DueKind): Int =
        when (kind) {
            DueKind.TOMORROW -> R.string.notification_due_tomorrow
            DueKind.TODAY -> R.string.notification_due_today
            DueKind.PAST -> R.string.notification_due_past
        }

    private fun manyTitle(kind: DueKind): Int =
        when (kind) {
            DueKind.TOMORROW -> R.string.notification_due_many_tomorrow
            DueKind.TODAY -> R.string.notification_due_many_today
            DueKind.PAST -> R.string.notification_due_many_past
        }

    private fun openReminderTab(): PendingIntent =
        PendingIntent.getActivity(
            appContext,
            REQUEST_OPEN,
            Intent(appContext, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_REMINDER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun action(
        name: String,
        reminderId: Long,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            name.hashCode(),
            Intent(appContext, ReminderActionReceiver::class.java)
                .setAction(name)
                .putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val CHANNEL_ID = "due"
        const val NOTIFICATION_ID = 1
        private const val REQUEST_OPEN = 10
    }
}
