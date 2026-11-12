package com.ikumemo.mobile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.AppSettings
import com.ikumemo.mobile.data.AppearancePrefs
import com.ikumemo.mobile.data.DataEraser
import com.ikumemo.mobile.data.GuidePrefs
import com.ikumemo.mobile.data.OperationPrefs
import com.ikumemo.mobile.data.PhotoCleaner
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.RecordRepository
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.data.ReminderRepository
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.backup.BackupExporter
import com.ikumemo.mobile.data.backup.BackupImporter
import com.ikumemo.mobile.data.backup.BackupSettingsStore
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.reminder.ReminderActions
import com.ikumemo.mobile.reminder.ReminderNotifier
import com.ikumemo.mobile.reminder.ReminderScheduler
import com.ikumemo.mobile.voice.AndroidSpeechEngine
import com.ikumemo.mobile.voice.SpeechEngine
import com.ikumemo.mobile.widget.WidgetUpdater
import java.time.Clock

/**
 * 手動 DI の入れ物（PLAN §4.1）。[IkumemoApplication] が 1 つだけ持ち、ViewModel・Worker・受信処理へ渡す。
 * テストでは DB・設定ファイル・音声認識・時計を差し替えて作る。
 */
class AppContainer(
    context: Context,
    val clock: Clock = Clock.systemDefaultZone(),
    databaseFactory: (Context) -> IkumemoDatabase = IkumemoDatabase::create,
    dataStoreFactory: (Context) -> DataStore<Preferences> = AppSettings::createDataStore,
    private val speechEngine: (Context) -> SpeechEngine = ::AndroidSpeechEngine,
) {
    private val appContext = context.applicationContext

    val database: IkumemoDatabase by lazy { databaseFactory(appContext) }

    val normalizer = ProductNameNormalizer()

    val shoppingRepository by lazy { ShoppingRepository(database, normalizer, clock) }

    val recordRepository by lazy { RecordRepository(database, normalizer, clock) { reminderScheduler.reschedule() } }

    private val dataStore by lazy { dataStoreFactory(appContext) }

    val settings by lazy { AppSettings(dataStore) }

    val reminderPrefs by lazy { ReminderPrefs(dataStore) }

    val appearancePrefs by lazy { AppearancePrefs(dataStore) }

    val operationPrefs by lazy { OperationPrefs(dataStore) }

    val guidePrefs by lazy { GuidePrefs(dataStore) }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database, clock) { reminderScheduler.reschedule() }
    }

    val reminderNotifier by lazy { ReminderNotifier(appContext) }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(appContext, { reminderRepository }, reminderPrefs, reminderNotifier, clock)
    }

    val reminderActions by lazy { ReminderActions(reminderRepository, shoppingRepository, reminderNotifier) }

    val storeRepository by lazy { StoreRepository(database, settings, normalizer, clock) }

    val widgetUpdater by lazy { WidgetUpdater(appContext, this) }

    /** 背景画像の保存先（F-61、`filesDir/background/`）。 */
    val backgroundStore by lazy { PhotoStore(appContext, PhotoStore.BACKGROUND_DIR) }

    val photoCleaner by lazy { PhotoCleaner(database, PhotoStore(appContext), clock) }

    /** データをすべて消す（F-76）。消したあとはお知らせの予約と通知を片付け、ウィジェットを空の表示に更新する（R-43）。 */
    val dataEraser by lazy {
        DataEraser(database, dataStore, listOf(PhotoStore(appContext), backgroundStore)) {
            reminderNotifier.cancel()
            reminderScheduler.reschedule()
            widgetUpdater.refresh()
        }
    }

    private val backupSettings by lazy {
        BackupSettingsStore(settings, appearancePrefs, operationPrefs, reminderPrefs)
    }

    /** データの書き出し（F-72）。 */
    val backupExporter by lazy {
        BackupExporter(
            database,
            backupSettings,
            PhotoStore(appContext),
            backgroundStore,
            clock,
        )
    }

    /** 書き出したファイルの読み込み（F-72）。入れ替えたあとはお知らせを予約し直し、ウィジェットを描き直す。 */
    val backupImporter by lazy {
        BackupImporter(appContext, database, backupSettings, PhotoStore(appContext), backgroundStore) {
            reminderScheduler.reschedule()
            widgetUpdater.refresh()
        }
    }

    /** 音声認識の窓口を作る。ViewModel ごとに 1 つ作り、ViewModel の破棄で片付ける。 */
    val speechEngineFactory: () -> SpeechEngine = { speechEngine(appContext) }
}
