package com.ikumemo.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ikumemo.mobile.designsystem.IkumemoTheme

class MainActivity : ComponentActivity() {
    /** 通知などから開くタブ。開いたら null に戻す。 */
    private var requestedTab by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedTab = intent?.getStringExtra(EXTRA_OPEN_TAB)
        // targetSdk 35 以降は画面端までの表示が必須（PLAN §4.2）。余白は Scaffold の innerPadding で取る
        enableEdgeToEdge()
        setContent {
            IkumemoTheme {
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
