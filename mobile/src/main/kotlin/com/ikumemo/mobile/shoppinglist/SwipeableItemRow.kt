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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.OperationSettings
import com.ikumemo.mobile.data.db.ShoppingItemEntity

/** スワイプで確定する距離（画面幅の 40%、R-05）。 */
private const val SWIPE_THRESHOLD_FRACTION = 0.4f

/** そうさの設定（○ の表示・振動）。メモ画面が渡す。 */
val LocalOperationSettings = staticCompositionLocalOf { OperationSettings() }

/** 行の ○ に足す Modifier（G-04 で照らす場所にするため、メモの一番上の行にだけ渡す）。 */
val LocalBoughtButtonModifier = staticCompositionLocalOf<Modifier> { Modifier }

/**
 * メモの 1 行。右へスワイプで「買った」、左へスワイプで「やめた」（D-4・F-10・F-11）。左の ○ を押しても「買った」（F-13）。
 * しきい値を超えた瞬間に軽く振動させる（`performHapticFeedback` のため VIBRATE 権限は不要、R-05）。
 * ○ と振動は そうさ の設定で消せる（[LocalOperationSettings]）。
 */
@Composable
fun SwipeableItemRow(
    item: ShoppingItemEntity,
    highlighted: Boolean,
    onClick: () -> Unit,
    onBought: () -> Unit,
    onDismissed: () -> Unit,
) {
    val currentOnBought by rememberUpdatedState(onBought)
    val currentOnDismissed by rememberUpdatedState(onDismissed)
    val haptics = LocalHapticFeedback.current
    val vibration = LocalOperationSettings.current.vibration
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> currentOnBought()
                    SwipeToDismissBoxValue.EndToStart -> currentOnDismissed()
                    SwipeToDismissBoxValue.Settled -> Unit
                }
                value != SwipeToDismissBoxValue.Settled
            },
            positionalThreshold = { totalDistance -> totalDistance * SWIPE_THRESHOLD_FRACTION },
        )
    LaunchedEffect(dismissState.targetValue) {
        if (vibration && dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(dismissState.dismissDirection) },
        modifier = Modifier.testTag(TestTags.item(item.id)),
    ) {
        ItemRowContent(
            item = item,
            highlighted = highlighted,
            onClick = onClick,
            onBoughtClick = onBought,
        )
    }
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
) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (highlighted) colors.secondaryContainer else colors.surface,
        label = "highlight",
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(background)
                .clickable(onClick = onClick)
                .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (LocalOperationSettings.current.showBoughtButton) {
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
