package com.ikumemo.mobile.guide

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** その場のガイドの吹き出し（F-70、R-53・R-56）。 */
@RunWith(RobolectricTestRunner::class)
class GuideOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableStateListOf<String>()

    private fun show(useCard: Boolean) {
        composeRule.setContent {
            IkumemoTheme {
                val targets = rememberGuideTargets()
                CompositionLocalProvider(LocalGuideTargets provides targets) {
                    Box(Modifier.fillMaxSize()) {
                        Text("🎤", Modifier.size(64.dp).guideTarget(GuideId.G02_VOICE_BUTTON, targets))
                        GuideOverlay(
                            guide = GuideId.G02_VOICE_BUTTON,
                            onDismiss = { events += "dismiss" },
                            onDisableAll = { events += "disable" },
                            useCard = useCard,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun spotlightShowsMessageAndButtons() {
        show(useCard = false)

        composeRule.onNodeWithTag(GuideTestTags.SPOTLIGHT).assertIsDisplayed()
        composeRule.onNodeWithText("ここを押して、買いたい物を話してみましょう").assertIsDisplayed()
        composeRule.onNodeWithTag(GuideTestTags.DISMISS).performClick()
        composeRule.onNodeWithTag(GuideTestTags.DISABLE_ALL).performClick()

        assertEquals(listOf("dismiss", "disable"), events.toList())
    }

    @Test
    fun screenReaderGetsCardWithoutSpotlight() {
        // R-56: 読み上げを使っているときは照らさず、画面下のカードで同じ内容を出す
        show(useCard = true)

        composeRule.onNodeWithTag(GuideTestTags.SPOTLIGHT).assertDoesNotExist()
        composeRule.onNodeWithTag(GuideTestTags.bubble(GuideId.G02_VOICE_BUTTON)).assertIsDisplayed()
        composeRule.onNodeWithText("分かった").performClick()

        assertEquals(listOf("dismiss"), events.toList())
    }
}
