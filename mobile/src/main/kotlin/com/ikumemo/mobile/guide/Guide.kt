package com.ikumemo.mobile.guide

/**
 * その場のガイド（F-70、PLAN §2.12 ②）。[storageKey] で表示済みを保存するため、更新で新しいガイドを足しても
 * 既存の利用者には「まだ出していない」ものとして、初めてその場面に来たときに 1 回だけ出る（R-55）。
 * 吹き出しの文と照らす場所は各画面のガイド（BL-096・BL-097）で決める。
 */
enum class GuideId(
    val storageKey: String,
) {
    /** メモが空のホームを初めて開いた → 🎤 ボタン。 */
    G02_VOICE_BUTTON("g02"),

    /** 初めてメモに項目が入った → 「れんしゅう」項目でスワイプ。 */
    G03_SWIPE_PRACTICE("g03"),

    /** G-03 を終えた／とばした → 項目左の ○。 */
    G04_BOUGHT_BUTTON("g04"),

    /** 初めて「買った」にした → 「元に戻す」。 */
    G05_UNDO("g05"),

    /** 2 回目の「買った」の前（お店未選択） → お店チップ。 */
    G06_STORE_CHIP("g06"),

    /** 初めてカレンダーを開いた → 買い物のあった日。 */
    G07_CALENDAR_DAY("g07"),

    /** 初めて「買った」の後におしらせを開いた → 「期限を決める」。 */
    G08_DUE_DATE("g08"),
    ;

    companion object {
        fun fromStorageKey(key: String): GuideId? = entries.firstOrNull { it.storageKey == key }
    }
}

/** ガイドの保存状態。 */
data class GuideState(
    /** はじめての案内（G-01）を見終えたか、とばしたか。 */
    val onboardingDone: Boolean = false,
    /** 「ガイドを出さない」を選んだか（R-53。使い方から再開できる）。 */
    val disabled: Boolean = false,
    /** 出したガイド（「わかった」で閉じたもの・先にその操作をしたもの R-51）。 */
    val shown: Set<GuideId> = emptySet(),
)

/** ガイドを出してよいかに関わる、いまの画面の様子（R-52）。 */
data class GuideScene(
    /** 音声入力中。 */
    val listening: Boolean = false,
    /** シート・ダイアログを出している。 */
    val sheetOpen: Boolean = false,
    /** 通知・ウィジェット・ショートカットから開いた直後。 */
    val openedFromOutside: Boolean = false,
)

/** その場のガイドを出すかの判定（R-50・R-52・R-53・R-55）。画面の状態を持たない純粋な関数にしてテストする。 */
object GuidePolicy {
    /**
     * [candidates] はいまの場面できっかけを満たしているガイド（優先する順）。まだ出していない最初の 1 つだけを返す
     * （1 つの画面で同時に 2 つ以上は出さず、残りは次の機会に回す R-50）。「ガイドを出さない」を選んでいる、はじめての案内の途中、
     * 音声入力中・シート表示中・外から開いた直後（R-52）なら何も出さない。
     */
    fun next(
        candidates: List<GuideId>,
        state: GuideState,
        scene: GuideScene,
    ): GuideId? {
        val blocked =
            state.disabled || !state.onboardingDone || scene.listening || scene.sheetOpen || scene.openedFromOutside
        return if (blocked) null else candidates.firstOrNull { it !in state.shown }
    }
}
