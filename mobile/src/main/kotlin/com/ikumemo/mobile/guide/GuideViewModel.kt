package com.ikumemo.mobile.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.mobile.data.GuidePrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ガイドの状態（F-70）。アプリ全体で 1 つ持ち、はじめての案内（G-01）を出すかを決める。
 * 各画面のその場のガイド（BL-096・BL-097）も、ここから出すガイドを決めて「わかった」「ガイドを出さない」を保存する。
 */
class GuideViewModel(
    private val prefs: GuidePrefs,
) : ViewModel() {
    /** 保存値を読み終えるまでは null（起動直後に案内が一瞬見える・見えないのちらつきを防ぐ）。 */
    val state: StateFlow<GuideState?> =
        prefs.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun finishOnboarding() {
        viewModelScope.launch { prefs.finishOnboarding() }
    }

    /** ガイドを「わかった」で閉じた、または先にその操作をした（R-51）。 */
    fun markShown(id: GuideId) {
        viewModelScope.launch { prefs.markShown(id) }
    }

    /** 吹き出しの「ガイドを出さない」（R-53）。いま出しているガイドも出したものとして閉じる。 */
    fun disableAll(current: GuideId) {
        viewModelScope.launch {
            prefs.markShown(current)
            prefs.setDisabled(true)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
