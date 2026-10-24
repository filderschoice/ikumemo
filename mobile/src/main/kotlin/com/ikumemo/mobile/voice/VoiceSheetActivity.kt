package com.ikumemo.mobile.voice

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.IkumemoApplication
import com.ikumemo.mobile.MainActivity
import com.ikumemo.mobile.R
import com.ikumemo.mobile.designsystem.AppThemeHost

/**
 * ウィジェット「話して追加」（W-01）から開く音声入力シート。透明な背景でホーム画面の上に重ねて開き、
 * 追加が終わったら閉じてホーム画面に戻る（アプリ本体は開かない、PLAN §2.11）。
 * 外部には公開しない（exported=false）。途中で閉じたら何も追加しない（R-42）。
 */
class VoiceSheetActivity : ComponentActivity() {
    private val viewModel: VoiceInputViewModel by viewModels { AppViewModelFactory.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appearance = (application as IkumemoApplication).container.appearancePrefs.appearance
        setContent {
            AppThemeHost(appearance) {
                VoiceInputSheet(
                    viewModel = viewModel,
                    // マイクが使えないときの「文字で入れる」はアプリの追加シートを開く（R-44）
                    onTypeInstead = {
                        startActivity(MainActivity.intent(this, MainActivity.OPEN_ADD_TEXT))
                        finish()
                    },
                    onAdded = { count ->
                        if (count > 0) {
                            Toast.makeText(
                                this,
                                getString(R.string.widget_voice_added, count),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        finish()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }
}
