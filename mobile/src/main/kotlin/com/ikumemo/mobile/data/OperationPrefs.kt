package com.ikumemo.mobile.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** そうさの設定（PLAN §3）。どちらも初期値はオン。 */
data class OperationSettings(
    /** メモの項目の左に ○（押すと「買った」）を出す（F-13）。 */
    val showBoughtButton: Boolean = true,
    /** スワイプがしきい値を超えた瞬間に軽く振動させる（R-05）。 */
    val vibration: Boolean = true,
)

/** そうさの設定の保存（[AppSettings] と同じ DataStore）。 */
class OperationPrefs(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<OperationSettings> =
        dataStore.data.map { prefs ->
            OperationSettings(
                showBoughtButton = prefs[SHOW_BOUGHT_BUTTON] ?: true,
                vibration = prefs[VIBRATION] ?: true,
            )
        }

    suspend fun setShowBoughtButton(show: Boolean) {
        dataStore.edit { it[SHOW_BOUGHT_BUTTON] = show }
    }

    suspend fun setVibration(enabled: Boolean) {
        dataStore.edit { it[VIBRATION] = enabled }
    }

    private companion object {
        val SHOW_BOUGHT_BUTTON = booleanPreferencesKey("operation_show_bought_button")
        val VIBRATION = booleanPreferencesKey("operation_vibration")
    }
}
