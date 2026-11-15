package com.ikumemo.mobile.shoppinglist

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.db.ShoppingItemEntity
import com.ikumemo.mobile.share.ShareText

/** お店チップの行。右端に「メモを送る」を置く。送るのは普段のモードの、品物があるときだけ（BL-196）。 */
@Composable
internal fun StoreRow(
    state: ShoppingListUiState,
    onEvent: (ShoppingListEvent) -> Unit,
    chipModifier: Modifier,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        StoreChip(
            selectedStore = state.selectedStore,
            onClick = { onEvent(ShoppingListEvent.OpenStorePicker) },
            modifier = chipModifier.weight(1f, fill = false),
        )
        if (state.selection == null && ShareText.isShareable(state.items)) {
            Spacer(Modifier.weight(1f))
            ShareButton(state.items)
        }
    }
}

/**
 * メモを文字にして共有シートで送る（BL-196）。上の帯は「買い物メモ」「えらんで消す」「設定」で窮屈なため、
 * 帯には足さず、お店チップと同じ行の右端に文字なしのアイコンで置く（読み上げの名前は「メモを送る」）。
 * 品物が無いメモ、えらんで消すモードの間は呼び出し側で出さない。
 */
@Composable
internal fun ShareButton(
    items: List<ShoppingItemEntity>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val title = stringResource(R.string.memo_share_title)
    val failed = stringResource(R.string.memo_share_failed)
    IconButton(
        onClick = {
            if (!ShareText.launch(context, ShareText.build(items), title)) {
                Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).testTag(TestTags.SHARE),
    ) {
        Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.memo_share))
    }
}
