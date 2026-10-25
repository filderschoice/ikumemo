package com.ikumemo.mobile.voice

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.testing.FakeSpeechEngine
import com.ikumemo.mobile.testing.awaitCondition
import com.ikumemo.mobile.testing.awaitTag
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** BL-021 の完了条件「牛乳2本とたまご の認識結果から 2 件が確認カードに並び、追加できる」を画面で確かめる。 */
@RunWith(RobolectricTestRunner::class)
class VoiceInputContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var repository: ShoppingRepository
    private val engine = FakeSpeechEngine()
    private lateinit var viewModel: VoiceInputViewModel

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = ShoppingRepository(db, ProductNameNormalizer(), fixedClock())
        viewModel = VoiceInputViewModel(repository, engine)
        composeRule.setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            IkumemoTheme {
                VoiceInputContent(
                    state = state,
                    needsPermission = false,
                    onEvent = viewModel::onEvent,
                    onSheetAction = {},
                )
            }
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun twoCardsAreShownAndAdded() {
        engine.autoResults = listOf("牛乳2本とたまご")
        composeRule.runOnIdle { viewModel.onEvent(VoiceInputEvent.Start) }

        composeRule.awaitTag(VoiceTestTags.card(1))
        composeRule.onNodeWithText("牛乳").assertIsDisplayed()
        composeRule.onNodeWithText("2本").assertIsDisplayed()
        composeRule.onNodeWithText("たまご").assertIsDisplayed()

        composeRule.onNodeWithTag(VoiceTestTags.ADD_ALL).performClick()

        composeRule.awaitCondition { viewModel.state.value == VoiceInputState.Added(2) }
        check(runBlocking { repository.observeItems().first() }.map { it.displayName } == listOf("牛乳", "たまご"))
    }

    @Test
    fun notHeardShowsRetry() {
        composeRule.runOnIdle { viewModel.onEvent(VoiceInputEvent.Start) }
        composeRule.runOnIdle { engine.emit(SpeechEvent.Error(SpeechError.NOT_HEARD)) }

        composeRule.onNodeWithText("うまく聞き取れませんでした").assertIsDisplayed()
        composeRule.onNodeWithText("もう一度話す").performClick()

        composeRule.runOnIdle { check(engine.startCount == 2) }
    }
}
