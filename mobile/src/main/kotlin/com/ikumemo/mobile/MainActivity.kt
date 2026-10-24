package com.ikumemo.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ikumemo.mobile.designsystem.IkumemoTheme

class MainActivity : ComponentActivity() {
    /** 通知などから開くタブ。開いたら null に戻す。 */
    private var requestedTab by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedTab = intent?.getStringExtra(EXTRA_OPEN_TAB)
        // targetSdk 35 以降は画面端までの表示が必須（PLAN §4.2）。余白は Scaffold の innerPadding で取る
        enableEdgeToEdge()
        val appearancePrefs = (application as IkumemoApplication).container.appearancePrefs
        setContent {
            val appearance by appearancePrefs.appearance.collectAsStateWithLifecycle(initialValue = null)
            // 保存した見た目を読み終えるまでは描かない（起動直後に既定のテーマが一瞬見えるのを防ぐ）
            val current = appearance ?: return@setContent
            IkumemoTheme(
                preset = current.theme,
                darkTheme = current.darkMode.isDark(isSystemInDarkTheme()),
                textSize = current.textSize,
            ) {
                IkumemoApp(requestedTab = requestedTab, onTabOpened = { requestedTab = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestedTab = intent.getStringExtra(EXTRA_OPEN_TAB)
    }

    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"
        const val TAB_REMINDER = "reminder"
    }
}
