package com.ikumemo.mobile.calendar

import android.Manifest
import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.data.PhotoStore
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.testing.FakeSpeechEngine
import com.ikumemo.mobile.voice.FieldDictationViewModel
import com.ikumemo.mobile.voice.LocalFieldDictation
import com.ikumemo.mobile.voice.fieldMicTag
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** BL-117 の完了条件「検索欄にマイクがあり、話した品名が検索語に入る」（F-42）。 */
@RunWith(RobolectricTestRunner::class)
class CalendarSearchMicTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val engine = FakeSpeechEngine()
    private val events = mutableStateListOf<CalendarEvent>()
    private var current by mutableStateOf("")

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
    }

    /** 検索を開いた状態で描く。検索語は送られた [CalendarEvent.QueryChanged] で置き換える（ViewModel の代わり）。 */
    private fun show(
        query: String = "",
        voiceAvailable: Boolean = true,
    ) {
        engine.isAvailable = voiceAvailable
        current = query
        val dictation = FieldDictationViewModel(engine)
        val photoStore = PhotoStore(app)
        composeRule.setContent {
            IkumemoTheme {
                CompositionLocalProvider(LocalFieldDictation provides dictation) {
                    CalendarContent(
                        state = CalendarSamples.state(searchOpen = true, query = current),
                        photoStore = photoStore,
                        onEvent = { event ->
                            events += event
                            if (event is CalendarEvent.QueryChanged) current = event.text
                        },
                        showGuide = false,
                        onOpenSettings = {},
                    )
                }
            }
        }
    }

    @Test
    fun spokenNameBecomesTheQuery() {
        // 入っていた検索語は、話した品名で置き換える（1 行の欄）
        show(query = "卵")
        engine.autoResults = listOf("牛乳")

        composeRule.onNodeWithTag(fieldMicTag(CalendarTestTags.SEARCH_INPUT), useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<CalendarEvent>(CalendarEvent.QueryChanged("牛乳")), events.toList())
    }

    @Test
    fun noMicWhenRecognitionIsUnavailable() {
        show(voiceAvailable = false)

        composeRule.onNodeWithTag(CalendarTestTags.SEARCH_INPUT).assertExists()
        composeRule
            .onNodeWithTag(fieldMicTag(CalendarTestTags.SEARCH_INPUT), useUnmergedTree = true)
            .assertDoesNotExist()
    }
}
