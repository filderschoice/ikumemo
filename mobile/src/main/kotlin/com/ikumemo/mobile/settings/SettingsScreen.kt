package com.ikumemo.mobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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

/**
 * せっていの項目（PLAN §3、8 項目）。BL-143 で「操作」と「お知らせ」を「操作とお知らせ」にまとめ、
 * 「ご意見・ご要望」は「このアプリについて」の中へ移した。BL-139 で各行の下に 1 行の説明（[detail]）を足した。
 * 画面での並びと区切りは [SettingsSection] が決める。
 */
enum class SettingsItem(
    val title: Int,
    val route: Any,
    val detail: Int? = null,
) {
    // 使い方は上の方に置く（「このアプリについて」の中に埋めない、H-01）
    HELP(R.string.settings_help, HelpRoute),
    APPEARANCE(R.string.settings_appearance, AppearanceRoute, R.string.settings_appearance_detail),
    OPERATION(R.string.settings_operation, OperationRoute, R.string.settings_operation_detail),
    STORES(R.string.settings_stores, StoreManageRoute, R.string.settings_stores_detail),
    PRODUCTS(R.string.settings_products, ProductManageRoute, R.string.settings_products_detail),
    HOME_SCREEN(R.string.settings_home_screen, HomeScreenRoute, R.string.settings_home_screen_detail),
    DATA(R.string.settings_data, DataRoute, R.string.settings_data_detail),
    ABOUT(R.string.settings_about, AboutRoute, R.string.settings_about_detail),
}

/** せっていの一覧の区切り（BL-139）。使い方だけは見出しを付けずに一番上に置く（H-01）。 */
enum class SettingsSection(
    val title: Int?,
    val items: List<SettingsItem>,
) {
    TOP(null, listOf(SettingsItem.HELP)),
    LOOK(
        R.string.settings_section_look,
        listOf(SettingsItem.APPEARANCE, SettingsItem.OPERATION, SettingsItem.HOME_SCREEN),
    ),
    REGISTERED(
        R.string.settings_section_registered,
        listOf(SettingsItem.STORES, SettingsItem.PRODUCTS, SettingsItem.DATA),
    ),
    APP(R.string.settings_section_app, listOf(SettingsItem.ABOUT)),
}

/** 上部のバーの右の ⚙ 設定（メモ・記録・期限の 3 つのタブで共通、BL-143）。アイコンだけにせず文字を添える（PLAN §2.7）。 */
@Composable
fun SettingsButton(
    onClick: () -> Unit,
    testTag: String,
) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).testTag(testTag)) {
        Icon(Icons.Rounded.Settings, contentDescription = null)
        Text(stringResource(R.string.settings_title), modifier = Modifier.padding(start = 4.dp))
    }
}

/** せってい（PLAN §3 右上メニュー）。見出しで区切り、項目を押すとそれぞれの画面を開く。 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpen: (SettingsItem) -> Unit,
) {
    SettingsScaffold(title = R.string.settings_title, onBack = onBack) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingsSection.entries.forEach { section ->
                section.title?.let { SettingsSectionHeading(it, "settings_section_${section.name.lowercase()}") }
                section.items.forEach { item ->
                    SettingsListRow(item = item, onClick = { onOpen(item) })
                    HorizontalDivider()
                }
            }
        }
    }
}

/** せっていの一覧の見出し（読み上げで見出しとして飛べるよう heading を付ける）。 */
@Composable
private fun SettingsSectionHeading(
    title: Int,
    testTag: String,
) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                .semantics { heading() }
                .testTag(testTag),
    )
}

/** せっていの一覧の 1 行（名前と 1 行の説明。SettingsSwitchRow と同じ作り）。 */
@Composable
private fun SettingsListRow(
    item: SettingsItem,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_${item.name.lowercase()}"),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(item.title), style = MaterialTheme.typography.bodyLarge)
        item.detail?.let {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
