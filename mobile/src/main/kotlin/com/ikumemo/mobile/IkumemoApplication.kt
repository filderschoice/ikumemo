package com.ikumemo.mobile

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class IkumemoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
        onStartup()
    }

    /** 単体テストでは DB・設定ファイル・音声認識をテスト用に差し替える。 */
    protected open fun createContainer(): AppContainer = AppContainer(this)

    /** 起動時の処理。単体テストでは本物の DB・設定ファイルに触れないよう何もしない Application に差し替える。 */
    protected open fun onStartup() {
        container.reminderNotifier.ensureChannel()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        // 電源が切れていた間の通知を補い、次の通知を予約し直す（R-24・R-28）
        scope.launch { container.reminderScheduler.notifyIfDue() }
        // データ・見た目が変わったらウィジェットを描き直す（R-41）
        container.widgetUpdater.start(scope)
    }
}
