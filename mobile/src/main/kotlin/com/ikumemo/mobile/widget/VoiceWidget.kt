package com.ikumemo.mobile.widget

import android.content.Context
import android.content.Intent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ikumemo.mobile.MainActivity
import com.ikumemo.mobile.R
import com.ikumemo.mobile.voice.VoiceSheetActivity

/**
 * 1×1 ウィジェット「話して追加」（W-01）。全体が 1 つのボタンで、押すと音声入力シートが重なって開く。
 * 横に 2 マス以上あるときは「🎤 話して追加」と文字を横に並べる。音声入力が使えない端末では「書いて追加」として
 * アプリの追加シートを開く（R-45）。
 */
class VoiceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val colors = WidgetColors.load(context)
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        provideContent { VoiceWidgetContent(colors, available, launchIntent(context, available)) }
    }

    companion object {
        val SMALL = DpSize(40.dp, 40.dp)
        val WIDE = DpSize(110.dp, 40.dp)

        /** 押したときに開く画面（音声入力シート、または音声が使えない端末ではアプリの追加シート）。 */
        fun launchIntent(
            context: Context,
            voiceAvailable: Boolean,
        ): Intent =
            if (voiceAvailable) {
                Intent(context, VoiceSheetActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                MainActivity.intent(context, MainActivity.OPEN_ADD_TEXT)
            }
    }
}

@Composable
private fun VoiceWidgetContent(
    colors: WidgetColors,
    available: Boolean,
    intent: Intent,
) {
    val wide = LocalSize.current.width >= VoiceWidget.WIDE.width
    val icon = if (available) R.drawable.ic_widget_mic else R.drawable.ic_widget_keyboard
    val label =
        if (wide) {
            if (available) R.string.widget_voice_label_wide else R.string.widget_write_label
        } else {
            if (available) R.string.widget_voice_label else R.string.widget_write_label_short
        }
    val description =
        LocalContext.current.getString(
            if (available) R.string.widget_voice_description else R.string.widget_write_description,
        )
    val modifier =
        GlanceModifier
            .fillMaxSize()
            .background(colors.primary)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity(intent))
            .semantics { contentDescription = description }
    val text = LocalContext.current.getString(label)
    if (wide) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                ImageProvider(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.onPrimary),
                modifier = GlanceModifier.size(28.dp),
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(text, style = TextStyle(color = colors.onPrimary), maxLines = 1)
        }
    } else {
        Column(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                ImageProvider(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.onPrimary),
                modifier = GlanceModifier.size(28.dp),
            )
            Text(text, style = TextStyle(color = colors.onPrimary), maxLines = 1)
        }
    }
}

class VoiceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VoiceWidget()
}
