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
import com.ikumemo.mobile.designsystem.AppThemeHost

class MainActivity : ComponentActivity() {
    /** 通知・ウィジェット・ショートカットから開く画面。開いたら null に戻す。 */
    private var requestedScreen by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedScreen = intent?.getStringExtra(EXTRA_OPEN_TAB)
        // targetSdk 35 以降は画面端までの表示が必須（PLAN §4.2）。余白は Scaffold の innerPadding で取る
        enableEdgeToEdge()
        val appearance = (application as IkumemoApplication).container.appearancePrefs.appearance
        setContent {
            AppThemeHost(appearance) {
                IkumemoApp(requestedTab = requestedScreen, onTabOpened = { requestedScreen = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestedScreen = intent.getStringExtra(EXTRA_OPEN_TAB)
    }

    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"
        const val TAB_REMINDER = "reminder"
        const val TAB_CALENDAR = "calendar"

        /** メモ画面を開いて「文字で追加」のシートを出す（R-44・R-45）。 */
        const val OPEN_ADD_TEXT = "add_text"

        /** メモ画面を開いて「話して追加」を始める（F-74）。 */
        const val OPEN_VOICE = "voice"

        fun intent(
            context: Context,
            screen: String,
        ): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_TAB, screen)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
