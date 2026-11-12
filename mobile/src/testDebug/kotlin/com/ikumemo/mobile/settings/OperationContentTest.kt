package com.ikumemo.mobile.settings

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.viewModelScope
import com.ikumemo.core.reminder.ReminderSettings
import com.ikumemo.mobile.data.OperationPrefs
import com.ikumemo.mobile.data.OperationSettings
import com.ikumemo.mobile.data.ReminderPrefs
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.shoppinglist.ShoppingListContent
import com.ikumemo.mobile.shoppinglist.ShoppingListUiState
import com.ikumemo.mobile.shoppinglist.TestTags
import com.ikumemo.mobile.testing.awaitCondition
import com.ikumemo.mobile.testing.testDataStore
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalTime

/**
 * BL-082 の そうさ。切り替えが届くことと、○ を消すとメモの行から ○ が消えること。
 * BL-143 で お知らせの設定（時刻・当日のお知らせ）を同じ画面「操作とお知らせ」にまとめた。
 */
@RunWith(RobolectricTestRunner::class)
class OperationContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val item =
        ShoppingItemEntity(id = 1, productId = 1, displayName = "牛乳", sortOrder = 0, createdAt = Instant.EPOCH)

    @Test
    fun switchesSendSettings() {
        val events = mutableStateListOf<OperationEvent>()
        val reminderEvents = mutableStateListOf<ReminderSettingsEvent>()
        composeRule.setContent {
            IkumemoTheme {
                OperationContent(
                    OperationSettings(vibration = false),
                    onEvent = { events += it },
                    reminder = ReminderSettings(time = LocalTime.of(7, 30), notifyOnDueDate = true),
                    onReminderEvent = { reminderEvents += it },
                )
            }
        }

        // 1 つの画面に ○ を出す・振動・お知らせの時刻・期限の当日もお知らせ の 4 行（BL-143）
        composeRule.onNodeWithTag(OperationTestTags.SHOW_BOUGHT_BUTTON).assertIsOn().performClick()
        composeRule.onNodeWithTag(OperationTestTags.VIBRATION).assertIsOff().performClick()
        composeRule.onNodeWithText("7:30").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(ReminderSettingsTestTags.ON_DUE_DATE).performScrollTo().assertIsOn().performClick()

        assertEquals(
            listOf(OperationEvent.SetShowBoughtButton(false), OperationEvent.SetVibration(true)),
            events.toList(),
        )
        assertEquals(listOf(ReminderSettingsEvent.SetNotifyOnDueDate(false)), reminderEvents.toList())
    }

    @Test
    fun timeRowPicksANewTime() {
        val reminderEvents = mutableStateListOf<ReminderSettingsEvent>()
        composeRule.setContent {
            IkumemoTheme {
                OperationContent(OperationSettings(), onEvent = {}, onReminderEvent = { reminderEvents += it })
            }
        }

        composeRule.onNodeWithTag(ReminderSettingsTestTags.TIME).performScrollTo().performClick()
        composeRule.onNodeWithTag(ReminderSettingsTestTags.TIME_DONE).performClick()

        assertEquals(listOf(ReminderSettingsEvent.SetTime(ReminderSettings.DEFAULT_TIME)), reminderEvents.toList())
    }

    @Test
    fun combinedScreenKeepsSettingsSavedBeforeTheMerge() {
        // 2 つの画面に分かれていた頃のキーで保存した値が、まとめた画面にそのまま出る（BL-143、DataStore のキーは変えない）
        val dataStore = testDataStore()
        runBlocking {
            dataStore.edit {
                it[booleanPreferencesKey("operation_show_bought_button")] = false
                it[booleanPreferencesKey("operation_vibration")] = false
                it[intPreferencesKey("reminder_time_minutes")] = 20 * 60 + 15
                it[booleanPreferencesKey("reminder_notify_on_due_date")] = false
            }
        }
        val operation = OperationViewModel(OperationPrefs(dataStore))
        val reminder = ReminderSettingsViewModel(ReminderPrefs(dataStore)) {}
        composeRule.setContent {
            IkumemoTheme { OperationScreen(onBack = {}, viewModel = operation, reminderViewModel = reminder) }
        }

        composeRule.awaitCondition { composeRule.onAllNodes(hasText("20:15")).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(OperationTestTags.SHOW_BOUGHT_BUTTON).assertIsOff()
        composeRule.onNodeWithTag(OperationTestTags.VIBRATION).assertIsOff()
        composeRule.onNodeWithTag(ReminderSettingsTestTags.ON_DUE_DATE).performScrollTo().assertIsOff()
        operation.viewModelScope.cancel()
        reminder.viewModelScope.cancel()
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
