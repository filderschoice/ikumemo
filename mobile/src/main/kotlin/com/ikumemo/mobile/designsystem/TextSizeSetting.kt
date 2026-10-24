package com.ikumemo.mobile.designsystem

/**
 * アプリ内の文字の大きさ（PLAN §1.3-6・F-65）。端末の文字サイズに掛け合わせて使う。
 * [storageKey] は設定の保存用で、表示名を変えても保存済みの選択が壊れないよう固定する。
 */
enum class TextSizeSetting(
    val storageKey: String,
    val multiplier: Float,
) {
    NORMAL("normal", 1.0f),
    LARGE("large", 1.15f),
    EXTRA_LARGE("extra_large", 1.3f),
    ;

    companion object {
        /** 端末とアプリの倍率を掛け合わせた上限（PLAN F-65、画面の崩れを防ぐため 200%）。 */
        const val MAX_FONT_SCALE = 2.0f

        /** 端末の文字サイズ [deviceFontScale] にアプリ内設定を掛け、上限で頭打ちにした倍率を返す。 */
        fun effectiveFontScale(
            deviceFontScale: Float,
            setting: TextSizeSetting,
        ): Float = (deviceFontScale * setting.multiplier).coerceAtMost(MAX_FONT_SCALE)

        fun fromStorageKey(key: String?): TextSizeSetting = entries.firstOrNull { it.storageKey == key } ?: NORMAL
    }
}
