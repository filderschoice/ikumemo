package com.ikumemo.mobile.reminder

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ReminderEntity
import com.ikumemo.mobile.data.db.ReminderRow
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.designsystem.LocalHasBackgroundImage
import com.ikumemo.mobile.designsystem.ReadablePanelTestTags
import com.ikumemo.mobile.help.HelpPages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

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
    private var openedSettings = 0
    private var openedReminderSettings = 0
    private var enableRequests = 0

    private fun show(
        state: ReminderUiState = ReminderSamples.state(),
        hasBackgroundImage: Boolean = false,
    ) {
        composeRule.setContent {
            IkumemoTheme {
                CompositionLocalProvider(LocalHasBackgroundImage provides hasBackgroundImage) {
                    ReminderContent(
                        state = state,
                        onEvent = { events += it },
                        onEnableNotifications = { enableRequests++ },
                        navigation =
                            ReminderNavigation(
                                onOpenHelp = { openedHelp += it },
                                onOpenSettings = { openedSettings++ },
                                onOpenReminderSettings = { openedReminderSettings++ },
                            ),
                        showGuide = false,
                    )
                }
            }
        }
    }

    private fun panelCount(): Int =
        composeRule.onAllNodesWithTag(ReadablePanelTestTags.PANEL, useUnmergedTree = true).fetchSemanticsNodes().size

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
    fun pendingSectionComesBeforeRecentSection() {
        show(
            ReminderSamples.state(
                recent = listOf(ReminderSamples.recent(id = 2, name = "牛乳")),
                pending = listOf(ReminderSamples.pending(id = 4, name = "豆腐")),
            ),
        )

        val pendingTitle = composeRule.onNodeWithText(text(R.string.reminder_pending)).fetchSemanticsNode()
        val recentTitle = composeRule.onNodeWithText(text(R.string.reminder_recent)).fetchSemanticsNode()
        val pendingRow = composeRule.onNodeWithTag(ReminderTestTags.pending(4)).fetchSemanticsNode()
        assertTrue(pendingTitle.boundsInRoot.top < pendingRow.boundsInRoot.top)
        assertTrue(pendingRow.boundsInRoot.top < recentTitle.boundsInRoot.top)
    }

    @Test
    fun recentShowsThreeAndMoreButtonShowsTheRest() {
        val recent = (1L..5L).map { ReminderSamples.recent(id = it, name = "品$it") }
        show(ReminderSamples.state(recent = recent))

        val list = composeRule.onNode(hasScrollAction())
        // 一覧は LazyColumn のため、画面外の行は作られない。「ほか N 件を見る」まで送ってから確かめる
        list.performScrollToNode(hasTestTag(ReminderTestTags.RECENT_MORE))
        composeRule.onNodeWithTag(ReminderTestTags.setDue(3)).assertExists()
        composeRule.onNodeWithTag(ReminderTestTags.setDue(4)).assertDoesNotExist()
        composeRule
            .onNodeWithTag(ReminderTestTags.RECENT_MORE)
            .assertTextEquals(text(R.string.reminder_recent_more, 2))
            .performClick()

        list.performScrollToNode(hasTestTag(ReminderTestTags.setDue(5)))
        composeRule.onNodeWithTag(ReminderTestTags.setDue(5)).assertIsDisplayed()
        composeRule.onNodeWithTag(ReminderTestTags.RECENT_MORE).assertDoesNotExist()
    }

    @Test
    fun threeRecentRecordsHaveNoMoreButton() {
        show(ReminderSamples.state(recent = (1L..3L).map { ReminderSamples.recent(id = it, name = "品$it") }))

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(ReminderTestTags.NOT_ARRIVING))
        composeRule.onNodeWithTag(ReminderTestTags.setDue(3)).assertExists()
        composeRule.onNodeWithTag(ReminderTestTags.RECENT_MORE).assertDoesNotExist()
    }

    @Test
    fun noDueOnARecentRecordSendsItsEvent() {
        show(ReminderSamples.state(recent = listOf(ReminderSamples.recent(id = 2, name = "トイレットペーパー"))))

        composeRule.onNodeWithTag(ReminderTestTags.noDue(2)).performScrollTo().performClick()

        assertEquals(listOf<ReminderEvent>(ReminderEvent.NoDue(2, "トイレットペーパー")), events.toList())
    }

    @Test
    fun noDueUndoOfferShowsMessageAndUndo() {
        show(ReminderSamples.state().copy(undo = NoDueUndo(serial = 1, recordId = 2, name = "牛乳")))

        composeRule.onNodeWithText(text(R.string.reminder_no_due_message, "牛乳")).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.memo_undo)).performClick()

        composeRule.waitForIdle()
        assertEquals(listOf<ReminderEvent>(ReminderEvent.UndoNoDue(1)), events.toList())
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
    fun settingsButtonOpensSettings() {
        show()

        composeRule.onNodeWithTag(ReminderTestTags.SETTINGS).performClick()

        assertEquals(1, openedSettings)
    }

    @Test
    fun reminderTimeButtonShowsTheTimeAndOpensItsSettings() {
        show(ReminderSamples.state().copy(notificationsEnabled = false, notifyTime = LocalTime.of(7, 30)))

        // 通知オフの帯の下、一覧の上に設定値の時刻を出す（BL-143）
        val banner = composeRule.onNodeWithTag(ReminderTestTags.NOTIFICATIONS_OFF).fetchSemanticsNode().boundsInRoot
        val button = composeRule.onNodeWithTag(ReminderTestTags.TIME).fetchSemanticsNode().boundsInRoot
        val title = composeRule.onNodeWithText(text(R.string.reminder_pending)).fetchSemanticsNode().boundsInRoot
        assertTrue(button.top >= banner.bottom && button.bottom <= title.top)
        composeRule.onNodeWithText(text(R.string.reminder_time_button, "7:30")).assertIsDisplayed()
        composeRule.onNodeWithTag(ReminderTestTags.TIME).performClick()

        assertEquals(1, openedReminderSettings)
    }

    @Test
    fun emptyListsShowTheirHints() {
        show(ReminderSamples.state())

        composeRule.onNodeWithText(text(R.string.reminder_recent_empty)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.reminder_pending_empty)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun backgroundImageLaysPanelsUnderTheRows() {
        show(
            ReminderSamples.state(
                recent = listOf(ReminderSamples.recent(id = 2, name = "牛乳")),
                pending = listOf(ReminderSamples.pending(id = 4, name = "豆腐")),
            ),
            hasBackgroundImage = true,
        )

        // 期限が近い順の行・最近買った物の行・「おしらせが届かないとき」は面の上に描き、見出しは写真の上に残す（BL-130）
        assertOnPanel(ReminderTestTags.TIME)
        assertOnPanel(ReminderTestTags.pending(4))
        assertOnPanel(ReminderTestTags.setDue(2))
        assertOnPanel(ReminderTestTags.NOT_ARRIVING)
        val title = composeRule.onNodeWithText(text(R.string.reminder_pending)).performScrollTo().fetchSemanticsNode()
        assertFalse(onPanel(title.boundsInRoot))
    }

    /** [tag] の要素まで送り、その全体がどれかの面の内側にあることを確かめる。 */
    private fun assertOnPanel(tag: String) {
        // 一覧は LazyColumn のため、画面外の行は作られない。送ってから探す
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
        val bounds = composeRule.onNodeWithTag(tag).performScrollTo().fetchSemanticsNode().boundsInRoot
        assertTrue("$tag が面の上に無い", onPanel(bounds))
    }

    private fun onPanel(bounds: Rect): Boolean =
        composeRule
            .onAllNodesWithTag(ReadablePanelTestTags.PANEL, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .any { it.boundsInRoot.intersect(bounds) == bounds }

    @Test
    fun noPanelsWithoutBackgroundImage() {
        show(ReminderSamples.state(pending = listOf(ReminderSamples.pending(id = 4, name = "豆腐"))))

        assertEquals(0, panelCount())
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
