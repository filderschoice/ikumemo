package com.ikumemo.mobile.data

import com.ikumemo.mobile.data.db.IkumemoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration

/**
 * 使われなくなった写真の掃除（R-14）。メモ・お買い物・記録のどれからも参照されていない写真を消す。
 * 「やめた」で消したメモの写真（R-15）や、編集で差し替えた古い写真がここで消える。
 *
 * 取り込んだ直後でまだ DB に保存されていない写真を消さないよう、書いてから [minAge] 未満の写真は残す。
 */
class PhotoCleaner(
    private val database: IkumemoDatabase,
    private val photoStore: PhotoStore,
    private val clock: Clock,
    private val minAge: Duration = MIN_AGE,
) {
    /** 掃除して、消した数を返す。 */
    suspend fun clean(): Int {
        val referenced = database.photoDao().referencedPhotos().toSet()
        val modifiedBefore = clock.instant().minus(minAge).toEpochMilli()
        return withContext(Dispatchers.IO) { photoStore.deleteUnreferenced(referenced, modifiedBefore) }
    }

    companion object {
        val MIN_AGE: Duration = Duration.ofDays(1)
    }
}
