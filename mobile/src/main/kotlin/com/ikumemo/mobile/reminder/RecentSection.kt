package com.ikumemo.mobile.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.designsystem.ReadablePanel
import com.ikumemo.mobile.designsystem.panelEdge
import com.ikumemo.mobile.guide.GuideId
import com.ikumemo.mobile.guide.GuideTargets
import com.ikumemo.mobile.guide.guideTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 「最近買った物」で最初に出す件数（BL-142）。残りは「ほか N 件を見る」で出す。 */
internal const val RECENT_SHOWN_FIRST = 3

/** 「期限なしにしました」を出す時間（メモの「元に戻す」と同じ 5 秒）。 */
private const val NO_DUE_UNDO_MILLIS = 5_000L

/**
 * 「最近買った物」の行（F-50・BL-142）。最初の [RECENT_SHOWN_FIRST] 件だけ出し、[expanded] なら全部出す。
 * G-08 は一番上の行の「期限を決める」を照らす（外した記録を除いた 1 行目）。
 */
internal fun LazyListScope.recentRows(
    recent: List<PurchaseRecordEntity>,
    expanded: Boolean,
    onExpand: () -> Unit,
    onEvent: (ReminderEvent) -> Unit,
    targets: GuideTargets?,
) {
    val shown = if (expanded) recent else recent.take(RECENT_SHOWN_FIRST)
    val rest = recent.size - shown.size
    // 背景の写真があるときは、行と「ほか N 件を見る」を続いた 1 枚の面に載せる（BL-130）
    val panelRows = shown.size + if (rest > 0) 1 else 0
    itemsIndexed(shown, key = { _, record -> "recent_${record.id}" }) { index, record ->
        val target = if (index == 0) Modifier.guideTarget(GuideId.G08_DUE_DATE, targets) else Modifier
        ReadablePanel(panelEdge(index, panelRows)) {
            RecentRow(
                record = record,
                setDueModifier = target,
                onSetDue = { onEvent(ReminderEvent.OpenDue(DueTarget(record.id, record.displayName))) },
                onNoDue = { onEvent(ReminderEvent.NoDue(record.id, record.displayName)) },
            )
            HorizontalDivider()
        }
    }
    if (rest > 0) {
        item(key = "recent_more") {
            ReadablePanel(panelEdge(panelRows - 1, panelRows)) {
                TextButton(
                    onClick = onExpand,
                    modifier =
                        Modifier
                            .padding(horizontal = 8.dp)
                            .heightIn(min = 48.dp)
                            .testTag(ReminderTestTags.RECENT_MORE),
                ) {
                    Text(stringResource(R.string.reminder_recent_more, rest))
                }
            }
        }
    }
}

/** 最近買った物の 1 行。「期限を決める」（G-08 で照らす場所）と「期限なし」（一覧から外す、BL-142）。 */
@Composable
private fun RecentRow(
    record: PurchaseRecordEntity,
    setDueModifier: Modifier,
    onSetDue: () -> Unit,
    onNoDue: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(record.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    R.string.reminder_bought_on,
                    record.purchasedDate.monthValue,
                    record.purchasedDate.dayOfMonth,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = onNoDue,
            modifier = Modifier.heightIn(min = 48.dp).testTag(ReminderTestTags.noDue(record.id)),
        ) {
            Text(stringResource(R.string.reminder_no_due))
        }
        Box(setDueModifier) {
            OutlinedButton(
                onClick = onSetDue,
                modifier = Modifier.heightIn(min = 48.dp).testTag(ReminderTestTags.setDue(record.id)),
            ) {
                Text(stringResource(R.string.reminder_set_due))
            }
        }
    }
}

/** 「期限なしにしました：品名 [元に戻す]」を 5 秒出す。新しく外したら前の表示は置き換わる（メモの UndoEffect と同じ考え方）。 */
@Composable
internal fun NoDueUndoEffect(
    undo: NoDueUndo?,
    snackbarHostState: SnackbarHostState,
    onEvent: (ReminderEvent) -> Unit,
) {
    val messageFormat = stringResource(R.string.reminder_no_due_message)
    val undoLabel = stringResource(R.string.memo_undo)
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(undo?.serial) {
        undo ?: return@LaunchedEffect
        val timeout =
            accessibilityManager?.calculateRecommendedTimeoutMillis(
                NO_DUE_UNDO_MILLIS,
                containsIcons = false,
                containsText = true,
                containsControls = true,
            ) ?: NO_DUE_UNDO_MILLIS
        snackbarHostState.currentSnackbarData?.dismiss()
        val dismissJob =
            launch {
                delay(timeout)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        val message = messageFormat.format(undo.name)
        val result = snackbarHostState.showSnackbar(message, undoLabel, duration = SnackbarDuration.Indefinite)
        dismissJob.cancel()
        if (result == SnackbarResult.ActionPerformed) {
            onEvent(ReminderEvent.UndoNoDue(undo.serial))
        } else {
            onEvent(ReminderEvent.NoDueUndoExpired(undo.serial))
        }
    }
}
