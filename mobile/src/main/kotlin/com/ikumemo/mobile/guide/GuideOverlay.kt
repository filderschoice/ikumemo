package com.ikumemo.mobile.guide

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import kotlin.math.roundToInt

/** 照らす場所の位置（画面の左上からの座標）。各画面の部品が [guideTarget] で書き込み、[GuideOverlay] が読む。 */
typealias GuideTargets = SnapshotStateMap<GuideId, Rect>

val LocalGuideTargets = staticCompositionLocalOf<GuideTargets?> { null }

@Composable
fun rememberGuideTargets(): GuideTargets = remember { mutableStateMapOf() }

/** この部品を [id] のガイドで照らす場所にする。 */
fun Modifier.guideTarget(
    id: GuideId,
    targets: GuideTargets?,
): Modifier =
    if (targets == null) {
        this
    } else {
        onGloballyPositioned { targets[id] = it.boundsInRoot() }
    }

/** その場のガイドの吹き出しの文（PLAN §2.12 ②）。 */
fun GuideId.message(): Int =
    when (this) {
        GuideId.G02_VOICE_BUTTON -> R.string.guide_g02
        GuideId.G03_SWIPE_PRACTICE -> R.string.guide_g03
        GuideId.G04_BOUGHT_BUTTON -> R.string.guide_g04
        GuideId.G05_UNDO -> R.string.guide_g05
        GuideId.G06_STORE_CHIP -> R.string.guide_g06
        GuideId.G07_CALENDAR_DAY -> R.string.guide_g07
        GuideId.G08_DUE_DATE -> R.string.guide_g08
    }

/** TalkBack などの画面の読み上げ（タッチで探索）を使っているか（R-56）。 */
fun Context.isTouchExplorationEnabled(): Boolean =
    (getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.isTouchExplorationEnabled == true

/**
 * その場のガイド（F-70）。照らす場所（[LocalGuideTargets] の位置）の周りを暗くし、吹き出しで説明する。
 * 暗くするのは見た目だけで、照らした部品はそのまま操作できる（G-03 の「れんしゅう」をスワイプできるように）。
 * 「わかった」で閉じ、「ガイドを出さない」ですべて止める（R-53）。
 * 読み上げを使っているときは照らす演出を使わず、画面下のカードで同じ内容を出して「わかった」へ移動する（R-56）。
 */
@Composable
fun GuideOverlay(
    guide: GuideId,
    onDismiss: () -> Unit,
    onDisableAll: () -> Unit,
    modifier: Modifier = Modifier,
    useCard: Boolean = LocalContext.current.isTouchExplorationEnabled(),
) {
    val target = LocalGuideTargets.current?.get(guide)
    if (useCard || target == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            GuideBubble(guide, onDismiss, onDisableAll, focusOnShow = useCard, modifier = Modifier.padding(16.dp))
        }
        return
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        val density = LocalDensity.current
        val local = target.translate(-origin)
        Spotlight(target = local)
        val gap = with(density) { BUBBLE_GAP.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        // 照らす場所が画面の上半分なら吹き出しをその下に、下半分ならその上に出す
        val bubbleModifier =
            if (local.center.y < heightPx / 2) {
                Modifier.offset { IntOffset(0, (local.bottom + gap).roundToInt()) }
            } else {
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = with(density) { (heightPx - local.top + gap).coerceAtLeast(0f).toDp() })
            }
        GuideBubble(
            guide,
            onDismiss,
            onDisableAll,
            focusOnShow = false,
            modifier = bubbleModifier.padding(horizontal = 16.dp),
        )
    }
}

/** 照らす場所だけを残して周りを暗くする（見た目だけ。タッチは下の画面へ通す）。 */
@Composable
private fun Spotlight(target: Rect) {
    val padding = with(LocalDensity.current) { SPOT_PADDING.toPx() }
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .testTag(GuideTestTags.SPOTLIGHT),
    ) {
        drawRect(Color.Black.copy(alpha = SCRIM_ALPHA))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(target.left - padding, target.top - padding),
            size = Size(target.width + padding * 2, target.height + padding * 2),
            cornerRadius = CornerRadius(padding * 2),
            blendMode = BlendMode.Clear,
        )
    }
}

@Composable
private fun GuideBubble(
    guide: GuideId,
    onDismiss: () -> Unit,
    onDisableAll: () -> Unit,
    focusOnShow: Boolean,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    if (focusOnShow) {
        LaunchedEffect(guide) { runCatching { focus.requestFocus() } }
    }
    Card(modifier.fillMaxWidth().testTag(GuideTestTags.bubble(guide))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(guide.message()),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onDisableAll,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(GuideTestTags.DISABLE_ALL),
                ) {
                    Text(stringResource(R.string.guide_disable_all), style = MaterialTheme.typography.labelMedium)
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp).focusRequester(focus).testTag(GuideTestTags.DISMISS),
                    ) {
                        Text(stringResource(R.string.guide_ok))
                    }
                }
            }
        }
    }
}

private const val SCRIM_ALPHA = 0.55f
private val SPOT_PADDING = 6.dp
private val BUBBLE_GAP = 12.dp

object GuideTestTags {
    const val SPOTLIGHT = "guide_spotlight"
    const val DISMISS = "guide_dismiss"
    const val DISABLE_ALL = "guide_disable_all"

    fun bubble(id: GuideId) = "guide_bubble_${id.storageKey}"
}
