package com.ikumemo.mobile.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.ikumemo.mobile.AppContainer
import com.ikumemo.mobile.runAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/**
 * ウィジェットの描き直しの窓口（R-41・R-43）。アプリが動いている間はメモ・おしらせ・お店・見た目の変化を見て描き直す。
 * 日付の変わり目と選んだお店の解除（R-06）はアプリが止まっていても起きるため、その時刻に [WidgetRefreshReceiver] を
 * 予約する（正確さは要らないため省電力の set を使う）。定期的な自動更新はしない（電池のため）。
 */
class WidgetUpdater(
    context: Context,
    private val container: AppContainer,
) {
    private val appContext = context.applicationContext

    /** 起動時に呼ぶ。最初の 1 回（再起動・日付の変更の後）も描き直す。 */
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(
                MemoWidgetState.observe(container),
                container.appearancePrefs.appearance,
            ) { memo, look -> memo to look }
                .distinctUntilChanged()
                .collectLatest { refresh() }
        }
    }

    suspend fun refresh() {
        val manager = GlanceAppWidgetManager(appContext)
        val placed =
            manager.getGlanceIds(MemoListWidget::class.java).isNotEmpty() ||
                manager.getGlanceIds(VoiceWidget::class.java).isNotEmpty()
        if (!placed) return
        MemoListWidget().updateAll(appContext)
        // テーマ・暗い画面の変更を 1×1 の色にも反映する
        VoiceWidget().updateAll(appContext)
        scheduleNext()
    }

    private suspend fun scheduleNext() {
        val zone = container.clock.zone
        val midnight =
            LocalDate
                .now(container.clock)
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
        val storeExpiry =
            container.settings.storeSelection
                .first()
                ?.expiresAt(zone)
                ?.takeIf { it.isAfter(container.clock.instant()) }
        val next: Instant = storeExpiry?.let { minOf(it, midnight) } ?: midnight
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        alarmManager.set(AlarmManager.RTC, next.toEpochMilli(), refreshIntent())
    }

    private fun refreshIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            REQUEST_REFRESH,
            Intent(appContext, WidgetRefreshReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val REQUEST_REFRESH = 2
    }
}

/** 予約した描き直しの時刻になった（アプリ内からだけ呼ぶため exported=false）。 */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        runAsync(context) { it.widgetUpdater.refresh() }
    }
}
