package com.ikumemo.mobile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.AppSettings
import com.ikumemo.mobile.data.AppearancePrefs
import com.ikumemo.mobile.data.GuidePrefs
import com.ikumemo.mobile.data.OperationPrefs
import com.ikumemo.mobile.data.PhotoCleaner
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.data.RecordRepository
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.data.ReminderRepository
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
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

    val recordRepository by lazy { RecordRepository(database, normalizer, clock) }

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

    /** 音声認識の窓口を作る。ViewModel ごとに 1 つ作り、ViewModel の破棄で片付ける。 */
    val speechEngineFactory: () -> SpeechEngine = { speechEngine(appContext) }
}
