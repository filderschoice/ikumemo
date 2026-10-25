package com.ikumemo.mobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R

/** せっていの各画面の枠（見出しと戻るボタン）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
    title: Int,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                    )
                }
            },
            windowInsets = WindowInsets(0),
        )
        content()
    }
}

/** せっていの項目。 */
enum class SettingsItem(
    val title: Int,
) {
    APPEARANCE(R.string.settings_appearance),
    OPERATION(R.string.settings_operation),
    REMINDER(R.string.settings_reminder),
    STORES(R.string.settings_stores),
    PRODUCTS(R.string.settings_products),
}

/** せってい（PLAN §3 右上メニュー）。項目を押すとそれぞれの画面を開く。 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpen: (SettingsItem) -> Unit,
) {
    SettingsScaffold(title = R.string.settings_title, onBack = onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsItem.entries.forEach { item ->
                Text(
                    stringResource(item.title),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable { onOpen(item) }
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .testTag("settings_${item.name.lowercase()}"),
                )
                HorizontalDivider()
            }
        }
    }
}

/** せっていの切り替えの 1 行（見出し・説明・スイッチ）。行のどこを押しても切り替わる。 */
@Composable
fun SettingsSwitchRow(
    title: Int,
    detail: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // 行全体で切り替えるため、スイッチ自体は押せないようにする（読み上げが二重にならないように）
        Switch(checked = checked, onCheckedChange = null)
    }
}
