package com.ikumemo.mobile.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ikumemo.mobile.R

/**
 * 「話して追加」のシート（PLAN §3.2）。開くとすぐ聞き取りを始め、確認カードで直してから追加する。
 * マイクの許可は初めて使う時に理由を添えて求める（PLAN §5）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputSheet(
    viewModel: VoiceInputViewModel,
    onTypeInstead: () -> Unit,
    onAdded: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasRecordAudioPermission()) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
            viewModel.onEvent(if (granted) VoiceInputEvent.Start else VoiceInputEvent.PermissionDenied)
        }
    LaunchedEffect(Unit) {
        if (hasPermission) viewModel.onEvent(VoiceInputEvent.Start)
    }
    LaunchedEffect(state) {
        (state as? VoiceInputState.Added)?.let { onAdded(it.count) }
    }
    val close = {
        viewModel.onEvent(VoiceInputEvent.Cancel)
        onDismiss()
    }
    ModalBottomSheet(
        onDismissRequest = close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        VoiceInputContent(
            state = state,
            needsPermission = !hasPermission,
            onEvent = viewModel::onEvent,
            onSheetAction = { action ->
                when (action) {
                    VoiceSheetAction.RequestPermission -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    VoiceSheetAction.OpenSettings -> context.openAppSettings()
                    VoiceSheetAction.TypeInstead -> {
                        viewModel.onEvent(VoiceInputEvent.Cancel)
                        onTypeInstead()
                    }
                }
            },
        )
    }
}

/** シートの外（権限の要求・端末の設定・文字入力への切り替え）へ渡す操作。 */
enum class VoiceSheetAction {
    RequestPermission,
    OpenSettings,
    TypeInstead,
}

@Composable
fun VoiceInputContent(
    state: VoiceInputState,
    needsPermission: Boolean,
    onEvent: (VoiceInputEvent) -> Unit,
    onSheetAction: (VoiceSheetAction) -> Unit,
) {
    val onTypeInstead = { onSheetAction(VoiceSheetAction.TypeInstead) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            state is VoiceInputState.PermissionDenied ->
                Guidance(
                    text = stringResource(R.string.voice_permission_denied),
                    primaryLabel = stringResource(R.string.voice_open_settings),
                    onPrimary = { onSheetAction(VoiceSheetAction.OpenSettings) },
                    onTypeInstead = onTypeInstead,
                )
            state is VoiceInputState.Unavailable ->
                Guidance(
                    text = stringResource(R.string.voice_unavailable),
                    primaryLabel = null,
                    onPrimary = {},
                    onTypeInstead = onTypeInstead,
                )
            needsPermission ->
                Guidance(
                    text = stringResource(R.string.voice_permission_rationale),
                    primaryLabel = stringResource(R.string.voice_use_microphone),
                    onPrimary = { onSheetAction(VoiceSheetAction.RequestPermission) },
                    onTypeInstead = onTypeInstead,
                )
            state is VoiceInputState.Listening -> Listening(state.partialText)
            state is VoiceInputState.Confirm -> ConfirmCards(state.cards, onEvent, onTypeInstead)
            state is VoiceInputState.NotHeard ->
                Guidance(
                    text = stringResource(R.string.voice_not_heard),
                    primaryLabel = stringResource(R.string.voice_retry),
                    onPrimary = { onEvent(VoiceInputEvent.Start) },
                    onTypeInstead = onTypeInstead,
                )
            else -> Listening("")
        }
    }
}

@Composable
private fun Listening(partialText: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.voice_listening), style = MaterialTheme.typography.titleMedium)
    }
    if (partialText.isNotEmpty()) {
        Text(
            text = stringResource(R.string.voice_partial, partialText),
            modifier = Modifier.testTag(VoiceTestTags.PARTIAL),
        )
    }
}

@Composable
private fun Guidance(
    text: String,
    primaryLabel: String?,
    onPrimary: () -> Unit,
    onTypeInstead: () -> Unit,
) {
    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag(VoiceTestTags.GUIDANCE))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        TextButton(onClick = onTypeInstead, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.voice_type_instead))
        }
        if (primaryLabel != null) {
            Button(onClick = onPrimary, modifier = Modifier.heightIn(min = 48.dp)) { Text(primaryLabel) }
        }
    }
}

internal fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/** 端末の設定のこのアプリの画面を開く（PLAN §2.10）。 */
fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

object VoiceTestTags {
    const val PARTIAL = "voice_partial"
    const val ADD_ALL = "voice_add_all"
    const val GUIDANCE = "voice_guidance"

    fun card(key: Int) = "voice_card_$key"
}
