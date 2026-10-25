package com.ikumemo.mobile.reminder

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ikumemo.mobile.R

/** Android 13 以降で 2 回断られた後は、端末の設定のこのアプリの画面を開く（R-29）。 */
private const val MAX_PERMISSION_REQUESTS = 2

/**
 * 通知の許可の求め方（PLAN §5）。初めて期限を決めた時に「期限が近づいたら、おしらせします。」を添えて求める。
 * 通知オフの案内の「通知をオンにする」では、2 回までは許可を求め、それより後は端末の設定を開く。
 */
class NotificationPermission internal constructor(
    private val context: Context,
    private val state: ReminderUiState,
    private val onEvent: (ReminderEvent) -> Unit,
    private val launcher: ActivityResultLauncher<String>,
    private val showRationale: MutableState<Boolean>,
) {
    private val needsRuntimePermission get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun askAfterFirstDue() {
        if (needsRuntimePermission && !state.notificationsEnabled && state.permissionRequests == 0) {
            showRationale.value = true
        }
    }

    fun enable() {
        if (needsRuntimePermission && state.permissionRequests < MAX_PERMISSION_REQUESTS) {
            request()
        } else {
            context.openNotificationSettings()
        }
    }

    private fun request() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        onEvent(ReminderEvent.PermissionRequested)
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Composable
    fun Dialog() {
        var visible by showRationale
        if (!visible) return
        AlertDialog(
            onDismissRequest = { visible = false },
            text = { Text(stringResource(R.string.reminder_permission_rationale)) },
            confirmButton = {
                TextButton(onClick = {
                    visible = false
                    request()
                }) { Text(stringResource(R.string.reminder_permission_allow)) }
            },
            dismissButton = {
                TextButton(onClick = { visible = false }) { Text(stringResource(R.string.reminder_permission_later)) }
            },
        )
    }
}

@Composable
fun rememberNotificationPermission(
    state: ReminderUiState,
    onEvent: (ReminderEvent) -> Unit,
    onResult: () -> Unit,
): NotificationPermission {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onResult() }
    val showRationale = rememberSaveable { mutableStateOf(false) }
    return NotificationPermission(context, state, onEvent, launcher, showRationale)
}
