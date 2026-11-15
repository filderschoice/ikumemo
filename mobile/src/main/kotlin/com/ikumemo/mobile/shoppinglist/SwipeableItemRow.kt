package com.ikumemo.mobile.shoppinglist

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.OperationSettings
import com.ikumemo.mobile.data.db.ShoppingItemEntity

/** そうさの設定（○ の表示・振動）。メモ画面が渡す。 */
val LocalOperationSettings = staticCompositionLocalOf { OperationSettings() }

/** 「えらんで消す」モードのときの 1 行の状態（BL-194）。普段のモードでは null。 */
data class RowSelection(
    val checked: Boolean,
)

/** 行の「えらんで消す」の状態。メモ画面が行ごとに渡す。 */
val LocalRowSelection = staticCompositionLocalOf<RowSelection?> { null }

/** 行の ○ に足す Modifier（G-04 で照らす場所にするため、メモの一番上の行にだけ渡す）。 */
val LocalBoughtButtonModifier = staticCompositionLocalOf<Modifier> { Modifier }

/**
 * メモの 1 行。右へスワイプで「買った」、左へスワイプで「やめた」（D-4・F-10・F-11）。左の ○ を押しても「買った」（F-13）。
 * しきい値（幅の 40%）に届いたときだけ確定し、速度だけでは確定しない（BL-193）。
 * しきい値を超えた瞬間に軽く振動させる（`performHapticFeedback` のため VIBRATE 権限は不要、R-05）。
 * ○ と振動は そうさ の設定で消せる（[LocalOperationSettings]）。
 * 「えらんで消す」モード（[LocalRowSelection] が null でないとき、BL-194）では、スワイプと ○ の代わりにチェックを出し、行を押すと [onClick]（チェックの入れ外し）を呼ぶ。
 */
@Composable
fun SwipeableItemRow(
    item: ShoppingItemEntity,
    highlighted: Boolean,
    onClick: () -> Unit,
    onBought: () -> Unit,
    onDismissed: () -> Unit,
) {
    val selection = LocalRowSelection.current
    val selecting = selection != null
    val currentOnBought by rememberUpdatedState(onBought)
    val currentOnDismissed by rememberUpdatedState(onDismissed)
    val haptics = LocalHapticFeedback.current
    val vibration = LocalOperationSettings.current.vibration
    val guard = remember { SwipeGuard() }
    val dismissState = rememberGuardedDismissState(guard, { currentOnBought() }, { currentOnDismissed() })
    LaunchedEffect(dismissState.targetValue) {
        if (vibration && dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    // スワイプできない・しにくい人のため、読み上げ（TalkBack）の操作の一覧に「買った」「やめた」を出す（PLAN §8、R-04）
    val boughtLabel = stringResource(R.string.memo_bought)
    val dismissedLabel = stringResource(R.string.memo_dismissed)
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(dismissState.dismissDirection) },
        enableDismissFromStartToEnd = !selecting,
        enableDismissFromEndToStart = !selecting,
        modifier =
            Modifier
                .onSizeChanged { guard.widthPx = it.width.toFloat() }
                .then(guard.observe())
                .semantics {
                    customActions =
                        if (selecting) {
                            emptyList()
                        } else {
                            listOf(
                                CustomAccessibilityAction(boughtLabel) {
                                    currentOnBought()
                                    true
                                },
                                CustomAccessibilityAction(dismissedLabel) {
                                    currentOnDismissed()
                                    true
                                },
                            )
                        }
                }.testTag(TestTags.item(item.id)),
    ) {
        ItemRowContent(
            item = item,
            highlighted = highlighted,
            onClick = onClick,
            onBoughtClick = onBought,
            selection = selection?.checked,
        )
    }
}

