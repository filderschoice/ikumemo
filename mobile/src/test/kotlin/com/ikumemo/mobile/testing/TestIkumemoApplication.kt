package com.ikumemo.mobile.testing

import com.ikumemo.mobile.AppContainer
import com.ikumemo.mobile.IkumemoApplication

/**
 * 単体テスト用の Application（robolectric.properties で指定）。起動時の通知の確認などを行わず、
 * DB はインメモリ、設定はテストごとの一時ファイル、音声認識は [speechEngine] に差し替える。
 */
class TestIkumemoApplication : IkumemoApplication() {
    val speechEngine = FakeSpeechEngine()

    override fun createContainer(): AppContainer =
        AppContainer(
            context = this,
            clock = fixedClock(),
            databaseFactory = { inMemoryDatabase() },
            dataStoreFactory = { testDataStore() },
            speechEngine = { speechEngine },
        )

    override fun onStartup() = Unit
}
