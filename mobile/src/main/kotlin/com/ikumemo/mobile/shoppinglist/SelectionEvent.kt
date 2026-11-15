package com.ikumemo.mobile.shoppinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R

/** 「えらんで消す」モードの操作（BL-194）。チェックの集合だけを変える。 */
sealed interface SelectionEvent : ShoppingListEvent {
    /** モードを始める。 */
    data object Start : SelectionEvent

    /** モードをやめる（何も消さない）。 */
    data object Stop : SelectionEvent

    /** その品物のチェックを入れ外しする。モードに入っていなければ無視する。 */
    data class Toggle(
        val itemId: Long,
    ) : SelectionEvent
}

/** チェックの集合（null は普段のモード）を [event] で変えた結果。 */
internal fun Set<Long>?.after(event: SelectionEvent): Set<Long>? =
    when (event) {
        SelectionEvent.Start -> emptySet()
        SelectionEvent.Stop -> null
        is SelectionEvent.Toggle -> this?.let { if (event.itemId in it) it - event.itemId else it + event.itemId }
    }

/** 上の帯の「えらんで消す」（BL-194）。押すと各行にチェックが付く。 */
@Composable
internal fun SelectStartButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).testTag(TestTags.SELECT_START)) {
        Text(stringResource(R.string.memo_select_start))
    }
}

/** えらんで消すモードの上の帯の「やめる」。何も消さずに普段のモードへ戻る。 */
@Composable
internal fun SelectCancelButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).testTag(TestTags.SELECT_CANCEL)) {
        Text(stringResource(R.string.memo_select_cancel))
    }
}

/** えらんで消すモードの下のボタン。1 件も選んでいない間は押せない。 */
@Composable
internal fun SelectDeleteBar(
    count: Int,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onDelete,
            enabled = count > 0,
            modifier = Modifier.heightIn(min = 64.dp).widthIn(min = 220.dp).testTag(TestTags.SELECT_DELETE),
        ) {
            Text(stringResource(R.string.memo_select_delete, count))
        }
    }
}
