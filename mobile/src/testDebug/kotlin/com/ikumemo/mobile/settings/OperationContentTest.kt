package com.ikumemo.mobile.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.ikumemo.mobile.data.OperationSettings
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.shoppinglist.ShoppingListContent
import com.ikumemo.mobile.shoppinglist.ShoppingListUiState
import com.ikumemo.mobile.shoppinglist.TestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/** BL-082 の そうさ。切り替えが届くことと、○ を消すとメモの行から ○ が消えること。 */
@RunWith(RobolectricTestRunner::class)
class OperationContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val item =
        ShoppingItemEntity(id = 1, productId = 1, displayName = "牛乳", sortOrder = 0, createdAt = Instant.EPOCH)

    @Test
    fun switchesSendSettings() {
        val events = mutableStateListOf<OperationEvent>()
        composeRule.setContent {
            IkumemoTheme { OperationContent(OperationSettings(vibration = false), onEvent = { events += it }) }
        }

        composeRule.onNodeWithTag(OperationTestTags.SHOW_BOUGHT_BUTTON).assertIsOn().performClick()
        composeRule.onNodeWithTag(OperationTestTags.VIBRATION).assertIsOff().performClick()

        assertEquals(
            listOf(OperationEvent.SetShowBoughtButton(false), OperationEvent.SetVibration(true)),
            events.toList(),
        )
    }

    @Test
    fun hiddenCircleIsRemovedFromMemoRows() {
        composeRule.setContent {
            IkumemoTheme {
                ShoppingListContent(
                    state =
                        ShoppingListUiState(
                            items = listOf(item),
                            loaded = true,
                            operation = OperationSettings(showBoughtButton = false),
                        ),
                    voiceAvailable = false,
                    onEvent = {},
                )
            }
        }

        composeRule.onNodeWithTag(TestTags.item(item.id)).assertExists()
        composeRule.onNodeWithTag(TestTags.boughtButton(item.id)).assertDoesNotExist()
    }
}