/** 確定の直前に [SwipeGuard] で位置と縦横を確かめる状態（BL-193）。確定したときだけ [onBought]・[onDismissed] を呼ぶ。 */
@Composable
private fun rememberGuardedDismissState(
    guard: SwipeGuard,
    onBought: () -> Unit,
    onDismissed: () -> Unit,
): SwipeToDismissBoxState {
    val stateHolder = remember { arrayOfNulls<SwipeToDismissBoxState>(1) }
    val state =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                // 素早く払っただけでは確定しない。幅の 40% 以上で、縦より横の動きが大きいときだけ決める（BL-193）
                val offset = stateHolder[0]?.let { runCatching { it.requireOffset() }.getOrNull() } ?: 0f
                val confirmed = value == SwipeToDismissBoxValue.Settled || guard.allows(offset)
                if (confirmed && value != SwipeToDismissBoxValue.Settled && guard.firstConfirmation()) {
                    when (value) {
                        SwipeToDismissBoxValue.StartToEnd -> onBought()
                        SwipeToDismissBoxValue.EndToStart -> onDismissed()
                        SwipeToDismissBoxValue.Settled -> Unit
                    }
                }
                confirmed && value != SwipeToDismissBoxValue.Settled
            },
            positionalThreshold = { totalDistance -> totalDistance * SWIPE_THRESHOLD_FRACTION },
        )
    stateHolder[0] = state
    return state
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue) {
    val colors = MaterialTheme.colorScheme
    val (background, content) =
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> colors.primaryContainer to colors.onPrimaryContainer
            SwipeToDismissBoxValue.EndToStart -> colors.surfaceVariant to colors.onSurfaceVariant
            SwipeToDismissBoxValue.Settled -> Color.Transparent to Color.Transparent
        }
    Row(
        modifier = Modifier.fillMaxSize().background(background).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            if (direction == SwipeToDismissBoxValue.EndToStart) Arrangement.End else Arrangement.Start,
    ) {
        // 色だけで伝えず、アイコンと文字を並べる（PLAN §1.3-7）
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = content)
                Text(stringResource(R.string.memo_bought), color = content, modifier = Modifier.padding(start = 8.dp))
            }
            SwipeToDismissBoxValue.EndToStart -> {
                Text(stringResource(R.string.memo_dismissed), color = content, modifier = Modifier.padding(end = 8.dp))
                Icon(Icons.Rounded.Close, contentDescription = null, tint = content)
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
}

@Composable
private fun ItemRowContent(
    item: ShoppingItemEntity,
    highlighted: Boolean,
    onClick: () -> Unit,
    onBoughtClick: () -> Unit,
    selection: Boolean?,
) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (highlighted) colors.secondaryContainer else colors.surface,
        label = "highlight",
    )
    // 行の面は surface を塗るため、文字はペンの色（LocalContentColor）ではなく onSurface にする（BL-170）
    CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
        ItemRowBody(item, background, onClick, onBoughtClick, selection)
    }
}

@Composable
private fun ItemRowBody(
    item: ShoppingItemEntity,
    background: Color,
    onClick: () -> Unit,
    onBoughtClick: () -> Unit,
    selection: Boolean?,
) {
    // 「えらんで消す」では行全体がチェックの入れ外しになる（読み上げでは「チェックボックス・オン」と読まれる）
    val tap =
        if (selection == null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier.toggleable(value = selection, role = Role.Checkbox, onValueChange = { onClick() })
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(background)
                .then(tap)
                .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selection != null) {
            // 行全体が toggleable のため、チェック自体は押せなくする（二重に反応しない）
            Checkbox(
                checked = selection,
                onCheckedChange = null,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp).heightIn(min = 48.dp),
            )
        } else if (LocalOperationSettings.current.showBoughtButton) {
            IconButton(
                onClick = onBoughtClick,
                modifier = LocalBoughtButtonModifier.current.testTag(TestTags.boughtButton(item.id)),
            ) {
                Icon(
                    Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = stringResource(R.string.memo_mark_bought_description, item.displayName),
                )
            }
        } else {
            Spacer(Modifier.width(16.dp))
        }
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        )
        if (item.photoUri != null) {
            Icon(
                Icons.Rounded.PhotoCamera,
                contentDescription = stringResource(R.string.memo_has_photo),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        item.quantity?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
