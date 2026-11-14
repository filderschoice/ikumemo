package com.ikumemo.mobile.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.Appearance
import com.ikumemo.mobile.designsystem.BackgroundScrim
import com.ikumemo.mobile.designsystem.DarkMode
import com.ikumemo.mobile.designsystem.IkumemoTheme
import com.ikumemo.mobile.designsystem.InkColor
import com.ikumemo.mobile.designsystem.TextSizeSetting
import com.ikumemo.mobile.designsystem.ThemePreset
import com.ikumemo.mobile.designsystem.photoFilterChipColors
import com.ikumemo.mobile.designsystem.photoOutlinedButtonColors
import com.ikumemo.mobile.designsystem.photoTextButtonColors

@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    viewModel: AppearanceViewModel = viewModel(factory = AppViewModelFactory.Factory),
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    SettingsScaffold(title = R.string.settings_appearance, onBack = onBack) {
        AppearanceContent(appearance = appearance, onEvent = viewModel::onEvent)
    }
}

/** きせかえ（F-65 文字の大きさ → F-60 プレビュー付きのカードでテーマを選ぶ → F-63 暗い画面 → F-61 背景の写真 の順）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceContent(
    appearance: Appearance,
    onEvent: (AppearanceEvent) -> Unit,
) {
    val dark = appearance.darkMode.isDark(isSystemInDarkTheme())
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 文字を大きくしたい人がスクロールせずに見つけられるよう、文字の大きさを一番上に置く。
        // 背景の写真は選ぶと膜の濃さ・ペンの色が増えるので最後に置く
        SectionTitle(R.string.appearance_text_size)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextSizeSetting.entries.forEach { size ->
                FilterChip(
                    selected = size == appearance.textSize,
                    onClick = { onEvent(AppearanceEvent.SelectTextSize(size)) },
                    label = { Text(stringResource(size.label()), fontSize = (BASE_SAMPLE_SP * size.multiplier).sp) },
                    colors = photoFilterChipColors(),
                    modifier = Modifier.heightIn(min = 48.dp).testTag(AppearanceTestTags.textSize(size)),
                )
            }
        }
        SectionTitle(R.string.appearance_theme)
        // 言葉ではなく見た目で選べるよう、各テーマの色で小さな画面の見本を描く
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemePreset.entries.forEach { preset ->
                ThemeCard(
                    preset = preset,
                    dark = dark,
                    selected = preset == appearance.theme,
                    onClick = { onEvent(AppearanceEvent.SelectTheme(preset)) },
                )
            }
        }
        SectionTitle(R.string.appearance_dark_mode)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DarkMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == appearance.darkMode,
                    onClick = { onEvent(AppearanceEvent.SelectDarkMode(mode)) },
                    label = { Text(stringResource(mode.label())) },
                    colors = photoFilterChipColors(),
                    modifier = Modifier.heightIn(min = 48.dp).testTag(AppearanceTestTags.darkMode(mode)),
                )
            }
        }
        BackgroundSection(appearance = appearance, onEvent = onEvent)
    }
}

private const val BASE_SAMPLE_SP = 14f

/** 背景の写真（F-61）。写真を選ぶ・外す、膜の濃さのスライダー（離したときに保存する）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundSection(
    appearance: Appearance,
    onEvent: (AppearanceEvent) -> Unit,
) {
    val pick =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { onEvent(AppearanceEvent.PickBackground(it)) }
        }
    SectionTitle(R.string.appearance_background)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
            onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            colors = photoOutlinedButtonColors(),
            modifier = Modifier.heightIn(min = 48.dp).testTag(AppearanceTestTags.BACKGROUND_PICK),
        ) {
            Text(stringResource(R.string.appearance_background_pick))
        }
        if (appearance.backgroundImage != null) {
            TextButton(
                onClick = { onEvent(AppearanceEvent.RemoveBackground) },
                colors = photoTextButtonColors(),
                modifier = Modifier.heightIn(min = 48.dp).testTag(AppearanceTestTags.BACKGROUND_REMOVE),
            ) {
                Text(stringResource(R.string.appearance_background_remove))
            }
        }
    }
    if (appearance.backgroundImage != null) {
        var level by remember(appearance.scrimLevel) { mutableFloatStateOf(appearance.scrimLevel) }
        Text(stringResource(R.string.appearance_scrim), style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = level,
            onValueChange = { level = it },
            onValueChangeFinished = { onEvent(AppearanceEvent.SetScrimLevel(level)) },
            valueRange = BackgroundScrim.MIN..BackgroundScrim.MAX,
            modifier = Modifier.fillMaxWidth().testTag(AppearanceTestTags.SCRIM),
        )
        Text(stringResource(R.string.appearance_ink), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InkColor.entries.forEach { ink ->
                FilterChip(
                    selected = ink == appearance.ink,
                    onClick = { onEvent(AppearanceEvent.SelectInk(ink)) },
                    label = { Text(stringResource(ink.label())) },
                    colors = photoFilterChipColors(),
                    modifier = Modifier.heightIn(min = 48.dp).testTag(AppearanceTestTags.ink(ink)),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ThemeCard(
    preset: ThemePreset,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val outline = MaterialTheme.colorScheme.primary
    Card(
        border = if (selected) BorderStroke(3.dp, outline) else null,
        modifier =
            Modifier
                .width(150.dp)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .testTag(AppearanceTestTags.theme(preset)),
    ) {
        IkumemoTheme(preset = preset, darkTheme = dark) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxWidth()) {
                    Surface(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.memo_title),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.appearance_sample_item),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    ) {
                        Text(
                            stringResource(R.string.appearance_sample_chip),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = outline)
            }
            Text(stringResource(preset.label()), modifier = Modifier.padding(start = 4.dp))
        }
    }
}

private fun ThemePreset.label(): Int =
    when (this) {
        ThemePreset.POP -> R.string.theme_pop
        ThemePreset.GENTLE -> R.string.theme_gentle
        ThemePreset.COOL -> R.string.theme_cool
        ThemePreset.SIMPLE -> R.string.theme_simple
    }

private fun DarkMode.label(): Int =
    when (this) {
        DarkMode.SYSTEM -> R.string.appearance_dark_system
        DarkMode.LIGHT -> R.string.appearance_dark_light
        DarkMode.DARK -> R.string.appearance_dark_dark
    }

private fun InkColor.label(): Int =
    when (this) {
        InkColor.AUTO -> R.string.appearance_ink_auto
        InkColor.BLACK -> R.string.appearance_ink_black
        InkColor.BLUE -> R.string.appearance_ink_blue
        InkColor.RED -> R.string.appearance_ink_red
        InkColor.WHITE -> R.string.appearance_ink_white
    }

private fun TextSizeSetting.label(): Int =
    when (this) {
        TextSizeSetting.NORMAL -> R.string.appearance_text_normal
        TextSizeSetting.LARGE -> R.string.appearance_text_large
        TextSizeSetting.EXTRA_LARGE -> R.string.appearance_text_extra_large
    }

object AppearanceTestTags {
    const val BACKGROUND_PICK = "appearance_background_pick"
    const val BACKGROUND_REMOVE = "appearance_background_remove"
    const val SCRIM = "appearance_scrim"

    fun theme(preset: ThemePreset) = "appearance_theme_${preset.storageKey}"

    fun darkMode(mode: DarkMode) = "appearance_dark_${mode.storageKey}"

    fun ink(ink: InkColor) = "appearance_ink_${ink.storageKey}"

    fun textSize(size: TextSizeSetting) = "appearance_text_${size.storageKey}"
}
