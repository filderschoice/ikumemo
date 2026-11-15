package com.ikumemo.mobile.voice

import android.Manifest
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.ShoppingRepository
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.testing.FakeSpeechEngine
import com.ikumemo.mobile.testing.awaitCondition
import com.ikumemo.mobile.testing.awaitTag
import com.ikumemo.mobile.testing.fixedClock
import com.ikumemo.mobile.testing.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** BL-190。1 回追加したあと、同じ ViewModel で開き直しても古い Added で即閉じず、聞き取り中になる。 */
@RunWith(RobolectricTestRunner::class)
class VoiceInputSheetRepeatTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: IkumemoDatabase
    private lateinit var repository: ShoppingRepository
    private val engine = FakeSpeechEngine()
    private lateinit var viewModel: VoiceInputViewModel
    private var shown by mutableStateOf(false)
    private var addedCalls = 0

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
        db = inMemoryDatabase()
        repository = ShoppingRepository(db, ProductNameNormalizer(), fixedClock())
        viewModel = VoiceInputViewModel(repository, engine)
        composeRule.setContent {
            IkumemoTheme {
                if (shown) {
                    VoiceInputSheet(
                        viewModel = viewModel,
                        onTypeInstead = {},
                        onAdded = {
                            addedCalls++
                            shown = false
                        },
                        onDismiss = { shown = false },
                    )
                }
            }
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun sheetListensAgainAfterAdding() {
        engine.autoResults = listOf("牛乳")
        repeat(ROUNDS) { round ->
            composeRule.runOnIdle { shown = true }
            composeRule.awaitTag(VoiceTestTags.card(0))
            assertEquals(round + 1, engine.startCount)
            composeRule.runOnIdle { viewModel.onEvent(VoiceInputEvent.ToggleCard(0)) }
            composeRule.runOnIdle { viewModel.onEvent(VoiceInputEvent.ToggleCard(0)) }
            composeRule.runOnIdle { viewModel.onEvent(VoiceInputEvent.AddAll) }
            composeRule.awaitCondition { addedCalls == round + 1 }
            composeRule.awaitCondition { viewModel.state.value == VoiceInputState.Idle }
        }
    }

    @Test
    fun staleAddedLeftBehindDoesNotCloseSheet() {
        engine.autoResults = null
        composeRule.runOnIdle { shown = true }
        composeRule.awaitCondition { viewModel.state.value is VoiceInputState.Listening }
        composeRule.runOnIdle { shown = false }
        // 画面が受け取らずに Added が残った状態から開き直す
        composeRule.runOnIdle { viewModel.onEvent(VoiceInputEvent.Cancel) }
        composeRule.runOnIdle { shown = true }
        composeRule.awaitCondition { engine.startCount == 2 }
        assertEquals(0, addedCalls)
    }

    private companion object {
        const val ROUNDS = 3
    }
}
