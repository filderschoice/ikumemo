package com.ikumemo.mobile.data.backup

import com.ikumemo.mobile.data.db.ReminderStatus

/**
 * 読み込むファイルの検査（F-72）。外から来た入力なので、DB に触る前にすべて確かめる。
 * 通れば表の外部キーと一意制約を満たすことが保証され、そのまま入れ直せる。
 */
object BackupValidator {
    const val MAX_ROWS = 2_000_000
    private const val MAX_DAY = 1_000_000L
    private val photoPath = Regex("^photos/[A-Za-z0-9._-]{1,128}$")
    private val backgroundPath = Regex("^background/[A-Za-z0-9._-]{1,128}$")

    fun isPhotoPath(path: String): Boolean = photoPath.matches(path)

    fun isBackgroundPath(path: String): Boolean = backgroundPath.matches(path)

    /** 問題があればその内容を、なければ null を返す。 */
    fun validate(file: BackupFile): String? = header(file) ?: uniqueIds(file) ?: references(file) ?: values(file)

    private fun header(file: BackupFile): String? =
        when {
            file.format != BackupFile.FORMAT -> "いくメモのファイルではありません"
            file.version != BackupFile.VERSION -> "対応していない版です（${file.version}）"
            rows(file) > MAX_ROWS -> "件数が多すぎます"
            else -> null
        }

    private fun rows(file: BackupFile): Int =
        listOf(file.stores, file.products, file.items, file.trips, file.records, file.reminders).sumOf { it.size }

    private fun uniqueIds(file: BackupFile): String? =
        when {
            !file.stores.map { it.id }.isUnique() -> "お店の ID が重なっています"
            !file.products.map { it.id }.isUnique() -> "品目の ID が重なっています"
            !file.products.map { it.normalizedName }.isUnique() -> "品目の名前が重なっています"
            !file.items.map { it.id }.isUnique() -> "メモの ID が重なっています"
            !file.trips.map { it.id }.isUnique() -> "お買い物の ID が重なっています"
            !file.records.map { it.id }.isUnique() -> "記録の ID が重なっています"
            !file.reminders.map { it.id }.isUnique() -> "期限の ID が重なっています"
            !file.reminders.map { it.purchaseRecordId }.isUnique() -> "1つの記録に期限が複数あります"
            else -> null
        }

    private fun references(file: BackupFile): String? {
        val stores = file.stores.map { it.id }.toSet()
        val products = file.products.map { it.id }.toSet()
        val trips = file.trips.map { it.id }.toSet()
        val records = file.records.map { it.id }.toSet()
        return when {
            file.items.any { it.productId !in products || it.plannedStoreId.missingIn(stores) } -> "メモの参照が合いません"
            file.trips.any { it.storeId.missingIn(stores) } -> "お買い物の参照が合いません"
            file.records.any { it.tripId !in trips || it.productId !in products || it.storeId.missingIn(stores) } ->
                "記録の参照が合いません"
            file.reminders.any { it.purchaseRecordId !in records || it.productId !in products } -> "期限の参照が合いません"
            else -> null
        }
    }

    private fun values(file: BackupFile): String? {
        val statuses = ReminderStatus.entries.map { it.name }.toSet()
        val photos = file.items.map { it.photoUri } + file.trips.map { it.photoUri } + file.records.map { it.photoUri }
        val days =
            file.products.mapNotNull { it.lastPurchasedAt } + file.trips.map { it.date } +
                file.records.map { it.purchasedDate } +
                file.reminders.flatMap { listOfNotNull(it.dueDate, it.snoozedUntil) }
        return when {
            photos.any { it != null && !isPhotoPath(it) } -> "写真のパスが正しくありません"
            file.settings.backgroundImage?.let { !isBackgroundPath(it) } == true -> "背景画像のパスが正しくありません"
            file.reminders.any { it.status !in statuses } -> "期限の状態が正しくありません"
            days.any { it !in -MAX_DAY..MAX_DAY } -> "日付が正しくありません"
            file.settings.reminderTimeMinutes?.let { it !in 0 until MINUTES_PER_DAY } == true -> "おしらせの時刻が正しくありません"
            else -> null
        }
    }

    private fun <T> List<T>.isUnique(): Boolean = size == toSet().size

    private fun Long?.missingIn(ids: Set<Long>): Boolean = this != null && this !in ids

    private const val MINUTES_PER_DAY = 24 * 60
}
