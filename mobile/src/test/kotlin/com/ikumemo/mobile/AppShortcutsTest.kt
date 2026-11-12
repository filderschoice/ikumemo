package com.ikumemo.mobile

import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** アイコン長押しのショートカット（F-74、BL-072 の完了条件「ショートカット 4 種が各画面を開く」）。 */
@RunWith(RobolectricTestRunner::class)
class AppShortcutsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun fourShortcutsOpenEachScreen() {
        AppShortcuts.publish(context)

        val shortcuts = ShortcutManagerCompat.getDynamicShortcuts(context).sortedBy { it.rank }
        assertEquals(
            listOf("話して追加", "文字で追加", "記録", "期限"),
            shortcuts.map { it.shortLabel.toString() },
        )
        // 各ショートカットの Intent は MainActivity を開き、開く画面として受け取られる
        assertEquals(
            listOf(
                MainActivity.OPEN_VOICE,
                MainActivity.OPEN_ADD_TEXT,
                MainActivity.TAB_CALENDAR,
                MainActivity.TAB_REMINDER,
            ),
            shortcuts.map { requireNotNull(ScreenRequest.from(it.intent)).screen },
        )
        shortcuts.forEach { assertEquals(MainActivity::class.java.name, it.intent.component?.className) }
        // 開く先ごとに別の Intent になる（PendingIntent が上書きし合わない）
        assertEquals(4, shortcuts.map { it.intent.data }.distinct().size)
    }

    @Test
    fun publishingAgainDoesNotDuplicate() {
        AppShortcuts.publish(context)
        AppShortcuts.publish(context)

        assertEquals(4, ShortcutManagerCompat.getDynamicShortcuts(context).size)
    }
}
