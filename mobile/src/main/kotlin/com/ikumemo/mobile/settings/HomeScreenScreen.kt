package com.ikumemo.mobile.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import com.ikumemo.mobile.widget.PinnableWidget
import com.ikumemo.mobile.widget.WidgetPinner

@Composable
fun HomeScreenScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val pinner = remember(context) { WidgetPinner(context) }
    SettingsScaffold(title = R.string.settings_home_screen, onBack = onBack) {
        HomeScreenContent(pinSupported = pinner.isSupported, onPin = { pinner.pin(it) })
    }
}

/**
 * ホーム画面に置く（W-03）。ウィジェット 2 種の見本と「置く」。メモ一覧は 4×4 で置き、4×2 へは置いた後に長押しで
 * 小さくできることを添える。ホーム画面アプリが置く操作に対応していなければ、ボタンの代わりに置き方を説明する。
 * 最後にアイコン長押しのショートカット（F-74）を案内する。
 */
@Composable
fun HomeScreenContent(
    pinSupported: Boolean,
    onPin: (PinnableWidget) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WidgetSample.entries.forEach { sample ->
            WidgetCard(sample = sample, pinSupported = pinSupported, onPin = onPin)
        }
        if (!pinSupported) {
            Text(stringResource(R.string.home_screen_manual), style = MaterialTheme.typography.bodyMedium)
        }
        Text(stringResource(R.string.home_screen_shortcuts_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.home_screen_shortcuts), style = MaterialTheme.typography.bodyMedium)
    }
}

/** ウィジェットの見本 1 枚分の表示内容。 */
private enum class WidgetSample(
    val icon: ImageVector,
    val title: Int,
    val detail: Int,
    val widget: PinnableWidget,
) {
    VOICE(Icons.Rounded.Mic, R.string.home_screen_voice, R.string.home_screen_voice_detail, PinnableWidget.VOICE),
    MEMO_LIST(
        Icons.AutoMirrored.Rounded.List,
        R.string.home_screen_memo,
        R.string.home_screen_memo_detail,
        PinnableWidget.MEMO_LIST,
    ),
}

@Composable
private fun WidgetCard(
    sample: WidgetSample,
    pinSupported: Boolean,
    onPin: (PinnableWidget) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(sample.icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(sample.title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(sample.detail), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (pinSupported) {
            Button(
                onClick = { onPin(sample.widget) },
                modifier =
                    Modifier
                        .align(Alignment.End)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .heightIn(min = 48.dp)
                        .testTag(HomeScreenTestTags.pin(sample.widget)),
            ) {
                Text(stringResource(R.string.home_screen_pin))
            }
        }
    }
}

object HomeScreenTestTags {
    fun pin(widget: PinnableWidget) = "home_screen_pin_${widget.name.lowercase()}"
}
