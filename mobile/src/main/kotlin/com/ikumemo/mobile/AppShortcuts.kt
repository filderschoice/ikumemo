package com.ikumemo.mobile

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * アイコン長押しのショートカット（F-74）。「話して追加」「文字で追加」「記録」「期限」の 4 つで、
 * それぞれ [MainActivity.intent] の画面を開く（「話して追加」はメモ画面を開いて音声入力を始める）。
 *
 * debug 版は applicationId が違い、静的なショートカット定義（shortcuts.xml）にはパッケージ名を固定で書くしかないため、
 * 起動時に動的なショートカットとして登録する。ホーム画面アプリが対応していれば、ホーム画面へ単独で置ける。
 */
object AppShortcuts {
    private enum class Entry(
        val screen: String,
        @StringRes val shortLabel: Int,
        @StringRes val longLabel: Int,
        @DrawableRes val icon: Int,
    ) {
        VOICE(
            MainActivity.OPEN_VOICE,
            R.string.shortcut_voice,
            R.string.shortcut_voice_long,
            R.drawable.ic_shortcut_voice,
        ),
        TEXT(
            MainActivity.OPEN_ADD_TEXT,
            R.string.shortcut_text,
            R.string.shortcut_text_long,
            R.drawable.ic_shortcut_text,
        ),
        CALENDAR(
            MainActivity.TAB_CALENDAR,
            R.string.shortcut_calendar,
            R.string.shortcut_calendar_long,
            R.drawable.ic_shortcut_calendar,
        ),
        REMINDER(
            MainActivity.TAB_REMINDER,
            R.string.shortcut_reminder,
            R.string.shortcut_reminder_long,
            R.drawable.ic_shortcut_reminder,
        ),
    }

    /** 登録するショートカット（長押しで並ぶ順）。 */
    fun create(context: Context): List<ShortcutInfoCompat> =
        Entry.entries.mapIndexed { rank, entry ->
            ShortcutInfoCompat
                .Builder(context, entry.screen)
                .setShortLabel(context.getString(entry.shortLabel))
                .setLongLabel(context.getString(entry.longLabel))
                .setIcon(IconCompat.createWithResource(context, entry.icon))
                .setIntent(MainActivity.intent(context, entry.screen).setAction(Intent.ACTION_VIEW))
                .setRank(rank)
                .build()
        }

    /** 起動時に登録し直す（文言を変えた版へ更新したときも反映されるように）。 */
    fun publish(context: Context) {
        ShortcutManagerCompat.setDynamicShortcuts(context, create(context))
    }
}
