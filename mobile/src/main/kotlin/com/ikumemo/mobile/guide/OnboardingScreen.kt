package com.ikumemo.mobile.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ikumemo.mobile.R
import com.ikumemo.mobile.widget.PinnableWidget

/** はじめての案内（G-01）の各ページ。1 枚 1 メッセージ、文は 2 行以内（PLAN §2.12 ①）。 */
enum class OnboardingPage(
    val emoji: String,
    val title: Int,
    val body: Int,
) {
    VOICE("🎤", R.string.onboarding_voice_title, R.string.onboarding_voice_body),
    SWIPE("👉", R.string.onboarding_swipe_title, R.string.onboarding_swipe_body),
    LOOK_BACK("📅", R.string.onboarding_look_back_title, R.string.onboarding_look_back_body),
    HOME_SCREEN("🏠", R.string.onboarding_home_title, R.string.onboarding_home_body),
}

/**
 * はじめての案内（F-70・G-01）。スライド 3 枚と、最後に「ホーム画面に置く」（W-03 の「置く」「あとで」）。
 * 「とばす」は常に出す。権限はここでは求めない（使う直前に求める、PLAN §1.3）。テーマ選びは含めない。
 * [onFinish] は最後まで見た・とばした・「あとで」のどれでも呼ぶ。「置く」はホーム画面アプリの確認を出すだけで、案内は閉じない。
 */
@Composable
fun OnboardingContent(
    pinSupported: Boolean,
    onPin: (PinnableWidget) -> Unit,
    onFinish: () -> Unit,
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val page = OnboardingPage.entries[index]
    val last = index == OnboardingPage.entries.lastIndex
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(OnboardingTestTags.SKIP),
                ) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // 1 枚目は「牛乳とたまご」が 2 つに分かれる手本を動かす（アニメーションを減らす設定では止める）
                if (page == OnboardingPage.VOICE) VoiceSplitDemo() else Text(page.emoji, fontSize = EMOJI_SP.sp)
                Spacer(Modifier.size(24.dp))
                Text(
                    stringResource(page.title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(OnboardingTestTags.page(page)),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                if (last) {
                    Spacer(Modifier.size(24.dp))
                    PinButtons(pinSupported = pinSupported, onPin = onPin)
                }
            }
            PageDots(index = index)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { if (last) onFinish() else index++ },
                    modifier = Modifier.heightIn(min = 48.dp).testTag(OnboardingTestTags.NEXT),
                ) {
                    Text(stringResource(if (last) R.string.onboarding_later else R.string.onboarding_next))
                }
            }
        }
    }
}

@Composable
private fun PinButtons(
    pinSupported: Boolean,
    onPin: (PinnableWidget) -> Unit,
) {
    if (!pinSupported) {
        // 置く操作に対応していないホーム画面アプリでは、置き方を説明する
        Text(
            stringResource(R.string.home_screen_manual),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        return
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PinnableWidget.entries.forEach { widget ->
            OutlinedButton(
                onClick = { onPin(widget) },
                modifier = Modifier.heightIn(min = 48.dp).testTag(OnboardingTestTags.pin(widget)),
            ) {
                Text(stringResource(widget.pinLabel()))
            }
        }
    }
}

private fun PinnableWidget.pinLabel(): Int =
    when (this) {
        PinnableWidget.VOICE -> R.string.onboarding_pin_voice
        PinnableWidget.MEMO_LIST -> R.string.onboarding_pin_memo
    }

/** いま何枚目か（● ○ ○ ○）。読み上げでは「1 / 4 枚目」と読む。 */
@Composable
private fun PageDots(index: Int) {
    val total = OnboardingPage.entries.size
    val description = stringResource(R.string.onboarding_page_position, index + 1, total)
    Row(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(total) { i ->
            val color = if (i == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(Modifier.size(8.dp).background(color, CircleShape))
        }
    }
}

private const val EMOJI_SP = 72

object OnboardingTestTags {
    const val SKIP = "onboarding_skip"
    const val NEXT = "onboarding_next"

    fun page(page: OnboardingPage) = "onboarding_page_${page.name.lowercase()}"

    fun pin(widget: PinnableWidget) = "onboarding_pin_${widget.name.lowercase()}"
}
