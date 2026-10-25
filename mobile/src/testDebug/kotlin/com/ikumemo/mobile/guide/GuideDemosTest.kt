package com.ikumemo.mobile.guide

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** はじめての案内と G-03 の手本のアニメーション（BL-098 の完了条件）。アニメーションを減らす設定では止める（R-56）。 */
@RunWith(RobolectricTestRunner::class)
class GuideDemosTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun exists(tag: String) = composeRule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun voiceDemoPlaysSpokenThenSplitAndStopsSplit() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { IkumemoTheme { VoiceSplitDemo(reduceMotion = false) } }
        composeRule.mainClock.advanceTimeByFrame()

        // 話した文が出てから、2 つの品目に分かれる
        composeRule.onNodeWithTag(GuideDemoTags.SPOKEN).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(1_600)
        composeRule.onNodeWithTag(GuideDemoTags.SPLIT).assertIsDisplayed()

        // くり返したあとは分かれた形で止まる（いつまでも動き続けない）
        composeRule.mainClock.advanceTimeBy(20_000)
        composeRule.onNodeWithTag(GuideDemoTags.SPLIT).assertIsDisplayed()
        assertTrue(!exists(GuideDemoTags.SPOKEN))
    }

    @Test
    fun voiceDemoIsStillWhenMotionIsReduced() {
        composeRule.setContent { IkumemoTheme { VoiceSplitDemo(reduceMotion = true) } }

        composeRule.onNodeWithTag(GuideDemoTags.SPLIT).assertIsDisplayed()
        assertTrue(!exists(GuideDemoTags.SPOKEN))
    }

    private fun nudge(reduceMotion: Boolean) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            IkumemoTheme {
                SwipeNudge(active = true, reduceMotion = reduceMotion) {
                    Box(Modifier.fillMaxWidth().height(56.dp).testTag("row"))
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(400)
    }

    @Test
    fun practiceRowIsNudgedRight() {
        nudge(reduceMotion = false)

        assertTrue(composeRule.onNodeWithTag("row").getBoundsInRoot().left > 0.dp)
    }

    @Test
    fun practiceRowStaysWhenMotionIsReduced() {
        nudge(reduceMotion = true)

        assertEquals(0.dp, composeRule.onNodeWithTag("row").getBoundsInRoot().left)
    }
}
