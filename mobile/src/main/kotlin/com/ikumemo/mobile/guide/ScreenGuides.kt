package com.ikumemo.mobile.guide

import com.ikumemo.mobile.data.GuidePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * カレンダー・おしらせ画面のその場のガイド（G-07・G-08）の保存と判定。[prefs] が無ければ何も出さない（テスト用）。
 * 出すかの判定は [GuidePolicy.next]、保存は [GuidePrefs]。
 */
class ScreenGuides(
    private val prefs: GuidePrefs?,
    private val scope: CoroutineScope,
) {
    val state: Flow<GuideState> = prefs?.state ?: flowOf(GuideState(disabled = true))

    fun next(
        candidates: List<GuideId>,
        state: GuideState,
        scene: GuideScene,
    ): GuideId? = GuidePolicy.next(candidates, state, scene)

    /** 「わかった」で閉じた、または先にその操作をした（R-51）。 */
    fun markShown(id: GuideId) {
        val prefs = prefs ?: return
        scope.launch { prefs.markShown(id) }
    }

    /** 「ガイドを出さない」（R-53）。 */
    fun disableAll(id: GuideId) {
        val prefs = prefs ?: return
        scope.launch { prefs.disableAll(id) }
    }
}
