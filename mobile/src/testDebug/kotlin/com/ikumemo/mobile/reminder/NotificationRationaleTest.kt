package com.ikumemo.mobile.reminder

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.R
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalTime

/** 通知の許可を求める前の説明（[NotificationRationaleDialog]、BL-140）。 */
@RunWith(RobolectricTestRunner::class)
class NotificationRationaleTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var nextCount = 0
    private var laterCount = 0

    private fun show(time: LocalTime) {
        composeRule.setContent {
            IkumemoTheme {
                NotificationRationaleDialog(
                    time = time,
                    onNext = { nextCount++ },
                    onLater = { laterCount++ },
                )
            }
        }
    }

    @Test
    fun showsTheTitleAndTheSetTime() {
        show(LocalTime.of(7, 30))

        composeRule.onNodeWithText(context.getString(R.string.reminder_permission_title)).assertIsDisplayed()
        composeRule.onNodeWithText("7:30", substring = true).assertIsDisplayed()
        val body = context.getString(R.string.reminder_permission_rationale, "7:30")
        composeRule.onNodeWithText(body).assertIsDisplayed()
    }

    @Test
    fun nextAsksForThePermission() {
        show(LocalTime.of(9, 0))

        composeRule.onNodeWithText(context.getString(R.string.reminder_permission_next)).performClick()

        assertEquals(1, nextCount)
        assertEquals(0, laterCount)
    }

    @Test
    fun laterDoesNotAskForThePermission() {
        show(LocalTime.of(9, 0))

        composeRule.onNodeWithText(context.getString(R.string.reminder_permission_later)).performClick()

        assertEquals(0, nextCount)
        assertEquals(1, laterCount)
    }
}
