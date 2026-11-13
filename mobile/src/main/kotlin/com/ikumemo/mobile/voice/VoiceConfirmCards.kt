package com.ikumemo.mobile.voice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R

/** 「話して追加」の確認カードと、その下の「N つ追加する」などのボタン（PLAN §3.2、BL-136）。 */
@Composable
internal fun ConfirmCards(
    cards: List<VoiceCard>,
    onEvent: (VoiceInputEvent) -> Unit,
    onTypeInstead: () -> Unit,
) {
    cards.forEach { card -> ConfirmCard(card, onEvent) }
    // 文字が大きい設定でもはみ出さないよう、主ボタンは全幅で 1 段目に置く。入れる件数を添える（0 件は押せない）
    val includedCount = cards.count { it.included }
    Button(
        onClick = { onEvent(VoiceInputEvent.AddAll) },
        enabled = includedCount > 0,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(VoiceTestTags.ADD_ALL),
    ) {
        Text(
            if (includedCount > 0) {
                stringResource(R.string.voice_add_count, includedCount)
            } else {
                stringResource(R.string.voice_add_all)
            },
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        TextButton(onClick = onTypeInstead, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.voice_type_instead))
        }
        OutlinedButton(onClick = { onEvent(VoiceInputEvent.Start) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.voice_retry))
        }
    }
}

/**
 * 確認カード。タップで直し、× で外す。外したカードは品名と数量に取り消し線を引いて薄くし、
 * 右のボタンを「↩ 戻す」に替えて、押すと戻ることが見た目で分かるようにする（BL-136）。
 */
@Composable
private fun ConfirmCard(
    card: VoiceCard,
    onEvent: (VoiceInputEvent) -> Unit,
) {
    var editing by rememberSaveable(card.key) { mutableStateOf(false) }
    val colors =
        if (card.included) {
            CardDefaults.cardColors()
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    Card(colors = colors, modifier = Modifier.fillMaxWidth().testTag(VoiceTestTags.card(card.key))) {
        if (editing) {
            EditingCard(card, onDone = { name, quantity ->
                onEvent(VoiceInputEvent.EditCard(card.key, name, quantity))
                editing = false
            })
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { editing = true }.padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardTexts(card, Modifier.weight(1f))
                ToggleCardButton(card, onToggle = { onEvent(VoiceInputEvent.ToggleCard(card.key)) })
            }
        }
    }
}

/** 品名・数量・「メモにあります」。外したカードは取り消し線を引いて薄くする。 */
@Composable
private fun CardTexts(
    card: VoiceCard,
    nameModifier: Modifier,
) {
    val excludedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val decoration = if (card.included) null else TextDecoration.LineThrough
    val color = if (card.included) Color.Unspecified else excludedColor
    Text(
        card.name,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        textDecoration = decoration,
        modifier = nameModifier,
    )
    card.quantity?.let {
        Text(it, color = color, textDecoration = decoration, modifier = Modifier.padding(horizontal = 8.dp))
    }
    if (card.alreadyInMemo) {
        Text(
            stringResource(R.string.voice_already_in_memo),
            style = MaterialTheme.typography.labelMedium,
            color = excludedColor,
        )
    }
}

/** 入れているときは ×、外したときは「↩ 戻す」。読み上げは今までどおり「〇〇を外す」「〇〇を入れる」。 */
@Composable
private fun ToggleCardButton(
    card: VoiceCard,
    onToggle: () -> Unit,
) {
    val description =
        stringResource(
            if (card.included) R.string.voice_exclude_card else R.string.voice_include_card,
            card.name,
        )
    if (card.included) {
        IconButton(onClick = onToggle) {
            Icon(Icons.Rounded.Close, contentDescription = description)
        }
    } else {
        TextButton(
            onClick = onToggle,
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = description },
        ) {
            Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null)
            Text(stringResource(R.string.voice_restore_card), modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun EditingCard(
    card: VoiceCard,
    onDone: (String, String) -> Unit,
) {
    var name by rememberSaveable(card.key) { mutableStateOf(card.name) }
    var quantity by rememberSaveable(card.key) { mutableStateOf(card.quantity.orEmpty()) }
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.edit_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text(stringResource(R.string.edit_quantity)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onDone(name, quantity) }, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
            Text(stringResource(R.string.common_done))
        }
    }
}
