package com.ikumemo.mobile.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.ikumemo.mobile.guide.GuideId
import com.ikumemo.mobile.guide.GuideState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ガイドの表示済みの記録と「ガイドを出さない」（R-50〜R-55、PLAN §4.3 DataStore）。[AppSettings] と同じ DataStore。
 * 表示済みはガイドの `storageKey` の集合で持ち、知らないキー（新しい版で足したガイドを古い版で読んだ場合）は無視する。
 */
class GuidePrefs(
    private val dataStore: DataStore<Preferences>,
) {
    val state: Flow<GuideState> =
        dataStore.data.map { prefs ->
            GuideState(
                onboardingDone = prefs[ONBOARDING_DONE] ?: false,
                disabled = prefs[DISABLED] ?: false,
                shown = prefs[SHOWN].orEmpty().mapNotNull(GuideId::fromStorageKey).toSet(),
            )
        }

    /** はじめての案内（G-01）を見終えた・とばした。 */
    suspend fun finishOnboarding() {
        dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    /** ガイドを出した、または先にその操作をした（R-51）。以後そのガイドは出さない。 */
    suspend fun markShown(id: GuideId) {
        dataStore.edit { it[SHOWN] = it[SHOWN].orEmpty() + id.storageKey }
    }

    /** 「ガイドを出さない」（R-53）。false で再開する（使い方から）。 */
    suspend fun setDisabled(disabled: Boolean) {
        dataStore.edit { it[DISABLED] = disabled }
    }

    /** 吹き出しの「ガイドを出さない」（R-53）。いま出しているガイドも出したことにし、まとめて 1 回で保存する。 */
    suspend fun disableAll(current: GuideId) {
        dataStore.edit {
            it[SHOWN] = it[SHOWN].orEmpty() + current.storageKey
            it[DISABLED] = true
        }
    }

    /** 使い方の各ページの「ガイドをもう一度見る」（H-02）。そのガイドを未表示に戻し、止めていたガイドも再開する。 */
    suspend fun showGuideAgain(id: GuideId) {
        dataStore.edit {
            it[SHOWN] = it[SHOWN].orEmpty() - id.storageKey
            it[DISABLED] = false
        }
    }

    /** 使い方の「はじめての案内をもう一度見る」（H-02）。 */
    suspend fun showOnboardingAgain() {
        dataStore.edit { it[ONBOARDING_DONE] = false }
    }

    /** 使い方の「ガイドをもう一度見る」用。表示済みを消し、止めていたガイドも再開する（はじめての案内は出し直さない）。 */
    suspend fun resetGuides() {
        dataStore.edit {
            it.remove(SHOWN)
            it[DISABLED] = false
        }
    }

    private companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("guide_onboarding_done")
        val DISABLED = booleanPreferencesKey("guide_disabled")
        val SHOWN = stringSetPreferencesKey("guide_shown")
    }
}
