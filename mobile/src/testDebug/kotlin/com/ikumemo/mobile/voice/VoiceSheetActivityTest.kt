package com.ikumemo.mobile.voice

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.MainActivity
import com.ikumemo.mobile.testing.TestIkumemoApplication
import com.ikumemo.mobile.testing.awaitCondition
import com.ikumemo.mobile.testing.awaitTag
import com.ikumemo.mobile.widget.VoiceWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

/** BL-070 の完了条件「ウィジェットのタップでシートが開き、確認カードからメモへ追加できる。途中で閉じると何も追加されない」。 */
@RunWith(RobolectricTestRunner::class)
class VoiceSheetActivityTest {
    private val app = ApplicationProvider.getApplicationContext<Context>() as TestIkumemoApplication

    /** シートが開くとすぐ聞き取りを始めるため、Activity を起動する前にマイクの許可と認識結果を用意する。 */
    private val prepare: TestRule =
        object : TestWatcher() {
            override fun starting(description: Description) {
                shadowOf(app as Application).grantPermissions(Manifest.permission.RECORD_AUDIO)
                app.speechEngine.autoResults = listOf("牛乳2本とたまご")
            }
        }

    private val composeRule = createAndroidComposeRule<VoiceSheetActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(prepare).around(composeRule)

    private fun items() = runBlocking { app.container.shoppingRepository.observeItems().first() }

    @Test
    fun widgetOpensVoiceSheetWhenRecognitionIsAvailable() {
        val intent = VoiceWidget.launchIntent(app, voiceAvailable = true)

        assertEquals(VoiceSheetActivity::class.java.name, intent.component?.className)
    }

    @Test
    fun widgetOpensTextAddWhenRecognitionIsUnavailable() {
        val intent = VoiceWidget.launchIntent(app, voiceAvailable = false)

        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(MainActivity.OPEN_ADD_TEXT, intent.getStringExtra(MainActivity.EXTRA_OPEN_TAB))
    }

    @Test
    fun confirmCardsAreAddedAndSheetCloses() {
        composeRule.awaitTag(VoiceTestTags.card(1))

        // ボトムシートの中は座標のタップが届かないため、クリック処理を直接呼ぶ
        composeRule.onNodeWithTag(VoiceTestTags.ADD_ALL).performSemanticsAction(SemanticsActions.OnClick)

        composeRule.awaitCondition { items().size == 2 }
        composeRule.awaitCondition { composeRule.activity.isFinishing }
        assertEquals("2つ メモに入れました", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun closingSheetAddsNothing() {
        composeRule.awaitTag(VoiceTestTags.card(1))

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.awaitCondition { composeRule.activity.isFinishing }
        assertEquals(emptyList<Any>(), items())
    }
}
