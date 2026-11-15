package com.ikumemo.mobile.share

import android.content.Intent
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/** メモを送る文の組み立てと共有シートの Intent（BL-196）。 */
@RunWith(RobolectricTestRunner::class)
class ShareTextTest {
    private fun item(
        name: String,
        quantity: String? = null,
        order: Int = 0,
    ) = ShoppingItemEntity(
        productId = 1,
        displayName = name,
        quantity = quantity,
        sortOrder = order,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun emptyMemoHasOnlyHeaderAndIsNotShareable() {
        assertEquals("🛒いくメモ", ShareText.build(emptyList()))
        assertFalse(ShareText.isShareable(emptyList()))
    }

    @Test
    fun itemsAreListedInOrderWithOptionalQuantity() {
        val items = listOf(item("牛乳", "2本"), item("たまご"), item("パン", " "))
        assertEquals("🛒いくメモ\n・牛乳 2本\n・たまご\n・パン", ShareText.build(items))
        assertTrue(ShareText.isShareable(items))
    }

    @Test
    fun newlinesInsideAnItemNeverBreakTheLineFormat() {
        assertEquals("🛒いくメモ\n・牛 乳 2 本", ShareText.build(listOf(item("牛\n乳", "2\n本"))))
    }

    @Test
    fun chooserWrapsATextPlainSendIntent() {
        val chooser = ShareText.chooserIntent("本文", "メモを送る")
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertEquals("本文", send.getStringExtra(Intent.EXTRA_TEXT))
    }
}
