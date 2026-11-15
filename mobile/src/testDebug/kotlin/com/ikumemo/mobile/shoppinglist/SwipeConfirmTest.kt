package com.ikumemo.mobile.shoppinglist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/** BL-193 の完了条件。速度だけでは確定せず、縦優勢では確定せず、幅の 40% 以上で従来どおり確定する（R-05）。 */
@RunWith(RobolectricTestRunner::class)
class SwipeConfirmTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var bought = 0
    private var dismissed = 0
    private val item =
        ShoppingItemEntity(id = 1, productId = 1, displayName = "牛乳", sortOrder = 0, createdAt = Instant.EPOCH)

    private fun show() {
        composeRule.setContent {
            IkumemoTheme {
                Box(Modifier.width(400.dp)) {
                    SwipeableItemRow(item, false, onClick = {}, onBought = { bought++ }, onDismissed = { dismissed++ })
                }
            }
        }
    }

    private fun drag(
        dx: Float,
        dy: Float,
        durationMillis: Long,
    ) {
        composeRule.onNodeWithTag(TestTags.item(1)).performTouchInput {
            val start = Offset(width * 0.5f, height * 0.5f)
            swipe(start, start + Offset(dx * width, dy * width), durationMillis)
        }
        composeRule.waitForIdle()
    }

    @Test
    fun fastFlingBelowThresholdDoesNotConfirm() {
        show()
        drag(dx = 0.3f, dy = 0f, durationMillis = 20)
        drag(dx = -0.3f, dy = 0f, durationMillis = 20)
        assertEquals(0, bought)
        assertEquals(0, dismissed)
    }

    @Test
    fun verticalDominantDragDoesNotConfirm() {
        show()
        drag(dx = 0.45f, dy = 0.9f, durationMillis = 100)
        assertEquals(0, bought)
        assertEquals(0, dismissed)
    }

    @Test
    fun dragBeyondThresholdConfirms() {
        show()
        drag(dx = 0.45f, dy = 0f, durationMillis = 300)
        assertEquals(1, bought)
        assertEquals(0, dismissed)
    }

    @Test
    fun dragLeftBeyondThresholdDismisses() {
        show()
        drag(dx = -0.45f, dy = 0f, durationMillis = 300)
        assertEquals(1, dismissed)
        assertEquals(0, bought)
    }

    @Test
    fun guardChecksPositionAndAxis() {
        assertTrue(shouldConfirmSwipe(-200f, 400f, -200f, 10f))
        assertFalse(shouldConfirmSwipe(100f, 400f, 100f, 0f))
        assertFalse(shouldConfirmSwipe(200f, 400f, 200f, 300f))
        assertFalse(shouldConfirmSwipe(200f, 0f, 200f, 0f))
    }
}
