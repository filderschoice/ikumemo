package com.ikumemo.core.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderScheduleTest {
    private val settings = ReminderSettings()

    private fun item(
        id: Long,
        name: String,
        due: LocalDate,
        snoozed: LocalDate? = null,
    ) = ReminderItem(id, name, due, snoozed)

    @Test
    fun notifiesDayBeforeAndDueDate() {
        val milk = item(1, "牛乳", LocalDate.of(2026, 10, 10))

        assertEquals(
            setOf(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 10)),
            ReminderSchedule.notifyDates(milk, settings),
        )
    }

    @Test
    fun dueDateNotificationCanBeTurnedOff() {
        val milk = item(1, "牛乳", LocalDate.of(2026, 10, 10))

        assertEquals(
            setOf(LocalDate.of(2026, 10, 9)),
            ReminderSchedule.notifyDates(milk, settings.copy(notifyOnDueDate = false)),
        )
    }

    @Test
    fun dayBeforeCrossesMonthAndYearEnd() {
        fun dayBefore(due: LocalDate) = due.minusDays(1) in ReminderSchedule.notifyDates(item(1, "a", due), settings)

        assertTrue(dayBefore(LocalDate.of(2026, 11, 1)))
        assertTrue(dayBefore(LocalDate.of(2027, 1, 1)))
        assertTrue(dayBefore(LocalDate.of(2028, 3, 1)))
        assertEquals(LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 1).minusDays(1))
    }

    @Test
    fun sameDayItemsAreMergedIntoOneNotification() {
        val tomorrow = LocalDate.of(2026, 10, 10)
        val items =
            listOf(
                item(1, "牛乳", tomorrow),
                item(2, "たまご", tomorrow),
                item(3, "パン", tomorrow),
                item(4, "納豆", LocalDate.of(2026, 10, 20)),
            )

        val notification = requireNotNull(ReminderSchedule.notificationFor(LocalDate.of(2026, 10, 9), items, settings))

        assertEquals(3, notification.notices.size)
        assertEquals(2, notification.othersCount)
        assertEquals(DueKind.TOMORROW, notification.kind)
        assertFalse(notification.hasActions)
    }

    @Test
    fun singleItemNotificationHasActions() {
        val notification =
            requireNotNull(
                ReminderSchedule.notificationFor(
                    LocalDate.of(2026, 10, 10),
                    listOf(item(1, "牛乳", LocalDate.of(2026, 10, 10))),
                    settings,
                ),
            )

        assertEquals(DueKind.TODAY, notification.kind)
        assertTrue(notification.hasActions)
        assertEquals("牛乳", notification.lead.name)
    }

    @Test
    fun mixedKindsUseMostUrgent() {
        val date = LocalDate.of(2026, 10, 10)
        val items = listOf(item(1, "牛乳", date.plusDays(1)), item(2, "パン", date))

        val notification = requireNotNull(ReminderSchedule.notificationFor(date, items, settings))

        assertEquals(DueKind.TODAY, notification.kind)
        assertTrue(notification.mixed)
        assertEquals("パン", notification.lead.name)
    }

    @Test
    fun noNotificationOnDaysWithoutDue() {
        assertNull(
            ReminderSchedule.notificationFor(
                LocalDate.of(2026, 10, 1),
                listOf(item(1, "牛乳", LocalDate.of(2026, 10, 10))),
                settings,
            ),
        )
    }

    @Test
    fun snoozeNotifiesNextDayWithoutChangingDueDate() {
        val due = LocalDate.of(2026, 10, 10)
        val snoozed = item(1, "牛乳", due, snoozed = due.plusDays(1))

        val notification = requireNotNull(ReminderSchedule.notificationFor(due.plusDays(1), listOf(snoozed), settings))

        assertEquals(DueKind.PAST, notification.kind)
        assertEquals(due, notification.lead.dueDate)
    }

    @Test
    fun nextNotificationIsTodayAtSetTimeIfNotYetPassed() {
        val items = listOf(item(1, "牛乳", LocalDate.of(2026, 10, 11)))

        val next = ReminderSchedule.nextNotificationAt(items, settings, LocalDateTime.of(2026, 10, 10, 7, 0), null)

        assertEquals(LocalDateTime.of(2026, 10, 10, 9, 0), next)
    }

    @Test
    fun missedNotificationTodayIsShownImmediately() {
        val items = listOf(item(1, "牛乳", LocalDate.of(2026, 10, 11)))
        val now = LocalDateTime.of(2026, 10, 10, 12, 30)

        assertEquals(now, ReminderSchedule.nextNotificationAt(items, settings, now, LocalDate.of(2026, 10, 9)))
    }

    @Test
    fun afterTodaysNotificationNextIsFollowingDay() {
        val items = listOf(item(1, "牛乳", LocalDate.of(2026, 10, 11)))

        val next =
            ReminderSchedule.nextNotificationAt(
                items,
                settings,
                LocalDateTime.of(2026, 10, 10, 9, 0),
                LocalDate.of(2026, 10, 10),
            )

        assertEquals(LocalDateTime.of(2026, 10, 11, 9, 0), next)
    }

    @Test
    fun customTimeIsUsed() {
        val items = listOf(item(1, "牛乳", LocalDate.of(2026, 10, 20)))
        val evening = settings.copy(time = LocalTime.of(18, 30))

        assertEquals(
            LocalDateTime.of(2026, 10, 19, 18, 30),
            ReminderSchedule.nextNotificationAt(items, evening, LocalDateTime.of(2026, 10, 10, 9, 0), null),
        )
    }

    @Test
    fun pastDueItemsAreNotNotified() {
        val items = listOf(item(1, "牛乳", LocalDate.of(2026, 10, 5)))

        assertNull(ReminderSchedule.nextNotificationAt(items, settings, LocalDateTime.of(2026, 10, 10, 8, 0), null))
        assertTrue(ReminderList.isOverdue(items.single(), LocalDate.of(2026, 10, 10)))
    }

    @Test
    fun nearIncludesTomorrowTodayAndOverdue() {
        val today = LocalDate.of(2026, 10, 10)

        assertTrue(ReminderList.isNear(item(1, "a", today.plusDays(1)), today))
        assertTrue(ReminderList.isNear(item(1, "a", today.minusDays(3)), today))
        assertFalse(ReminderList.isNear(item(1, "a", today.plusDays(2)), today))
    }
}
