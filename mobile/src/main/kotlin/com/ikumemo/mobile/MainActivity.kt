package com.ikumemo.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ikumemo.mobile.designsystem.IkumemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 35 以降は画面端までの表示が必須（PLAN §4.2）。余白は Scaffold の innerPadding で取る
        enableEdgeToEdge()
        setContent {
            IkumemoTheme {
                IkumemoApp()
            }
        }
    }
}
