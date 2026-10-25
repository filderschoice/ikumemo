package com.ikumemo.mobile.voice

import androidx.lifecycle.viewModelScope
import com.ikumemo.mobile.R
import com.ikumemo.mobile.testing.FakeSpeechEngine
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** BL-022 の各欄のマイク。聞き取った文字は分割せずにそのまま入れる（R-32）。 */
class FieldDictationViewModelTest {
    private val engine = FakeSpeechEngine()
    private val viewModel = FieldDictationViewModel(engine)

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun nameIsNotSplit() {
        viewModel.start("edit_name")
        engine.emit(SpeechEvent.Results(listOf(" 牛乳とたまご ", "牛乳と卵")))

        // 品名欄では「と」で分けず、いちばん確からしい候補をそのまま入れる
        assertEquals(FieldText("edit_name", "牛乳とたまご"), viewModel.state.value.result)
        assertNull(viewModel.state.value.listening)
    }

    @Test
    fun onlyOneFieldListensAtATime() {
        viewModel.start("edit_name")
        viewModel.start("edit_note")

        assertEquals("edit_note", viewModel.state.value.listening)
        assertEquals(2, engine.cancelCount)
    }

    @Test
    fun notHeardAndStopAddNothing() {
        viewModel.start("edit_quantity")
        engine.emit(SpeechEvent.Error(SpeechError.NOT_HEARD))

        assertNull(viewModel.state.value.result)
        assertEquals(FieldMessage("edit_quantity", R.string.voice_not_heard), viewModel.state.value.message)

        viewModel.start("edit_quantity")
        viewModel.stop()
        assertNull(viewModel.state.value.listening)
    }

    @Test
    fun singleLineReplacesAndNoteAppends() {
        assertEquals("2本", FieldDictationViewModel.merge("1本", "2本", singleLine = true))
        assertEquals("特売の日に", FieldDictationViewModel.merge("", "特売の日に", singleLine = false))
        assertEquals("低脂肪\n特売の日に", FieldDictationViewModel.merge("低脂肪 ", "特売の日に", singleLine = false))
    }
}
