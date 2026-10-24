package com.ikumemo.mobile.designsystem

/** 暗い画面の設定（F-63「端末に合わせる / 明るい / 暗い」）。 */
enum class DarkMode(
    val storageKey: String,
) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    fun isDark(systemDark: Boolean): Boolean =
        when (this) {
            SYSTEM -> systemDark
            LIGHT -> false
            DARK -> true
        }

    companion object {
        fun fromStorageKey(key: String?): DarkMode = entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}
