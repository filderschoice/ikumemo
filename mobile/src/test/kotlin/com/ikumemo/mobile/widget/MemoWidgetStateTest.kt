package com.ikumemo.mobile.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.data.AddItemResult
import com.ikumemo.mobile.data.UndoableAction
import com.ikumemo.mobile.testing.TestIkumemoApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/** メモ一覧ウィジェットの内容（メモと同じ並び、期限が近い件数、選んだお店、PLAN §2.11）。 */
@RunWith(RobolectricTestRunner::class)
class MemoWidgetStateTest {
    private val container = (ApplicationProvider.getApplicationContext<Context>() as TestIkumemoApplication).container
    private val today = LocalDate.of(2026, 10, 25)

    private suspend fun boughtRecord(name: String): Long {
        val itemId = (container.shoppingRepository.addItem(name) as AddItemResult.Added).itemId
        return (container.shoppingRepository.markBought(itemId) as UndoableAction.Bought).recordId
    }

    @Test
    fun emptyDataGivesEmptyState() =
        runTest {
            assertEquals(MemoWidgetState(), MemoWidgetState.load(container))
        }

    @Test
    fun collectsItemsDueSoonCountAndSelectedStore() =
        runTest {
            // 期限が近いのは前日（あした）・当日・期限切れ。あさって以降は数えない
            container.reminderRepository.setDue(boughtRecord("洗剤"), today.minusDays(1))
            container.reminderRepository.setDue(boughtRecord("しょうゆ"), today.plusDays(1))
            container.reminderRepository.setDue(boughtRecord("米"), today.plusDays(2))
            container.shoppingRepository.addItem("牛乳", quantity = "2本")
            container.shoppingRepository.addItem("たまご")
            val store = checkNotNull(container.storeRepository.addStore("さくらスーパー"))
            container.storeRepository.select(store.id)

            val state = MemoWidgetState.load(container)

            // 並びはアプリのメモと同じ
            val memo = container.shoppingRepository.observeItems().first()
            assertEquals(memo.map { it.id }, state.items.map { it.id })
            assertEquals(setOf("牛乳" to "2本", "たまご" to null), state.items.map { it.name to it.quantity }.toSet())
            assertEquals(2, state.dueSoonCount)
            assertEquals("さくらスーパー", state.storeName)
        }
}
