package com.ikumemo.mobile.data.backup

import com.ikumemo.core.reminder.ReminderSettings
import com.ikumemo.mobile.data.AppSettings
import com.ikumemo.mobile.data.AppearancePrefs
import com.ikumemo.mobile.data.OperationPrefs
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.designsystem.BackgroundScrim
import com.ikumemo.mobile.designsystem.DarkMode
import com.ikumemo.mobile.designsystem.TextSizeSetting
import com.ikumemo.mobile.designsystem.ThemePreset
import kotlinx.coroutines.flow.first
import java.time.LocalTime

/**
 * 書き出しファイルに入れる設定（見た目・操作・おしらせ）の読み書き（F-72）。
 * ガイドの表示済みや選んでいるお店は端末ごとの状態なので入れず、読み込むときはお店の選択だけ外す。
 */
class BackupSettingsStore(
    private val appSettings: AppSettings,
    private val appearancePrefs: AppearancePrefs,
    private val operationPrefs: OperationPrefs,
    private val reminderPrefs: ReminderPrefs,
) {
    suspend fun read(): BackupSettings {
        val appearance = appearancePrefs.appearance.first()
        val operation = operationPrefs.settings.first()
        val reminder = reminderPrefs.currentSettings()
        return BackupSettings(
            theme = appearance.theme.storageKey,
            darkMode = appearance.darkMode.storageKey,
            textSize = appearance.textSize.storageKey,
            scrimLevel = appearance.scrimLevel,
            backgroundImage = appearance.backgroundImage,
            showBoughtButton = operation.showBoughtButton,
            vibration = operation.vibration,
            reminderTimeMinutes = reminder.time.hour * MINUTES_PER_HOUR + reminder.time.minute,
            notifyOnDueDate = reminder.notifyOnDueDate,
        )
    }

    /** [settings] の値を保存する。ファイルに無い項目は初期値にする。 */
    suspend fun apply(settings: BackupSettings) {
        appearancePrefs.setTheme(ThemePreset.fromStorageKey(settings.theme))
        appearancePrefs.setDarkMode(DarkMode.fromStorageKey(settings.darkMode))
        appearancePrefs.setTextSize(TextSizeSetting.fromStorageKey(settings.textSize))
        appearancePrefs.setScrimLevel(BackgroundScrim.clamp(settings.scrimLevel ?: BackgroundScrim.DEFAULT))
        appearancePrefs.setBackgroundImage(settings.backgroundImage)
        operationPrefs.setShowBoughtButton(settings.showBoughtButton ?: true)
        operationPrefs.setVibration(settings.vibration ?: true)
        val minutes = settings.reminderTimeMinutes
        reminderPrefs.update(
            ReminderSettings(
                time =
                    minutes?.let { LocalTime.of(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR) }
                        ?: ReminderSettings.DEFAULT_TIME,
                notifyOnDueDate = settings.notifyOnDueDate ?: true,
            ),
        )
        appSettings.clearStoreSelection()
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
