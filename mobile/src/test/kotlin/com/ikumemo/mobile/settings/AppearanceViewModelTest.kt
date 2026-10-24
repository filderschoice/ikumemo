package com.ikumemo.mobile.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ikumemo.mobile.data.Appearance
import com.ikumemo.mobile.data.AppearancePrefs
import com.ikumemo.mobile.designsystem.DarkMode
import com.ikumemo.mobile.designsystem.TextSizeSetting
import com.ikumemo.mobile.designsystem.ThemePreset
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.TURBINE_TIMEOUT
import com.ikumemo.mobile.testing.testDataStore
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** BL-060 の完了条件「テーマ・ダークモード・文字サイズの変更が保存され、画面に反映される」。 */
@RunWith(RobolectricTestRunner::class)
class AppearanceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dataStore = testDataStore()
    private val prefs = AppearancePrefs(dataStore)
    private lateinit var viewModel: AppearanceViewModel

    // Main の差し替え（MainDispatcherRule）の後に作る
    @Before
    fun setUp() {
        viewModel = AppearanceViewModel(prefs)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
    }

    private suspend fun ReceiveTurbine<Appearance>.awaitUntil(predicate: (Appearance) -> Boolean): Appearance {
        while (true) {
            val value = awaitItem()
            if (predicate(value)) return value
        }
    }

    @Test
    fun defaultsArePopSystemNormal() =
        runTest {
            assertEquals(Appearance(ThemePreset.POP, DarkMode.SYSTEM, TextSizeSetting.NORMAL), prefs.appearance.first())
        }

    @Test
    fun selectionsAreSavedAndShown() =
        runTest {
            viewModel.appearance.test(timeout = TURBINE_TIMEOUT) {
                viewModel.onEvent(AppearanceEvent.SelectTheme(ThemePreset.COOL))
                viewModel.onEvent(AppearanceEvent.SelectDarkMode(DarkMode.DARK))
                viewModel.onEvent(AppearanceEvent.SelectTextSize(TextSizeSetting.EXTRA_LARGE))

                val expected = Appearance(ThemePreset.COOL, DarkMode.DARK, TextSizeSetting.EXTRA_LARGE)
                assertEquals(expected, awaitUntil { it == expected })
                cancelAndIgnoreRemainingEvents()
            }
            // 作り直しても（アプリを再起動しても）同じ見た目になる
            assertEquals(ThemePreset.COOL, AppearancePrefs(dataStore).appearance.first().theme)
        }

    @Test
    fun unknownSavedValuesFallBackToDefaults() =
        runTest {
            dataStore.edit {
                it[stringPreferencesKey("appearance_theme")] = "rainbow"
                it[stringPreferencesKey("appearance_dark_mode")] = "dim"
                it[stringPreferencesKey("appearance_text_size")] = "huge"
            }

            assertEquals(Appearance(), prefs.appearance.first())
        }

    @Test
    fun darkModeFollowsSystemOnlyWhenSystem() {
        assertEquals(true, DarkMode.SYSTEM.isDark(systemDark = true))
        assertEquals(false, DarkMode.LIGHT.isDark(systemDark = true))
        assertEquals(true, DarkMode.DARK.isDark(systemDark = false))
    }
}
