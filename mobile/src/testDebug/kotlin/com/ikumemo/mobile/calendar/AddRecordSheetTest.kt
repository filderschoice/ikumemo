package com.ikumemo.mobile.calendar

import android.content.Context
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.designsystem.IkumemoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * あとから記録のシート（F-17）を開いたときのフォーカス。品名の欄にだけフォーカスが当たる（BL-131）。
 */
@RunWith(RobolectricTestRunner::class)
class AddRecordSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun show() {
        val photoStore = PhotoStore(context)
        composeRule.setContent {
            IkumemoTheme {
                AddRecordSheet(
                    date = LocalDate.of(2026, 11, 13),
                    stores = emptyList(),
                    photoStore = photoStore,
                    onEvent = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun nameFieldIsFocusedOnOpen() {
        show()

        composeRule.onNodeWithTag(CalendarTestTags.ADD_RECORD_NAME).assertIsFocused()
        composeRule.onNodeWithTag("add_record_quantity").assertIsNotFocused()
    }
}
