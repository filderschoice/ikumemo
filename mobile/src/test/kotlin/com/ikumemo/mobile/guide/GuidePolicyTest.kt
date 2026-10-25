package com.ikumemo.mobile.guide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** その場のガイドを出すかの判定（R-50・R-52・R-53・R-55、BL-080 の完了条件）。 */
class GuidePolicyTest {
    private val ready = GuideState(onboardingDone = true)

    @Test
    fun onlyFirstNotShownGuideIsReturned() {
        // R-50: 1 つの画面で同時に 2 つ以上は出さない。出したものは飛ばして次の機会に回す
        val candidates = listOf(GuideId.G03_SWIPE_PRACTICE, GuideId.G05_UNDO)
        assertEquals(GuideId.G03_SWIPE_PRACTICE, GuidePolicy.next(candidates, ready, GuideScene()))
        assertEquals(
            GuideId.G05_UNDO,
            GuidePolicy.next(candidates, ready.copy(shown = setOf(GuideId.G03_SWIPE_PRACTICE)), GuideScene()),
        )
        assertNull(GuidePolicy.next(candidates, ready.copy(shown = candidates.toSet()), GuideScene()))
    }

    @Test
    fun nothingWhileListeningSheetOpenOrOpenedFromOutside() {
        // R-52: 音声入力中・シート表示中・通知から開いた直後は出さない
        val candidates = listOf(GuideId.G02_VOICE_BUTTON)
        assertNull(GuidePolicy.next(candidates, ready, GuideScene(listening = true)))
        assertNull(GuidePolicy.next(candidates, ready, GuideScene(sheetOpen = true)))
        assertNull(GuidePolicy.next(candidates, ready, GuideScene(openedFromOutside = true)))
    }

    @Test
    fun nothingWhenDisabledOrBeforeOnboarding() {
        val candidates = listOf(GuideId.G02_VOICE_BUTTON)
        // R-53: 「ガイドを出さない」を選んだら全部止める
        assertNull(GuidePolicy.next(candidates, ready.copy(disabled = true), GuideScene()))
        // はじめての案内の途中は、その場のガイドを重ねない
        assertNull(GuidePolicy.next(candidates, GuideState(onboardingDone = false), GuideScene()))
    }

    @Test
    fun guideAddedInUpdateIsShownOnceForExistingUsers() {
        // R-55: 以前の版で G-02〜G-07 を見終えた人にも、新しく足したガイド（ここでは G-08）は 1 回だけ出る
        val oldGuides = GuideId.entries.filter { it != GuideId.G08_DUE_DATE }.toSet()
        val state = ready.copy(shown = oldGuides)
        assertEquals(GuideId.G08_DUE_DATE, GuidePolicy.next(listOf(GuideId.G08_DUE_DATE), state, GuideScene()))
        assertNull(
            GuidePolicy.next(
                listOf(GuideId.G08_DUE_DATE),
                state.copy(shown = oldGuides + GuideId.G08_DUE_DATE),
                GuideScene(),
            ),
        )
    }
}
