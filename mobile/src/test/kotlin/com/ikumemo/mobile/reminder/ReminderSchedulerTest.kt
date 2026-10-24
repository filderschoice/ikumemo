package com.ikumemo.mobile.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.core.reminder.ReminderSettings
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.data.ReminderRepository
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.UndoableAction
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.data.db.ReminderStatus
import com.ikumemo.mobile.testing.MutableClock
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import com.ikumemo.mobile.testing.testDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** BL-052 の完了条件「予約時刻の計算・再登録・通知ボタンの処理」を Robolectric で確かめる。 */
@RunWith(RobolectricTestRunner::class)
class ReminderSchedulerTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var db: IkumemoDatabase
    private lateinit var clock: MutableClock
    private lateinit var prefs: ReminderPrefs
    private lateinit var shopping: ShoppingRepository
    private lateinit var reminders: ReminderRepository
    private lateinit var notifier: ReminderNotifier
    private lateinit var scheduler: ReminderScheduler
    private val zone = ZoneId.of("Asia/Tokyo")
    private val today = LocalDate.of(2026, 10, 25)

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        db = inMemoryDatabase()
        clock = fixedClock("2026-10-24T22:00:00Z") // 日本時間 10 月 25 日 7:00
        prefs = ReminderPrefs(testDataStore())
        shopping = ShoppingRepository(db, ProductNameNormalizer(), clock)
        notifier = ReminderNotifier(app)
        reminders = ReminderRepository(db, clock) { scheduler.reschedule() }
        scheduler = ReminderScheduler(app, { reminders }, prefs, notifier, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun buyWithDue(
        name: String,
        due: LocalDate,
    ): Long {
        val itemId = (shopping.addItem(name) as AddItemResult.Added).itemId
        val recordId = (shopping.markBought(itemId) as UndoableAction.Bought).recordId
        reminders.setDue(recordId, due)
        return requireNotNull(db.reminderDao().findByRecord(recordId)).id
    }

    private fun scheduledAt(): LocalDateTime? =
        shadowOf(app.getSystemService(AlarmManager::class.java)).nextScheduledAlarm?.let {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it.triggerAtTime), zone)
        }

    private fun postedTitles(): List<String> =
        shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.map {
            it.extras.getCharSequence("android.title").toString()
        }

    @Test
    fun settingDueSchedulesDayBeforeAtNine() =
        runTest {
            buyWithDue("牛乳", today.plusDays(3))

            assertEquals(LocalDateTime.of(2026, 10, 27, 9, 0), scheduledAt())
        }

    @Test
    fun rescheduleUsesCustomTimeAndClearsWhenNothingLeft() =
        runTest {
            prefs.update(ReminderSettings(time = java.time.LocalTime.of(18, 30)))
            val id = buyWithDue("牛乳", today.plusDays(1))
            assertEquals(LocalDateTime.of(2026, 10, 25, 18, 30), scheduledAt())

            reminders.markUsedUp(id)

            assertNull(scheduler.reschedule())
        }

    @Test
    fun sameDayItemsArePostedAsOneNotificationWithoutButtons() =
        runTest {
            buyWithDue("牛乳", today.plusDays(1))
            buyWithDue("たまご", today.plusDays(1))
            buyWithDue("パン", today.plusDays(1))
            clock.now = Instant.parse("2026-10-25T00:00:00Z") // 9:00

            assertTrue(scheduler.notifyIfDue())

            val notification = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
            assertEquals("たまご ほか2つの期限があしたです", notification.extras.getCharSequence("android.title").toString())
            assertEquals(0, notification.actions?.size ?: 0)
            assertEquals(today, prefs.lastNotifiedDate())
            // きょうの分を出したので、次は期限当日の 9:00
            assertEquals(LocalDateTime.of(2026, 10, 26, 9, 0), scheduledAt())
        }

    @Test
    fun singleItemNotificationHasThreeButtons() =
        runTest {
            buyWithDue("牛乳", today)
            clock.now = Instant.parse("2026-10-25T00:30:00Z")

            scheduler.notifyIfDue()

            val notification = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.single()
            assertEquals("牛乳の期限がきょうです", notification.extras.getCharSequence("android.title").toString())
            assertEquals(listOf("使い切った", "また買う", "あとで"), notification.actions.map { it.title.toString() })
        }

    @Test
    fun missedNotificationIsShownOnNextStartOnlyOnce() =
        runTest {
            buyWithDue("牛乳", today.plusDays(1))
            clock.now = Instant.parse("2026-10-25T05:00:00Z") // 14:00（9:00 に電源が切れていた）

            assertTrue(scheduler.notifyIfDue())
            assertFalse(scheduler.notifyIfDue())

            assertEquals(1, postedTitles().size)
        }

    @Test
    fun beforeTimeNothingIsShown() =
        runTest {
            buyWithDue("牛乳", today.plusDays(1))

            assertFalse(scheduler.notifyIfDue())
            assertTrue(postedTitles().isEmpty())
        }

    @Test
    fun withoutPermissionNothingIsPostedButScheduleContinues() =
        runTest {
            shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
            buyWithDue("牛乳", today.plusDays(1))
            clock.now = Instant.parse("2026-10-25T00:00:00Z")

            assertFalse(scheduler.notifyIfDue())
            assertNull(prefs.lastNotifiedDate())
        }

    @Test
    fun buyAgainAddsToMemoAndMarksUsedUp() =
        runTest {
            val id = buyWithDue("牛乳", today)
            val actions = ReminderActions(reminders, shopping, notifier)

            actions.handle(ReminderActionReceiver.BUY_AGAIN, id)

            assertEquals(listOf("牛乳"), shopping.observeItems().first().map { it.displayName })
            assertEquals(ReminderStatus.USED_UP, db.reminderDao().getById(id)?.status)
        }

    @Test
    fun laterSnoozesToTomorrowAndUsedUpCloses() =
        runTest {
            val milk = buyWithDue("牛乳", today)
            val bread = buyWithDue("パン", today.plusDays(5))
            val actions = ReminderActions(reminders, shopping, notifier)

            actions.handle(ReminderActionReceiver.LATER, milk)
            actions.handle(ReminderActionReceiver.USED_UP, bread)

            val snoozed = requireNotNull(db.reminderDao().getById(milk))
            assertEquals(today.plusDays(1), snoozed.snoozedUntil)
            assertEquals(today, snoozed.dueDate)
            assertEquals(ReminderStatus.USED_UP, db.reminderDao().getById(bread)?.status)
            assertEquals(LocalDateTime.of(2026, 10, 25, 9, 0), scheduledAt())
        }

    @Test
    fun manifestUsesInexactAlarmAndHandlesSystemEvents() {
        val packageInfo = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = packageInfo.requestedPermissions.orEmpty().toList()

        assertFalse(Manifest.permission.SCHEDULE_EXACT_ALARM in permissions)
        assertFalse("android.permission.USE_EXACT_ALARM" in permissions)
        assertTrue(Manifest.permission.RECEIVE_BOOT_COMPLETED in permissions)
        listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
            .forEach { action ->
                val receivers =
                    app.packageManager.queryBroadcastReceivers(
                        Intent(action).setPackage(app.packageName),
                        0,
                    )
                assertTrue(action, receivers.any { it.activityInfo.name.endsWith("ReminderBootReceiver") })
            }
    }
}
