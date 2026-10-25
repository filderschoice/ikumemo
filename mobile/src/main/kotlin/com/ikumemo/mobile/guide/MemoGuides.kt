package com.ikumemo.mobile.guide

import com.ikumemo.mobile.data.db.ShoppingItemEntity
import java.time.Instant

/** メモ画面のその場のガイド（G-02〜G-06）のきっかけに使う、いまの様子。 */
data class MemoGuideFacts(
    val loaded: Boolean,
    /** メモに入っている項目の数（れんしゅう項目は数えない）。 */
    val itemCount: Int,
    /** 「元に戻す」に出しているのが「買った」か。 */
    val undoingBought: Boolean,
    val storeSelected: Boolean,
    val showBoughtButton: Boolean,
)

/** メモ画面のガイドのきっかけ（PLAN §2.12 ② の表）。優先する順に並べ、出す 1 つは [GuidePolicy.next] が決める。 */
object MemoGuides {
    /** 「れんしゅう：にんじん」項目（R-54）の ID。DB の ID（1 から）と重ならない。 */
    const val PRACTICE_ITEM_ID = -1L

    /** 画面に出すだけの「れんしゅう」項目。品目（候補）にも DB にも入れない。 */
    fun practiceItem(name: String): ShoppingItemEntity =
        ShoppingItemEntity(
            id = PRACTICE_ITEM_ID,
            productId = PRACTICE_ITEM_ID,
            displayName = name,
            sortOrder = -1,
            createdAt = Instant.EPOCH,
        )

    /** れんしゅう項目を出すか。初めてメモに項目が入ってから、練習を終えるかとばす（G-03 を出し終える）まで。 */
    fun showPractice(
        facts: MemoGuideFacts,
        state: GuideState,
    ): Boolean =
        facts.loaded &&
            facts.itemCount > 0 &&
            state.onboardingDone &&
            !state.disabled &&
            GuideId.G03_SWIPE_PRACTICE !in state.shown

    fun candidates(
        facts: MemoGuideFacts,
        state: GuideState,
    ): List<GuideId> {
        if (!facts.loaded) return emptyList()
        val shown = state.shown
        return buildList {
            // G-05: 初めて「買った」にした直後の「元に戻す」
            if (facts.undoingBought) add(GuideId.G05_UNDO)
            if (facts.itemCount > 0) {
                add(GuideId.G03_SWIPE_PRACTICE)
                // G-04: 練習を終えた／とばした後。○ を出さない設定なら照らす物が無いので出さない
                if (GuideId.G03_SWIPE_PRACTICE in shown && facts.showBoughtButton) add(GuideId.G04_BOUGHT_BUTTON)
                // G-06: 1 回「買った」にした後（G-05 を出し終えた後）、お店を選ばずに 2 回目を買おうとしている
                if (GuideId.G05_UNDO in shown && !facts.storeSelected) add(GuideId.G06_STORE_CHIP)
            } else {
                add(GuideId.G02_VOICE_BUTTON)
            }
        }
    }
}
