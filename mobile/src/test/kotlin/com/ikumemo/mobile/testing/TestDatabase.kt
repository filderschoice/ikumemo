package com.ikumemo.mobile.testing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.data.AppSettings
import com.ikumemo.mobile.data.db.IkumemoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.Executors

/**
 * テスト用のインメモリ DB。Flow の反映は非同期のため、テストでは Turbine で目的の状態まで待つ。
 * 実行スレッドは DB ごとに持つ（既定の共有プールだと、前のテストの処理が残って次のテストが止まることがあるため）。
 */
fun inMemoryDatabase(): IkumemoDatabase =
    Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), IkumemoDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor(Executors.newFixedThreadPool(QUERY_THREADS))
        .setTransactionExecutor(Executors.newSingleThreadExecutor())
        .build()

private const val QUERY_THREADS = 4

/** 日本時間の固定時計。 */
fun fixedClock(instant: String = "2026-10-25T01:00:00Z"): MutableClock = MutableClock(Instant.parse(instant))

class MutableClock(
    var now: Instant,
    private val zoneId: ZoneId = ZoneId.of("Asia/Tokyo"),
) : Clock() {
    override fun getZone(): ZoneId = zoneId

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)

    override fun instant(): Instant = now

    fun advanceSeconds(seconds: Long) {
        now = now.plusSeconds(seconds)
    }
}

/**
 * ViewModel テストで Dispatchers.Main を差し替える。Room の実行スレッドから戻る処理もテストのスレッドで順に動かすため
 * StandardTestDispatcher を使う。テスト側は終了時に ViewModel のスコープを止めてから差し替えを戻す。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

/** テスト用の設定ファイル。テストごとに別の一時ファイルへ保存する。 */
fun testDataStore(): DataStore<Preferences> {
    val file = File.createTempFile("ikumemo_settings", ".preferences_pb").apply { delete() }
    file.deleteOnExit()
    return PreferenceDataStoreFactory.create { file }
}

fun testSettings(): AppSettings = AppSettings(testDataStore())
