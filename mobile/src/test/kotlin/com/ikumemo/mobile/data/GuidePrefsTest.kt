package com.ikumemo.mobile.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.ikumemo.mobile.guide.GuideId
import com.ikumemo.mobile.guide.GuideState
import com.ikumemo.mobile.testing.testDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** ガイドの表示済みの記録と「ガイドを出さない」の保存（R-50〜R-55）。 */
@RunWith(RobolectricTestRunner::class)
class GuidePrefsTest {
    private val dataStore = testDataStore()
    private val prefs = GuidePrefs(dataStore)

    @Test
    fun defaultsShowOnboardingAndAllGuides() =
        runTest {
            assertEquals(GuideState(), prefs.state.first())
        }

    @Test
    fun onboardingShownAndDisabledAreSaved() =
        runTest {
            prefs.finishOnboarding()
            prefs.markShown(GuideId.G02_VOICE_BUTTON)
            prefs.markShown(GuideId.G05_UNDO)
            prefs.setDisabled(true)

            // 作り直しても（アプリを再起動しても）同じ
            val state = GuidePrefs(dataStore).state.first()
            assertTrue(state.onboardingDone)
            assertTrue(state.disabled)
            assertEquals(setOf(GuideId.G02_VOICE_BUTTON, GuideId.G05_UNDO), state.shown)
        }

    @Test
    fun resetShowsGuidesAgainButNotOnboarding() =
        runTest {
            prefs.finishOnboarding()
            prefs.markShown(GuideId.G07_CALENDAR_DAY)
            prefs.setDisabled(true)

            prefs.resetGuides()

            val state = prefs.state.first()
            assertTrue(state.onboardingDone)
            assertFalse(state.disabled)
            assertTrue(state.shown.isEmpty())
        }

    @Test
    fun showGuideAgainResetsOnlyThatGuideAndResumes() =
        runTest {
            prefs.finishOnboarding()
            prefs.markShown(GuideId.G05_UNDO)
            prefs.disableAll(GuideId.G07_CALENDAR_DAY)

            // 使い方の「ガイドをもう一度見る」（H-02）
            prefs.showGuideAgain(GuideId.G07_CALENDAR_DAY)

            val state = prefs.state.first()
            assertEquals(setOf(GuideId.G05_UNDO), state.shown)
            assertFalse(state.disabled)
        }

    @Test
    fun showOnboardingAgainKeepsGuides() =
        runTest {
            prefs.finishOnboarding()
            prefs.markShown(GuideId.G02_VOICE_BUTTON)

            prefs.showOnboardingAgain()

            val state = prefs.state.first()
            assertFalse(state.onboardingDone)
            assertEquals(setOf(GuideId.G02_VOICE_BUTTON), state.shown)
        }

    @Test
    fun unknownGuideKeysAreIgnored() =
        runTest {
            // 新しい版で足したガイドを古い版で読んだ場合
            dataStore.edit { it[stringSetPreferencesKey("guide_shown")] = setOf("g03", "g99") }

            assertEquals(setOf(GuideId.G03_SWIPE_PRACTICE), prefs.state.first().shown)
        }
}
