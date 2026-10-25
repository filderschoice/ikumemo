package com.ikumemo.mobile.data

import androidx.room.withTransaction
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ReminderEntity
import com.ikumemo.mobile.data.db.ReminderRow
import com.ikumemo.mobile.data.db.ReminderStatus
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDate

/**
 * おしらせ（期限）の読み書き（F-50〜F-53）。書き換えたら [onChanged] を呼ぶ（次の通知の予約をし直すため、R-24）。
 */
class ReminderRepository(
    private val db: IkumemoDatabase,
    private val clock: Clock,
    private val onChanged: suspend () -> Unit = {},
) {
    private val reminderDao = db.reminderDao()
    private val recordDao = db.purchaseRecordDao()

    /** 最近買った物（7 日以内・期限未設定、F-50）。 */
    fun observeRecentWithoutReminder(): Flow<List<PurchaseRecordEntity>> =
        recordDao.observeRecentWithoutReminder(LocalDate.now(clock).minusDays(RECENT_DAYS - 1))

    /** 未対応のおしらせ（期限が近い順、F-53）。 */
    fun observePending(): Flow<List<ReminderRow>> = reminderDao.observePendingRows()

    suspend fun pending(): List<ReminderRow> = reminderDao.getPendingRows()

    /** 期限を決める・決め直す（F-50）。決め直したら「あとで」は取り消す。 */
    suspend fun setDue(
        recordId: Long,
        dueDate: LocalDate,
    ) {
        db.withTransaction {
            val record = recordDao.getById(recordId) ?: return@withTransaction
            val existing = reminderDao.findByRecord(recordId)
            if (existing == null) {
                reminderDao.insert(
                    ReminderEntity(purchaseRecordId = recordId, productId = record.productId, dueDate = dueDate),
                )
            } else {
                reminderDao.update(
                    existing.copy(dueDate = dueDate, status = ReminderStatus.PENDING, snoozedUntil = null),
                )
            }
        }
        onChanged()
    }

    /** 使い切った（R-22・F-52）。 */
    suspend fun markUsedUp(reminderId: Long) = finish(reminderId, ReminderStatus.USED_UP)

    /** 捨てた（R-22）。 */
    suspend fun markDiscarded(reminderId: Long) = finish(reminderId, ReminderStatus.DISCARDED)

    /** あとで（翌日にもう一度、期限は変えない、R-23）。 */
    suspend fun snooze(reminderId: Long) {
        reminderDao.getById(reminderId)?.let {
            reminderDao.update(it.copy(snoozedUntil = LocalDate.now(clock).plusDays(1)))
        }
        onChanged()
    }

    suspend fun getRow(reminderId: Long): ReminderRow? =
        reminderDao.getPendingRows().firstOrNull {
            it.reminder.id == reminderId
        }

    private suspend fun finish(
        reminderId: Long,
        status: ReminderStatus,
    ) {
        reminderDao.getById(reminderId)?.let { reminderDao.update(it.copy(status = status)) }
        onChanged()
    }

    companion object {
        /** 「最近買った物」に出す日数（きょうを含む）。 */
        const val RECENT_DAYS = 7L
    }
}
