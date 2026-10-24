package com.ikumemo.core.reminder

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 未対応のおしらせ 1 件（期限、R-20）。 */
data class ReminderItem(
    val id: Long,
    val name: String,
    val dueDate: LocalDate,
    /** 「あとで」で決めた再通知の日（R-23）。期限は変えない。 */
    val snoozedUntil: LocalDate? = null,
)

/** おしらせの設定（PLAN §4.3。品目ごとには持たない）。 */
data class ReminderSettings(
    val time: LocalTime = DEFAULT_TIME,
    /** 期限当日にも知らせるか（R-20、初期値オン）。 */
    val notifyOnDueDate: Boolean = true,
) {
    companion object {
        val DEFAULT_TIME: LocalTime = LocalTime.of(9, 0)
    }
}

/** その日の通知での、期限との関係。 */
enum class DueKind {
    /** 期限があした（前日の通知）。 */
    TOMORROW,

    /** 期限がきょう。 */
    TODAY,

    /** 期限を過ぎている（期限当日に「あとで」を押した翌日）。 */
    PAST,
}

data class DueNotice(
    val item: ReminderItem,
    val kind: DueKind,
)

/**
 * 1 日 1 通にまとめた通知（R-21）。[lead] の品名と「ほか [othersCount] つ」で見出しを作る。
 * [kind] は全部が同じならその種類、混ざっていれば最も急ぐもの（期限切れ → きょう → あした）。
 */
data class DailyNotification(
    val date: LocalDate,
    val notices: List<DueNotice>,
) {
    val lead: ReminderItem get() = notices.first().item
    val othersCount: Int get() = notices.size - 1
    val kind: DueKind get() = notices.maxOf { it.kind }
    val mixed: Boolean get() = notices.map { it.kind }.distinct().size > 1

    /** 対象が 1 品目のときだけ通知のボタンを出す（R-27）。 */
    val hasActions: Boolean get() = notices.size == 1
}

/**
 * 通知日の計算（R-20〜R-23・R-28）。1 品目につき「期限の前日」と「期限当日」（設定でオフ可）、「あとで」を押したら
 * 翌日にもう一度。同じ日の分は 1 通にまとめる。過ぎた日の分は出さない（R-22、一覧に残すだけ）。
 */
object ReminderSchedule {
    /** [item] を知らせる日。 */
    fun notifyDates(
        item: ReminderItem,
        settings: ReminderSettings,
    ): Set<LocalDate> =
        buildSet {
            add(item.dueDate.minusDays(1))
            if (settings.notifyOnDueDate) add(item.dueDate)
            item.snoozedUntil?.let(::add)
        }

    /** [date] に出す通知。無ければ null。期限の近い順・品名順に並べる。 */
    fun notificationFor(
        date: LocalDate,
        items: List<ReminderItem>,
        settings: ReminderSettings,
    ): DailyNotification? {
        val notices =
            items
                .filter { date in notifyDates(it, settings) }
                .map { DueNotice(it, kindOn(date, it.dueDate)) }
                .sortedWith(compareBy<DueNotice> { it.item.dueDate }.thenBy { it.item.name })
        return if (notices.isEmpty()) null else DailyNotification(date, notices)
    }

    /**
     * 次に通知を出す日時。予約は「次の 1 件」だけにする（PLAN §4.1）。
     * きょうの通知時刻を過ぎていても、きょうの分をまだ出していなければすぐ出す（電源が切れていた場合、R-28）。
     * [lastNotifiedDate] は最後に通知を出した日。
     */
    fun nextNotificationAt(
        items: List<ReminderItem>,
        settings: ReminderSettings,
        now: LocalDateTime,
        lastNotifiedDate: LocalDate?,
    ): LocalDateTime? {
        val today = now.toLocalDate()
        return items
            .flatMap { notifyDates(it, settings) }
            .filter { it >= today }
            .distinct()
            .sorted()
            .firstNotNullOfOrNull { date ->
                when {
                    date > today -> date.atTime(settings.time)
                    lastNotifiedDate == today -> null
                    now.toLocalTime() < settings.time -> date.atTime(settings.time)
                    else -> now
                }
            }
    }

    private fun kindOn(
        date: LocalDate,
        dueDate: LocalDate,
    ): DueKind =
        when {
            date < dueDate -> DueKind.TOMORROW
            date == dueDate -> DueKind.TODAY
            else -> DueKind.PAST
        }
}

/** おしらせ一覧の並び（F-53）と期限切れの判定（R-22）。 */
object ReminderList {
    fun isOverdue(
        item: ReminderItem,
        today: LocalDate,
    ): Boolean = item.dueDate < today

    /** 期限が近いか（前日・当日・期限切れ。ウィジェットの「期限が近い N 件」、PLAN §2.11）。 */
    fun isNear(
        item: ReminderItem,
        today: LocalDate,
    ): Boolean = item.dueDate <= today.plusDays(1)

    fun sorted(items: List<ReminderItem>): List<ReminderItem> = items.sortedWith(compareBy({ it.dueDate }, { it.name }))
}
