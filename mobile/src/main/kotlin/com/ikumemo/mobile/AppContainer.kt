package com.ikumemo.mobile

import android.content.Context
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.AppSettings
import com.ikumemo.mobile.data.RecordRepository
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.voice.AndroidSpeechEngine
import com.ikumemo.mobile.voice.SpeechEngine
import java.time.Clock

/**
 * 手動 DI の入れ物（PLAN §4.1）。[IkumemoApplication] が 1 つだけ持ち、ViewModel・Worker・受信処理へ渡す。
 * テストでは [database] と [clock] を差し替えて作る。
 */
class AppContainer(
    context: Context,
    val clock: Clock = Clock.systemDefaultZone(),
    databaseFactory: (Context) -> IkumemoDatabase = IkumemoDatabase::create,
) {
    private val appContext = context.applicationContext

    val database: IkumemoDatabase by lazy { databaseFactory(appContext) }

    val normalizer = ProductNameNormalizer()

    val shoppingRepository by lazy { ShoppingRepository(database, normalizer, clock) }

    val recordRepository by lazy { RecordRepository(database) }

    val settings by lazy { AppSettings.create(appContext) }

    val storeRepository by lazy { StoreRepository(database, settings, normalizer, clock) }

    /** 音声認識の窓口を作る。ViewModel ごとに 1 つ作り、ViewModel の破棄で片付ける。 */
    val speechEngineFactory: () -> SpeechEngine = { AndroidSpeechEngine(appContext) }
}
