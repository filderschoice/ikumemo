package com.ikumemo.mobile.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ikumemo.mobile.data.db.IkumemoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * データをすべて消す（F-76）。記録・メモ・お店・品目（DB）、設定（DataStore）、写真と背景画像のファイルを消して、
 * 初めて使うときと同じ状態に戻す。取り消せない操作のため、確認は画面側で 1 回出す。
 *
 * 消したあとの後始末（お知らせの予約と通知、ウィジェットの描き直し、R-43）は [onErased] で呼び出し側が行う。
 */
class DataEraser(
    private val database: IkumemoDatabase,
    private val dataStore: DataStore<Preferences>,
    private val photoStores: List<PhotoStore>,
    private val onErased: suspend () -> Unit,
) {
    suspend fun eraseAll() {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
            dataStore.edit { it.clear() }
            photoStores.forEach { it.deleteAll() }
        }
        onErased()
    }
}
