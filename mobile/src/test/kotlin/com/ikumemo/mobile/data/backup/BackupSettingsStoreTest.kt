package com.ikumemo.mobile.data.backup

import com.ikumemo.mobile.data.AppSettings
import com.ikumemo.mobile.data.AppearancePrefs
import com.ikumemo.mobile.data.OperationPrefs
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.designsystem.SeedSwatch
import com.ikumemo.mobile.designsystem.ThemeChoice
import com.ikumemo.mobile.designsystem.ThemePreset
import com.ikumemo.mobile.testing.testDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** BL-180 の完了条件「Android 11 以前でおまかせを保存したまま書き出すと theme が wallpaper になる」。 */
@RunWith(RobolectricTestRunner::class)
class BackupSettingsStoreTest {
    private val dataStore = testDataStore()

    /** おまかせを選べない端末（Android 11 以前）の条件。 */
    private val appearance = AppearancePrefs(dataStore, wallpaperAvailable = false)

    private fun store() =
        BackupSettingsStore(
            AppSettings(dataStore),
            appearance,
            OperationPrefs(dataStore),
            ReminderPrefs(dataStore),
        )

    @Test
    fun wallpaperIsExportedAsSavedOnOlderDevices() =
        runTest {
            appearance.setTheme(ThemeChoice.Wallpaper)

            assertEquals(ThemeChoice.WALLPAPER_KEY, store().read().theme)
            // 画面に出す配色は従来どおり既定のテーマのまま
            assertEquals(ThemeChoice.DEFAULT, appearance.appearance.first().theme)
        }

    @Test
    fun presetIsExportedAsBefore() =
        runTest {
            appearance.setTheme(ThemePreset.COOL)

            assertEquals(ThemePreset.COOL.storageKey, store().read().theme)
        }

    @Test
    fun seedIsExportedAsBefore() =
        runTest {
            appearance.setTheme(SeedSwatch.GRAPE.choice)

            assertEquals(SeedSwatch.GRAPE.choice.storageKey, store().read().theme)
        }

    @Test
    fun nothingSavedExportsTheDefault() =
        runTest {
            assertEquals(ThemeChoice.DEFAULT.storageKey, store().read().theme)
        }
}
