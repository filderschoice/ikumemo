package com.ikumemo.mobile.guide

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ikumemo.mobile.R
import kotlinx.coroutines.delay

/**
 * 端末の「アニメーションを減らす」（開発者向けオプションやユーザー補助でアニメーションの長さを 0 にした状態）か（R-56）。
 * このときは手本のアニメーションを止め、最後の形を静止画として見せる。
 */
fun Context.isReducedMotion(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * はじめての案内 1 枚目の手本（PLAN §2.12 ①）。「牛乳とたまご」と話した文が、2 つの品目に分かれて入る様子を
 * [DEMO_CYCLES] 回くり返し、分かれた形で止まる（いつまでも動き続けない）。[reduceMotion] なら最初から分かれた形で止める。
 */
@Composable
fun VoiceSplitDemo(reduceMotion: Boolean = LocalContext.current.isReducedMotion()) {
    var split by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            split = true
            return@LaunchedEffect
        }
        repeat(DEMO_CYCLES) {
            split = false
            delay(SPOKEN_MILLIS)
            split = true
            delay(SPLIT_MILLIS)
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.heightIn(min = DEMO_HEIGHT),
    ) {
        Text("🎤", fontSize = EMOJI_SP.sp)
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(visible = !split, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    stringResource(R.string.onboarding_demo_spoken),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag(GuideDemoTags.SPOKEN),
                )
            }
            androidx.compose.animation.AnimatedVisibility(visible = split, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag(GuideDemoTags.SPLIT),
                ) {
                    DemoChip(stringResource(R.string.onboarding_demo_item1))
                    DemoChip(stringResource(R.string.onboarding_demo_item2))
                }
            }
        }
    }
}

@Composable
private fun DemoChip(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/**
 * G-03 のスワイプの手本（PLAN §2.12 ②）。[active] の間、中身を右へ少し動かして戻すのを [NUDGE_CYCLES] 回見せて止まる。
 * [reduceMotion] なら動かさない。
 */
@Composable
fun SwipeNudge(
    active: Boolean,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = LocalContext.current.isReducedMotion(),
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(active, reduceMotion) {
        if (!active || reduceMotion) {
            offset.snapTo(0f)
            return@LaunchedEffect
        }
        repeat(NUDGE_CYCLES) {
            offset.animateTo(NUDGE_DP, tween(NUDGE_MILLIS))
            offset.animateTo(0f, tween(NUDGE_MILLIS))
            delay(NUDGE_PAUSE_MILLIS)
        }
    }
    Box(modifier.offset { IntOffset(offset.value.dp.roundToPx(), 0) }) { content() }
}

private const val DEMO_CYCLES = 3
private const val SPOKEN_MILLIS = 1_200L
private const val SPLIT_MILLIS = 1_600L
private const val EMOJI_SP = 72
private val DEMO_HEIGHT = 140.dp
private const val NUDGE_CYCLES = 2
private const val NUDGE_DP = 48f
private const val NUDGE_MILLIS = 400
private const val NUDGE_PAUSE_MILLIS = 600L

object GuideDemoTags {
    const val SPOKEN = "demo_spoken"
    const val SPLIT = "demo_split"
}
