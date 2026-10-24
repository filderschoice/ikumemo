package com.ikumemo.mobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * 選んでいるいつものお店（F-14）。お店を選んだ時か最後の「買った」から 3 時間たつか、日付が変わったら解除する（R-06）。
 */
data class StoreSelection(
    val storeId: Long,
    val selectedAt: Instant,
    val lastBoughtAt: Instant? = null,
) {
    fun isExpired(
        now: Instant,
        zone: ZoneId,
    ): Boolean {
        val base = lastBoughtAt?.takeIf { it.isAfter(selectedAt) } ?: selectedAt
        val dateChanged = base.atZone(zone).toLocalDate() != now.atZone(zone).toLocalDate()
        return dateChanged || Duration.between(base, now) >= EXPIRY
    }

    companion object {
        val EXPIRY: Duration = Duration.ofHours(3)
    }
}

/** 端末内の設定（DataStore Preferences、PLAN §4.1）。品目ごとには持たない値をここに置く。 */
class AppSettings(
    private val dataStore: DataStore<Preferences>,
) {
    val storeSelection: Flow<StoreSelection?> =
        dataStore.data.map { prefs ->
            val storeId = prefs[SELECTED_STORE_ID] ?: return@map null
            val selectedAt = prefs[SELECTED_STORE_AT] ?: return@map null
            StoreSelection(
                storeId = storeId,
                selectedAt = Instant.ofEpochMilli(selectedAt),
                lastBoughtAt = prefs[LAST_BOUGHT_AT]?.let(Instant::ofEpochMilli),
            )
        }

    suspend fun selectStore(
        storeId: Long,
        now: Instant,
    ) {
        dataStore.edit {
            it[SELECTED_STORE_ID] = storeId
            it[SELECTED_STORE_AT] = now.toEpochMilli()
            it.remove(LAST_BOUGHT_AT)
        }
    }

    suspend fun clearStoreSelection() {
        dataStore.edit {
            it.remove(SELECTED_STORE_ID)
            it.remove(SELECTED_STORE_AT)
            it.remove(LAST_BOUGHT_AT)
        }
    }

    /** 選んでいるお店で「買った」にした時刻を残す（R-06 の 3 時間はここから数え直す）。 */
    suspend fun recordBoughtAtSelectedStore(now: Instant) {
        dataStore.edit { if (it.contains(SELECTED_STORE_ID)) it[LAST_BOUGHT_AT] = now.toEpochMilli() }
    }

    companion object {
        private const val FILE_NAME = "ikumemo_settings"
        private val SELECTED_STORE_ID = longPreferencesKey("selected_store_id")
        private val SELECTED_STORE_AT = longPreferencesKey("selected_store_at")
        private val LAST_BOUGHT_AT = longPreferencesKey("last_bought_at")

        fun create(context: Context): AppSettings =
            AppSettings(
                PreferenceDataStoreFactory.create { context.applicationContext.preferencesDataStoreFile(FILE_NAME) },
            )
    }
}
