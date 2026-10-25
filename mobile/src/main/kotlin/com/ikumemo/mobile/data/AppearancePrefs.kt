package com.ikumemo.mobile.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ikumemo.mobile.designsystem.BackgroundScrim
import com.ikumemo.mobile.designsystem.DarkMode
import com.ikumemo.mobile.designsystem.TextSizeSetting
import com.ikumemo.mobile.designsystem.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 見た目の設定（F-60 テーマ、F-63 ダークモード、F-65 文字の大きさ、F-61 背景画像と膜の濃さ）。 */
data class Appearance(
    val theme: ThemePreset = ThemePreset.DEFAULT,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val textSize: TextSizeSetting = TextSizeSetting.NORMAL,
    /** 背景画像の相対パス（`background/<ファイル名>`）。null なら背景画像なし。 */
    val backgroundImage: String? = null,
    /** 膜の濃さ（[BackgroundScrim.MIN]〜[BackgroundScrim.MAX]）。 */
    val scrimLevel: Float = BackgroundScrim.DEFAULT,
)

/** 見た目の設定の保存（[AppSettings] と同じ DataStore）。各値は `storageKey` で保存し、未知の値は既定値へ戻す。 */
class AppearancePrefs(
    private val dataStore: DataStore<Preferences>,
) {
    val appearance: Flow<Appearance> =
        dataStore.data.map { prefs ->
            Appearance(
                theme = ThemePreset.fromStorageKey(prefs[THEME]),
                darkMode = DarkMode.fromStorageKey(prefs[DARK_MODE]),
                textSize = TextSizeSetting.fromStorageKey(prefs[TEXT_SIZE]),
                backgroundImage = prefs[BACKGROUND_IMAGE],
                scrimLevel = BackgroundScrim.clamp(prefs[SCRIM_LEVEL] ?: BackgroundScrim.DEFAULT),
            )
        }

    suspend fun setTheme(theme: ThemePreset) {
        dataStore.edit { it[THEME] = theme.storageKey }
    }

    suspend fun setDarkMode(mode: DarkMode) {
        dataStore.edit { it[DARK_MODE] = mode.storageKey }
    }

    suspend fun setTextSize(size: TextSizeSetting) {
        dataStore.edit { it[TEXT_SIZE] = size.storageKey }
    }

    /** 背景画像を設定する（null で外す）。 */
    suspend fun setBackgroundImage(path: String?) {
        dataStore.edit { if (path == null) it.remove(BACKGROUND_IMAGE) else it[BACKGROUND_IMAGE] = path }
    }

    suspend fun setScrimLevel(level: Float) {
        dataStore.edit { it[SCRIM_LEVEL] = BackgroundScrim.clamp(level) }
    }

    private companion object {
        val THEME = stringPreferencesKey("appearance_theme")
        val DARK_MODE = stringPreferencesKey("appearance_dark_mode")
        val TEXT_SIZE = stringPreferencesKey("appearance_text_size")
        val BACKGROUND_IMAGE = stringPreferencesKey("appearance_background_image")
        val SCRIM_LEVEL = floatPreferencesKey("appearance_scrim_level")
    }
}
