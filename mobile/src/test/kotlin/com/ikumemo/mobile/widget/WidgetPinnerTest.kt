package com.ikumemo.mobile.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** アプリからウィジェットを置く（W-03、BL-072 の完了条件「2 種の追加要求が出せる」）。 */
@RunWith(RobolectricTestRunner::class)
class WidgetPinnerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val pinner = WidgetPinner(context)

    private fun setSupported(supported: Boolean) {
        shadowOf(AppWidgetManager.getInstance(context)).setRequestPinAppWidgetSupported(supported)
    }

    @Test
    fun bothWidgetsCanBeRequested() {
        setSupported(true)

        assertTrue(pinner.isSupported)
        assertTrue(pinner.pin(PinnableWidget.VOICE))
        assertTrue(pinner.pin(PinnableWidget.MEMO_LIST))
    }

    @Test
    fun unsupportedHomeAppDoesNotRequest() {
        setSupported(false)

        assertFalse(pinner.isSupported)
        assertFalse(pinner.pin(PinnableWidget.VOICE))
        assertFalse(pinner.pin(PinnableWidget.MEMO_LIST))
    }
}
