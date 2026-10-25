package com.ikumemo.mobile.shoppinglist

import com.ikumemo.mobile.data.GuidePrefs
import com.ikumemo.mobile.data.UndoableAction
import com.ikumemo.mobile.guide.GuideId
import com.ikumemo.mobile.guide.GuidePolicy
import com.ikumemo.mobile.guide.GuideScene
import com.ikumemo.mobile.guide.GuideState
import com.ikumemo.mobile.guide.MemoGuideFacts
import com.ikumemo.mobile.guide.MemoGuides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * メモ画面のその場のガイド（G-02〜G-06）と「れんしゅう」項目（R-54）。[prefs] が無ければ何も出さない。
 * 画面の操作は先に [handle] へ渡し、ガイドの操作・れんしゅう項目の操作はここで済ませる。
 */
internal class MemoGuideController(
    private val prefs: GuidePrefs?,
    private val scope: CoroutineScope,
) {
    val state: Flow<GuideState> = prefs?.state ?: flowOf(GuideState(disabled = true))

    /** 出すガイドとれんしゅう項目を決める（R-50・R-52・R-54）。 */
    fun decide(
        ui: ShoppingListUiState,
        guides: GuideState,
    ): ShoppingListUiState {
        val facts =
            MemoGuideFacts(
                loaded = ui.loaded,
                itemCount = ui.items.size,
                undoingBought = ui.undo?.action is UndoableAction.Bought,
                storeSelected = ui.selectedStore != null,
                showBoughtButton = ui.operation.showBoughtButton,
            )
        // シート（音声入力を含む）を出している間・ウィジェットの行から開いて光らせている間は出さない（R-52）
        val scene = GuideScene(sheetOpen = ui.sheet != null, openedFromOutside = ui.highlightedItemId != null)
        return ui.copy(
            guide = GuidePolicy.next(MemoGuides.candidates(facts, guides), guides, scene),
            practice = MemoGuides.showPractice(facts, guides),
        )
    }

    /**
     * ガイドの操作と、れんしゅう項目の「買った」「やめた」を扱う。扱ったら true（呼び出し側では何もしない）。
     * ほかの操作は、ガイドより先にその機能を使ったものとして記録し（R-51）、false を返す。
     */
    fun handle(event: ShoppingListEvent): Boolean {
        val practice =
            (event as? ShoppingListEvent.MarkBought)?.itemId == MemoGuides.PRACTICE_ITEM_ID ||
                (event as? ShoppingListEvent.Dismiss)?.itemId == MemoGuides.PRACTICE_ITEM_ID
        when {
            event is ShoppingListEvent.GuideDismissed -> markShown(event.guide)
            event is ShoppingListEvent.GuideDisableAll -> scope.launch { prefs?.disableAll(event.guide) }
            // れんしゅうを動かしたら練習を終える。記録・品目（候補）には残さず、元に戻すの対象にもしない（R-54）
            practice -> markShown(GuideId.G03_SWIPE_PRACTICE)
            else -> {
                usedFeature(event)?.let(::markShown)
                return false
            }
        }
        return true
    }

    /** その操作をしたら出さなくてよいガイド（R-51）。 */
    private fun usedFeature(event: ShoppingListEvent): GuideId? =
        when (event) {
            ShoppingListEvent.OpenVoice -> GuideId.G02_VOICE_BUTTON
            // 自分で項目を動かせたので、れんしゅうは要らない
            is ShoppingListEvent.MarkBought, is ShoppingListEvent.Dismiss -> GuideId.G03_SWIPE_PRACTICE
            is ShoppingListEvent.Undo -> GuideId.G05_UNDO
            ShoppingListEvent.OpenStorePicker -> GuideId.G06_STORE_CHIP
            else -> null
        }

    private fun markShown(id: GuideId) {
        val prefs = prefs ?: return
        scope.launch { prefs.markShown(id) }
    }
}
