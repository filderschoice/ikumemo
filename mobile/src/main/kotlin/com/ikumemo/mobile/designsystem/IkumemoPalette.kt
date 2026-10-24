package com.ikumemo.mobile.designsystem

/**
 * 1 つのテーマ・1 つの明るさ（ライト／ダーク）の配色。
 *
 * 色は ARGB の [Long]（例 `0xFFB83B5E`）で持つ。Compose の `Color` に依存させないことで、
 * コントラスト比の検査（[ContrastRatio]）を Android 実行環境なしの単体テストで行えるようにする。
 * 名前は Material 3 の ColorScheme の役割名にそろえる（[toColorScheme] でそのまま対応付ける）。
 */
data class IkumemoPalette(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val onSecondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val onTertiary: Long,
    val tertiaryContainer: Long,
    val onTertiaryContainer: Long,
    val background: Long,
    val onBackground: Long,
    val surface: Long,
    val onSurface: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
    val error: Long = 0xFFBA1A1A,
    val onError: Long = 0xFFFFFFFF,
    val errorContainer: Long = 0xFFFFDAD6,
    val onErrorContainer: Long = 0xFF410002,
) {
    /**
     * 文字（前景）と背景の組み合わせ。PLAN §1.3-7 の「コントラスト比 4.5:1 以上」を検査する対象。
     * 補助的な文字（onSurfaceVariant）は、カードの上だけでなく画面の背景の上にも置くため両方を含める。
     */
    fun textPairs(): Map<String, Pair<Long, Long>> =
        mapOf(
            "onPrimary/primary" to (onPrimary to primary),
            "onPrimaryContainer/primaryContainer" to (onPrimaryContainer to primaryContainer),
            "onSecondary/secondary" to (onSecondary to secondary),
            "onSecondaryContainer/secondaryContainer" to (onSecondaryContainer to secondaryContainer),
            "onTertiary/tertiary" to (onTertiary to tertiary),
            "onTertiaryContainer/tertiaryContainer" to (onTertiaryContainer to tertiaryContainer),
            "onBackground/background" to (onBackground to background),
            "onSurface/surface" to (onSurface to surface),
            "onSurfaceVariant/surfaceVariant" to (onSurfaceVariant to surfaceVariant),
            "onSurfaceVariant/surface" to (onSurfaceVariant to surface),
            "onSurfaceVariant/background" to (onSurfaceVariant to background),
            "primary/surface" to (primary to surface),
            "onError/error" to (onError to error),
            "onErrorContainer/errorContainer" to (onErrorContainer to errorContainer),
            "error/surface" to (error to surface),
        )
}

/** ダークテーマ共通のエラー色（Material 3 の基準値）。 */
internal fun IkumemoPalette.withDarkError(): IkumemoPalette =
    copy(
        error = 0xFFFFB4AB,
        onError = 0xFF690005,
        errorContainer = 0xFF93000A,
        onErrorContainer = 0xFFFFDAD6,
    )
