package com.ikumemo.mobile.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.ikumemo.mobile.data.Appearance
import com.ikumemo.mobile.data.AppearancePrefs
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.designsystem.BackgroundScrim
import com.ikumemo.mobile.designsystem.DarkMode
import com.ikumemo.mobile.designsystem.InkColor
import com.ikumemo.mobile.designsystem.TextSizeSetting
import com.ikumemo.mobile.designsystem.ThemeChoice
import com.ikumemo.mobile.designsystem.ThemePreset
import com.ikumemo.mobile.testing.MainDispatcherRule
import com.ikumemo.mobile.testing.TURBINE_TIMEOUT
import com.ikumemo.mobile.testing.testDataStore
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * BL-060 の完了条件「テーマ・ダークモード・文字サイズの変更が保存され、画面に反映される」と、
 * BL-061 の完了条件「背景画像の設定・解除・膜の濃さ変更が保存される」。背景画像は実際に JPEG を読み書きするためネイティブ描画で動かす。
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppearanceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dataStore = testDataStore()
    private val prefs = AppearancePrefs(dataStore)
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val backgroundStore = PhotoStore(context, PhotoStore.BACKGROUND_DIR)
    private lateinit var viewModel: AppearanceViewModel

    // Main の差し替え（MainDispatcherRule）の後に作る
    @Before
    fun setUp() {
        viewModel = AppearanceViewModel(prefs, backgroundStore)
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
    fun wallpaperThemeIsSavedAsItsOwnValue() =
        runTest {
            viewModel.appearance.test(timeout = TURBINE_TIMEOUT) {
                viewModel.onEvent(AppearanceEvent.SelectTheme(ThemeChoice.Wallpaper))
                awaitUntil { it.theme == ThemeChoice.Wallpaper }
                cancelAndIgnoreRemainingEvents()
            }
            // BL-176: 既存のキーに新しい値を足すだけで、プリセットの値の意味は変えない
            assertEquals("wallpaper", dataStore.data.first()[stringPreferencesKey("appearance_theme")])
            assertEquals(ThemeChoice.Wallpaper, AppearancePrefs(dataStore).appearance.first().theme)
        }

    @Test
    fun wallpaperReadsAsDefaultWhereDynamicColorIsUnavailable() =
        runTest {
            prefs.setTheme(ThemeChoice.Wallpaper)

            val older = AppearancePrefs(dataStore, wallpaperAvailable = false)

            assertEquals(ThemeChoice.DEFAULT, older.appearance.first().theme)
            // 保存値は書き換えない（端末を Android 12 以降へ上げたら、おまかせに戻る）
            assertEquals("wallpaper", dataStore.data.first()[stringPreferencesKey("appearance_theme")])
        }

    @Test
    fun savedPresetIsReadTheSameWhetherOrNotDynamicColorIsAvailable() =
        runTest {
            dataStore.edit { it[stringPreferencesKey("appearance_theme")] = "gentle" }

            listOf(true, false).forEach { available ->
                val theme = AppearancePrefs(dataStore, wallpaperAvailable = available).appearance.first().theme
                assertEquals(ThemePreset.GENTLE, theme)
            }
        }

    private fun jpeg(name: String): Uri {
        val file = File(context.cacheDir, "$name.jpg")
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(file)
    }

    /** 保存（別スレッドの書き込み）の後に消すため、実時間で少し待つ。消えたら true。 */
    private suspend fun awaitDeleted(file: File): Boolean {
        repeat(DELETE_WAIT_TRIES) {
            if (!file.exists()) return true
            Thread.sleep(DELETE_WAIT_MILLIS)
            yield()
        }
        return !file.exists()
    }

    @Test
    fun backgroundIsSavedReplacedAndRemoved() =
        runTest {
            viewModel.appearance.test(timeout = TURBINE_TIMEOUT) {
                viewModel.onEvent(AppearanceEvent.PickBackground(jpeg("first")))
                val first = requireNotNull(awaitUntil { it.backgroundImage != null }.backgroundImage)
                assertTrue(first.startsWith("background/"))
                assertTrue(backgroundStore.fileOf(first).exists())

                // 差し替えたら前の画像は消す
                viewModel.onEvent(AppearanceEvent.PickBackground(jpeg("second")))
                val second = requireNotNull(awaitUntil { it.backgroundImage != first }.backgroundImage)
                assertNotEquals(first, second)
                // 前の画像は新しい設定を保存した後に消す（表示中の画像を先に消さない）ので、消えるまで待つ
                assertTrue(awaitDeleted(backgroundStore.fileOf(first)))
                assertEquals(second, AppearancePrefs(dataStore).appearance.first().backgroundImage)

                viewModel.onEvent(AppearanceEvent.RemoveBackground)
                assertNull(awaitUntil { it.backgroundImage == null }.backgroundImage)
                assertTrue(awaitDeleted(backgroundStore.fileOf(second)))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun scrimLevelIsSavedWithinRange() =
        runTest {
            assertEquals(BackgroundScrim.DEFAULT, prefs.appearance.first().scrimLevel)
            viewModel.appearance.test(timeout = TURBINE_TIMEOUT) {
                viewModel.onEvent(AppearanceEvent.SetScrimLevel(0.8f))
                assertEquals(0.8f, awaitUntil { it.scrimLevel == 0.8f }.scrimLevel)
                viewModel.onEvent(AppearanceEvent.SetScrimLevel(5f))
                assertEquals(BackgroundScrim.MAX, awaitUntil { it.scrimLevel == BackgroundScrim.MAX }.scrimLevel)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(BackgroundScrim.MAX, AppearancePrefs(dataStore).appearance.first().scrimLevel)
        }

    @Test
    fun inkColorIsSavedAndDefaultsToAuto() =
        runTest {
            assertEquals(InkColor.AUTO, prefs.appearance.first().ink)
            viewModel.appearance.test(timeout = TURBINE_TIMEOUT) {
                viewModel.onEvent(AppearanceEvent.SelectInk(InkColor.WHITE))
                assertEquals(InkColor.WHITE, awaitUntil { it.ink == InkColor.WHITE }.ink)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(InkColor.WHITE, AppearancePrefs(dataStore).appearance.first().ink)
        }

    @Test
    fun scrimIsDarkerInDarkTheme() {
        assertEquals(0.6f, BackgroundScrim.effectiveAlpha(0.6f, darkTheme = false), 0.0001f)
        assertEquals(0.75f, BackgroundScrim.effectiveAlpha(0.6f, darkTheme = true), 0.0001f)
        // 暗い画面で足しても写真が少し見える濃さまで
        assertEquals(BackgroundScrim.MAX_EFFECTIVE, BackgroundScrim.effectiveAlpha(BackgroundScrim.MAX, true), 0.0001f)
        assertEquals(BackgroundScrim.MIN, BackgroundScrim.effectiveAlpha(0f, darkTheme = false), 0.0001f)
    }

    @Test
    fun darkModeFollowsSystemOnlyWhenSystem() {
        assertEquals(true, DarkMode.SYSTEM.isDark(systemDark = true))
        assertEquals(false, DarkMode.LIGHT.isDark(systemDark = true))
        assertEquals(true, DarkMode.DARK.isDark(systemDark = false))
    }

    private companion object {
        const val DELETE_WAIT_TRIES = 500
        const val DELETE_WAIT_MILLIS = 10L
    }
}
