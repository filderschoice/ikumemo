package com.ikumemo.mobile.share

import android.content.Context
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.core.share.SharedTextParser
import com.ikumemo.mobile.testing.TestIkumemoApplication
import com.ikumemo.mobile.testing.awaitCondition
import com.ikumemo.mobile.testing.awaitTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

/** BL-197 の共有の受け取り。ACTION_SEND の text/plain だけを受け付け、確かめた品物をメモへ入れる。 */
@RunWith(RobolectricTestRunner::class)
class ShareReceiveActivityTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Context>() as TestIkumemoApplication

    private fun items() =
        runBlocking { app.container.shoppingRepository.observeItems().first().map { it.displayName to it.quantity } }

    private fun sendIntent(
        text: CharSequence?,
        type: String = "text/plain",
    ) = Intent(app, ShareReceiveActivity::class.java).apply {
        action = Intent.ACTION_SEND
        this.type = type
        if (text != null) putExtra(Intent.EXTRA_TEXT, text)
    }

    @Test
    fun sharedIkumemoTextIsAddedAfterConfirming() {
        ActivityScenario.launch<ShareReceiveActivity>(sendIntent("🛒いくメモ\n・牛乳 2本\n・パン")).use { scenario ->
            lateinit var activity: ShareReceiveActivity
            scenario.onActivity { activity = it }
            composeRule.awaitTag(ShareImportTestTags.row(1))

            // ボトムシートの中は座標のタップが届かないため、クリック処理を直接呼ぶ
            composeRule.onNodeWithTag(ShareImportTestTags.ADD).performSemanticsAction(SemanticsActions.OnClick)

            composeRule.awaitCondition { items().size == 2 }
            composeRule.awaitCondition { activity.isFinishing }
            assertEquals(listOf("牛乳" to "2本", "パン" to null), items())
            assertEquals("2つ メモに入れました", ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun emptyTextShowsMessageAndAddsNothing() {
        ActivityScenario.launch<ShareReceiveActivity>(sendIntent(null)).use {
            composeRule.awaitTag(ShareImportTestTags.EMPTY)
            assertEquals(emptyList<Any>(), items())
        }
    }

    @Test
    fun otherIntentsCloseWithoutReading() {
        val image = sendIntent("牛乳", type = "image/png")
        ActivityScenario.launch<ShareReceiveActivity>(image).use { scenario ->
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
        val view = sendIntent("牛乳").setAction(Intent.ACTION_VIEW)
        ActivityScenario.launch<ShareReceiveActivity>(view).use { scenario ->
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
        assertEquals(emptyList<Any>(), items())
    }

    @Test
    fun intentFilterAcceptsOnlyTextPlainSend() {
        assertTrue(SharedIntent.isAccepted(sendIntent("x")))
        assertTrue(SharedIntent.isAccepted(sendIntent("x", type = "text/plain; charset=UTF-8")))
        assertFalse(SharedIntent.isAccepted(sendIntent("x", type = "text/html")))
        assertFalse(SharedIntent.isAccepted(null))
        assertFalse(SharedIntent.isAccepted(Intent(Intent.ACTION_SEND)))
    }

    @Test
    fun longTextIsCutBeforeParsing() {
        val text = SharedIntent.text(sendIntent("あ".repeat(1_000_000)))
        assertEquals(SharedTextParser.MAX_TEXT_LENGTH, text?.length)
        assertNull(SharedIntent.text(sendIntent(null)))
    }
}
