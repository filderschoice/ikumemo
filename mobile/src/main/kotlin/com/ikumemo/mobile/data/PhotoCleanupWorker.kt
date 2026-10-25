package com.ikumemo.mobile.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ikumemo.mobile.IkumemoApplication
import java.util.concurrent.TimeUnit

/** 写真の掃除（R-14）を 1 日 1 回動かす Worker（PLAN §4.1 定期処理）。 */
class PhotoCleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as IkumemoApplication).container
        container.photoCleaner.clean()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "photo-cleanup"

        /**
         * 1 日 1 回の掃除を予約する。既に予約があれば残す（起動のたびに周期をやり直さない）。
         * 端末を使っていない間に限る（「やめた」の元に戻す（R-04）を待っている写真を消さないため）。
         */
        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<PhotoCleanupWorker>(1, TimeUnit.DAYS)
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiresDeviceIdle(true)
                            .setRequiresBatteryNotLow(true)
                            .build(),
                    ).build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
