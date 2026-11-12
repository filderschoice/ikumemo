package com.ikumemo.mobile.reminder

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ReminderEntity
import com.ikumemo.mobile.data.db.ReminderRow
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.help.HelpPages
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

/**
 * おしらせの画面（F-50・F-53・R-29）の操作。[ReminderContent] に状態を渡して描き、押したときに出るイベントを確かめる。
 *
 * 文言やレイアウトが変わっても壊れにくいよう、要素はテストタグ（[ReminderTestTags]）で探し、
 * 文言で確かめるときは文字列リソース（[text]）から引く。テストを足すときは [ReminderSamples] で状態を組み立て、[show] で描く。
 */
@RunWith(RobolectricTestRunner::class)
class ReminderContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableStateListOf<ReminderEvent>()
    private val openedHelp = mutableStateListOf<String>()
    private var enableRequests = 0

    private fun show(state: ReminderUiState = ReminderSamples.state()) {
        composeRule.setContent {
            IkumemoTheme {
                ReminderContent(
                    state = state,
                    onEvent = { events += it },
                    onEnableNotifications = { enableRequests++ },
                    onOpenHelp = { openedHelp += it },
                    showGuide = false,
                )
            }
        }
    }

    private fun text(
        @StringRes id: Int,
        vararg args: Any,
    ): String = context.getString(id, *args)

    @Test
    fun usedUpAndDiscardedSendTheirEvents() {
        show(ReminderSamples.state(pending = listOf(ReminderSamples.pending(id = 4, name = "豆腐"))))

        composeRule.onNodeWithTag(ReminderTestTags.usedUp(4)).performScrollTo().performClick()
        composeRule.onNodeWithTag(ReminderTestTags.discarded(4)).performScrollTo().performClick()

        assertEquals(
            listOf(ReminderEvent.UsedUp(4), ReminderEvent.Discarded(4)),
            events.toList(),
        )
    }

    @Test
    fun tappingAPendingRowOpensTheDueSheetAgain() {
        val row = ReminderSamples.pending(id = 4, recordId = 9, name = "豆腐")
        show(ReminderSamples.state(pending = listOf(row)))

        composeRule.onNodeWithTag(ReminderTestTags.pending(4)).performScrollTo().performClick()

        assertEquals(
            listOf<ReminderEvent>(ReminderEvent.OpenDue(DueTarget(9, "豆腐", row.reminder.dueDate))),
            events.toList(),
        )
    }

    @Test
    fun setDueOnARecentRecordOpensTheDueSheet() {
        show(ReminderSamples.state(recent = listOf(ReminderSamples.recent(id = 2, name = "牛乳"))))

        composeRule.onNodeWithTag(ReminderTestTags.setDue(2)).performScrollTo().performClick()

        assertEquals(listOf<ReminderEvent>(ReminderEvent.OpenDue(DueTarget(2, "牛乳"))), events.toList())
    }

    @Test
    fun notificationsOffBannerAsksToEnable() {
        show(ReminderSamples.state(notificationsEnabled = false))

        composeRule.onNodeWithTag(ReminderTestTags.NOTIFICATIONS_OFF).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.reminder_enable_notifications)).performClick()

        assertEquals(1, enableRequests)
    }

    @Test
    fun noBannerWhileNotificationsAreOn() {
        show(ReminderSamples.state(notificationsEnabled = true))

        composeRule.onNodeWithTag(ReminderTestTags.NOTIFICATIONS_OFF).assertDoesNotExist()
    }

    @Test
    fun notArrivingOpensItsHelpPage() {
        show()

        composeRule.onNodeWithTag(ReminderTestTags.NOT_ARRIVING).performScrollTo().performClick()

        assertEquals(listOf(HelpPages.NOT_ARRIVING), openedHelp.toList())
    }

    @Test
    fun emptyListsShowTheirHints() {
        show(ReminderSamples.state())

        composeRule.onNodeWithText(text(R.string.reminder_recent_empty)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.reminder_pending_empty)).performScrollTo().assertIsDisplayed()
    }
}

/** おしらせの画面のテスト用の状態。既定は「読み込み済み、通知オン、どちらの一覧も空」。 */
internal object ReminderSamples {
    val TODAY: LocalDate = LocalDate.of(2026, 11, 13)
    private val RECORDED_AT: Instant = Instant.parse("2026-11-13T01:00:00Z")

    fun state(
        recent: List<PurchaseRecordEntity> = emptyList(),
        pending: List<ReminderRow> = emptyList(),
        notificationsEnabled: Boolean = true,
        loaded: Boolean = true,
    ): ReminderUiState =
        ReminderUiState(
            today = TODAY,
            recent = recent,
            pending = pending,
            notificationsEnabled = notificationsEnabled,
            loaded = loaded,
        )

    /** 最近買った物（期限をまだ決めていない記録）。 */
    fun recent(
        id: Long,
        name: String,
        purchasedDate: LocalDate = TODAY.minusDays(1),
    ): PurchaseRecordEntity =
        PurchaseRecordEntity(
            id = id,
            tripId = 1,
            productId = id,
            displayName = name,
            purchasedDate = purchasedDate,
            recordedAt = RECORDED_AT,
        )

    /** 期限が近い順の 1 行。既定の期限は 3 日後。 */
    fun pending(
        id: Long,
        name: String,
        recordId: Long = id,
        dueDate: LocalDate = TODAY.plusDays(3),
    ): ReminderRow =
        ReminderRow(
            reminder = ReminderEntity(id = id, purchaseRecordId = recordId, productId = recordId, dueDate = dueDate),
            displayName = name,
        )
}
