package com.ikumemo.mobile.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ikumemo.core.share.SharedTextParser
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.IkumemoApplication
import com.ikumemo.mobile.R
import com.ikumemo.mobile.designsystem.AppThemeHost

/**
 * LINE などの共有シートから文を受け取り、メモへ取り込むシート（F-08・D-9、BL-197）。
 *
 * 共有シートに出るため exported=true。受け付けるのは ACTION_SEND の text/plain の EXTRA_TEXT だけで、
 * ほかの Intent・extra は読まずに閉じる。受け取った文は信頼できない外部入力として扱い、ログに出さない。
 */
class ShareReceiveActivity : ComponentActivity() {
    private val viewModel: ShareImportViewModel by viewModels { AppViewModelFactory.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SharedIntent.isAccepted(intent)) {
            finish()
            return
        }
        viewModel.load(SharedIntent.text(intent))
        val appearance = (application as IkumemoApplication).container.appearancePrefs.appearance
        setContent {
            AppThemeHost(appearance) {
                ShareImportSheet(
                    viewModel = viewModel,
                    onAdded = { count ->
                        if (count > 0) {
                            val message = getString(R.string.widget_voice_added, count)
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }
}

/** 共有で受け取った Intent の読み方。受け付ける形を 1 か所で決める。 */
object SharedIntent {
    private const val TEXT_PLAIN = "text/plain"

    /** ACTION_SEND で、種類が text/plain のときだけ受け付ける。 */
    fun isAccepted(intent: Intent?): Boolean =
        intent?.action == Intent.ACTION_SEND &&
            intent.type
                ?.substringBefore(';')
                ?.trim()
                .equals(TEXT_PLAIN, ignoreCase = true)

    /**
     * EXTRA_TEXT を上限の長さまで読む。無い・読めない（壊れた extra）ときは null。
     * 長すぎる文は先頭の [SharedTextParser.MAX_TEXT_LENGTH] 文字だけを文字列にする。
     */
    fun text(intent: Intent): String? =
        try {
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.let { text ->
                text.subSequence(0, minOf(text.length, SharedTextParser.MAX_TEXT_LENGTH)).toString()
            }
        } catch (_: RuntimeException) {
            // 壊れた Bundle（BadParcelableException など）でも落とさず、空の文として扱う
            null
        }
}
