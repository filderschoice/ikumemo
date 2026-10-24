package com.ikumemo.mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.ikumemo.mobile.designsystem.AppThemeHost

class MainActivity : ComponentActivity() {
    /** 通知・ウィジェット・ショートカットから開く画面。開いたら null に戻す。 */
    private var requestedScreen by mutableStateOf<ScreenRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedScreen = intent?.let(ScreenRequest::from)
        // targetSdk 35 以降は画面端までの表示が必須（PLAN §4.2）。余白は Scaffold の innerPadding で取る
        enableEdgeToEdge()
        val appearance = (application as IkumemoApplication).container.appearancePrefs.appearance
        setContent {
            AppThemeHost(appearance) {
                IkumemoApp(requestedScreen = requestedScreen, onTabOpened = { requestedScreen = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestedScreen = ScreenRequest.from(intent)
    }

    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"
        const val EXTRA_ITEM_ID = "item_id"
        const val OPEN_MEMO = "memo"
        const val TAB_REMINDER = "reminder"
        const val TAB_CALENDAR = "calendar"

        /** メモ画面を開いて「文字で追加」のシートを出す（R-44・R-45）。 */
        const val OPEN_ADD_TEXT = "add_text"

        /** メモ画面を開いて「話して追加」を始める（F-74）。 */
        const val OPEN_VOICE = "voice"

        /** メモ画面を開いて [EXTRA_ITEM_ID] の項目まで送り、一瞬光らせる（R-40）。 */
        const val OPEN_ITEM = "item"

        /** 開く画面ごとに別の Intent になるよう data に画面名を入れる（PendingIntent が上書きし合わないように）。 */
        fun intent(
            context: Context,
            screen: String,
        ): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_TAB, screen)
                .setData("ikumemo://open/$screen".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        /** メモの項目を開く。項目ごとに別の Intent になるよう data に ID も入れる。 */
        fun itemIntent(
            context: Context,
            itemId: Long,
        ): Intent =
            intent(context, OPEN_ITEM)
                .putExtra(EXTRA_ITEM_ID, itemId)
                .setData("ikumemo://open/$OPEN_ITEM/$itemId".toUri())
    }
}
