package com.ikumemo.mobile.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.mobile.data.Appearance
import com.ikumemo.mobile.data.AppearancePrefs
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.designsystem.DarkMode
import com.ikumemo.mobile.designsystem.InkColor
import com.ikumemo.mobile.designsystem.TextSizeSetting
import com.ikumemo.mobile.designsystem.ThemePreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppearanceEvent {
    data class SelectTheme(
        val theme: ThemePreset,
    ) : AppearanceEvent

    data class SelectDarkMode(
        val mode: DarkMode,
    ) : AppearanceEvent

    data class SelectTextSize(
        val size: TextSizeSetting,
    ) : AppearanceEvent

    /** 写真を選んで背景にする（F-61）。 */
    data class PickBackground(
        val source: Uri,
    ) : AppearanceEvent

    data object RemoveBackground : AppearanceEvent

    /** 膜の濃さ（スライダーを離したときに保存する）。 */
    data class SetScrimLevel(
        val level: Float,
    ) : AppearanceEvent

    /** 背景の写真の上の文字の色（ペンの色）。 */
    data class SelectInk(
        val ink: InkColor,
    ) : AppearanceEvent
}

/**
 * きせかえ（F-60・F-61・F-63・F-65）。選んだらすぐ保存し、アプリ全体の見た目へ反映する。
 * 背景画像は [backgroundStore]（`filesDir/background/`）へ取り込み、差し替えたり外したりしたら前の画像を消す。
 */
class AppearanceViewModel(
    private val prefs: AppearancePrefs,
    private val backgroundStore: PhotoStore,
) : ViewModel() {
    val appearance: StateFlow<Appearance> =
        prefs.appearance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Appearance())

    fun onEvent(event: AppearanceEvent) {
        viewModelScope.launch {
            when (event) {
                is AppearanceEvent.SelectTheme -> prefs.setTheme(event.theme)
                is AppearanceEvent.SelectDarkMode -> prefs.setDarkMode(event.mode)
                is AppearanceEvent.SelectTextSize -> prefs.setTextSize(event.size)
                is AppearanceEvent.PickBackground -> backgroundStore.import(event.source)?.let { replaceBackground(it) }
                AppearanceEvent.RemoveBackground -> replaceBackground(null)
                is AppearanceEvent.SetScrimLevel -> prefs.setScrimLevel(event.level)
                is AppearanceEvent.SelectInk -> prefs.setInk(event.ink)
            }
        }
    }

    private suspend fun replaceBackground(path: String?) {
        val previous = prefs.appearance.first().backgroundImage
        prefs.setBackgroundImage(path)
        previous?.takeIf { it != path }?.let(backgroundStore::delete)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
