package com.ikumemo.mobile

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 通知・ウィジェットから開く画面の受け渡し（R-40）。 */
@RunWith(RobolectricTestRunner::class)
class ScreenRequestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun itemIntentCarriesItemId() {
        val request = ScreenRequest.from(MainActivity.itemIntent(context, 42))

        assertEquals(ScreenRequest(MainActivity.OPEN_ITEM, 42), request)
    }

    @Test
    fun screenIntentHasNoItem() {
        assertEquals(
            ScreenRequest(MainActivity.TAB_REMINDER),
            ScreenRequest.from(MainActivity.intent(context, MainActivity.TAB_REMINDER)),
        )
        assertNull(ScreenRequest.from(Intent(context, MainActivity::class.java)))
    }

    @Test
    fun intentsForDifferentTargetsAreDistinct() {
        // 中身だけが違う Intent は PendingIntent が上書きし合うため、data で区別する
        val memo = MainActivity.intent(context, MainActivity.OPEN_MEMO)
        val reminder = MainActivity.intent(context, MainActivity.TAB_REMINDER)
        val item1 = MainActivity.itemIntent(context, 1)
        val item2 = MainActivity.itemIntent(context, 2)

        assertFalse(memo.filterEquals(reminder))
        assertFalse(item1.filterEquals(item2))
    }
}
