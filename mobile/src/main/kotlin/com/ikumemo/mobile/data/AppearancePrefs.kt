package com.ikumemo.mobile.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ikumemo.mobile.designsystem.BackgroundScrim
import com.ikumemo.mobile.designsystem.DarkMode
import com.ikumemo.mobile.designsystem.DynamicColor
import com.ikumemo.mobile.designsystem.InkColor
import com.ikumemo.mobile.designsystem.TextSizeSetting
import com.ikumemo.mobile.designsystem.ThemeChoice
import com.ikumemo.mobile.designsystem.availableOr
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 見た目の設定（F-60 テーマ、F-63 ダークモード、F-65 文字の大きさ、F-61 背景画像と膜の濃さ・ペンの色）。 */
data class Appearance(
    /** テーマ（プリセット 4 種またはおまかせ）。おまかせを選べない端末では読み込み時に既定のテーマへ置き換わる。 */
    val theme: ThemeChoice = ThemeChoice.DEFAULT,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val textSize: TextSizeSetting = TextSizeSetting.NORMAL,
    /** 背景画像の相対パス（`background/<ファイル名>`）。null なら背景画像なし。 */
    val backgroundImage: String? = null,
    /** 膜の濃さ（[BackgroundScrim.MIN]〜[BackgroundScrim.MAX]）。 */
    val scrimLevel: Float = BackgroundScrim.DEFAULT,
    /** 背景画像の上に直接置く文字の色（ペンの色）。背景画像が無いときは効かない。 */
    val ink: InkColor = InkColor.AUTO,
)

/**
 * 見た目の設定の保存（[AppSettings] と同じ DataStore）。各値は `storageKey` で保存し、未知の値は既定値へ戻す。
 * おまかせ（壁紙の色）は [wallpaperAvailable] が false の端末（Android 11 以前）では既定のテーマとして読む
 * （新しい端末で書き出したバックアップを読み込んだ場合など）。保存値そのものは書き換えない。
 */
class AppearancePrefs(
    private val dataStore: DataStore<Preferences>,
    private val wallpaperAvailable: Boolean = DynamicColor.isAvailable(),
) {
    val appearance: Flow<Appearance> =
        dataStore.data.map { prefs ->
            Appearance(
                theme =
                    ThemeChoice
                        .fromStorageKey(prefs[THEME])
                        .availableOr(ThemeChoice.DEFAULT, wallpaperAvailable),
                darkMode = DarkMode.fromStorageKey(prefs[DARK_MODE]),
                textSize = TextSizeSetting.fromStorageKey(prefs[TEXT_SIZE]),
                backgroundImage = prefs[BACKGROUND_IMAGE],
                scrimLevel = BackgroundScrim.clamp(prefs[SCRIM_LEVEL] ?: BackgroundScrim.DEFAULT),
                ink = InkColor.fromStorageKey(prefs[INK]),
            )
        }

    /**
     * 保存したままのテーマ（端末で選べるかどうかで置き換えない）。書き出し（F-72）に使い、
     * Android 11 以前の端末でおまかせを保存したまま書き出しても、Android 12 以降で読み込めばおまかせに戻るようにする（BL-180）。
     * 未知の値は [appearance] と同じく既定のテーマへ戻す。
     */
    val savedTheme: Flow<ThemeChoice> =
        dataStore.data.map { prefs -> ThemeChoice.fromStorageKey(prefs[THEME]) }

    suspend fun setTheme(theme: ThemeChoice) {
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

    suspend fun setInk(ink: InkColor) {
        dataStore.edit { it[INK] = ink.storageKey }
    }

    private companion object {
        val THEME = stringPreferencesKey("appearance_theme")
        val DARK_MODE = stringPreferencesKey("appearance_dark_mode")
        val TEXT_SIZE = stringPreferencesKey("appearance_text_size")
        val BACKGROUND_IMAGE = stringPreferencesKey("appearance_background_image")
        val SCRIM_LEVEL = floatPreferencesKey("appearance_scrim_level")
        val INK = stringPreferencesKey("appearance_ink")
    }
}
