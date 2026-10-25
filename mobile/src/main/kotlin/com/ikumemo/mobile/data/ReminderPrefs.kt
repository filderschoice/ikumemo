package com.ikumemo.mobile.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.ikumemo.core.reminder.ReminderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/**
 * おしらせの設定と記録（PLAN §4.3。品目ごとには持たない）。通知時刻・当日のおしらせのオン/オフ・最後に通知した日（R-28）。
 * [AppSettings] と同じ DataStore に保存する。
 */
class ReminderPrefs(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<ReminderSettings> =
        dataStore.data.map { prefs ->
            ReminderSettings(
                time =
                    prefs[TIME_MINUTES]?.let {
                        LocalTime.of(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR)
                    } ?: ReminderSettings.DEFAULT_TIME,
                notifyOnDueDate = prefs[NOTIFY_ON_DUE_DATE] ?: true,
            )
        }

    suspend fun currentSettings(): ReminderSettings = settings.first()

    suspend fun update(settings: ReminderSettings) {
        dataStore.edit {
            it[TIME_MINUTES] = settings.time.hour * MINUTES_PER_HOUR + settings.time.minute
            it[NOTIFY_ON_DUE_DATE] = settings.notifyOnDueDate
        }
    }

    suspend fun lastNotifiedDate(): LocalDate? = dataStore.data.first()[LAST_NOTIFIED]?.let(LocalDate::ofEpochDay)

    suspend fun setLastNotifiedDate(date: LocalDate) {
        dataStore.edit { it[LAST_NOTIFIED] = date.toEpochDay() }
    }

    /** 通知の許可を求めた回数（Android 13 以降で 2 回断られたら端末の設定を開く、R-29）。 */
    suspend fun permissionRequestCount(): Int = dataStore.data.first()[PERMISSION_REQUESTS] ?: 0

    suspend fun countPermissionRequest() {
        dataStore.edit { it[PERMISSION_REQUESTS] = (it[PERMISSION_REQUESTS] ?: 0) + 1 }
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
        val TIME_MINUTES = intPreferencesKey("reminder_time_minutes")
        val NOTIFY_ON_DUE_DATE = booleanPreferencesKey("reminder_notify_on_due_date")
        val LAST_NOTIFIED = longPreferencesKey("reminder_last_notified_epoch_day")
        val PERMISSION_REQUESTS = intPreferencesKey("reminder_permission_requests")
    }
}
