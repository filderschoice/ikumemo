package com.ikumemo.mobile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.mobile.data.Appearance
import com.ikumemo.mobile.data.AppearancePrefs
import com.ikumemo.mobile.designsystem.DarkMode
import com.ikumemo.mobile.designsystem.TextSizeSetting
import com.ikumemo.mobile.designsystem.ThemePreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
}

/** きせかえ（F-60・F-63・F-65）。選んだらすぐ保存し、アプリ全体の見た目へ反映する。 */
class AppearanceViewModel(
    private val prefs: AppearancePrefs,
) : ViewModel() {
    val appearance: StateFlow<Appearance> =
        prefs.appearance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Appearance())

    fun onEvent(event: AppearanceEvent) {
        viewModelScope.launch {
            when (event) {
                is AppearanceEvent.SelectTheme -> prefs.setTheme(event.theme)
                is AppearanceEvent.SelectDarkMode -> prefs.setDarkMode(event.mode)
                is AppearanceEvent.SelectTextSize -> prefs.setTextSize(event.size)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
