package com.ikumemo.mobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.R

/** データ（PLAN §3）。データをすべて消す（F-76）。 */
@Composable
fun DataScreen(
    onBack: () -> Unit,
    viewModel: DataViewModel = viewModel(factory = AppViewModelFactory.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScaffold(title = R.string.settings_data, onBack = onBack) {
        DataContent(state, viewModel::onEvent)
    }
}

@Composable
fun DataContent(
    state: DataUiState,
    onEvent: (DataEvent) -> Unit,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(enabled = !state.busy) { confirming = true }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(DataTestTags.ERASE),
        ) {
            Text(stringResource(R.string.data_erase_title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.data_erase_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider()
        if (state.erased) {
            Text(
                stringResource(R.string.data_erased),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp).testTag(DataTestTags.ERASED),
            )
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.data_erase_confirm_title)) },
            text = { Text(stringResource(R.string.data_erase_confirm_detail)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onEvent(DataEvent.EraseAll)
                    },
                    modifier = Modifier.testTag(DataTestTags.ERASE_CONFIRM),
                ) { Text(stringResource(R.string.data_erase_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.reminder_settings_cancel)) }
            },
        )
    }
}

object DataTestTags {
    const val ERASE = "data_erase"
    const val ERASE_CONFIRM = "data_erase_confirm"
    const val ERASED = "data_erased"
}
