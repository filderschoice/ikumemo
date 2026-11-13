package com.ikumemo.mobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R

/**
 * このアプリについて（PLAN §3）。版・名前の由来・ご意見・ご要望・ライセンス・プライバシーポリシー・GitHub・ストアのページ。
 * ご意見・ご要望は BL-143 で設定の一覧からここへ移した。
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenFeedback: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember { context.appVersionName() }
    SettingsScaffold(title = R.string.settings_about, onBack = onBack) {
        AboutContent(
            version = version,
            onOpenLicenses = onOpenLicenses,
            onOpenLink = context::openExternal,
            onOpenFeedback = onOpenFeedback,
        )
    }
}

@Composable
fun AboutContent(
    version: String,
    onOpenLicenses: () -> Unit,
    onOpenLink: (ExternalLink) -> Unit,
    onOpenFeedback: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.about_version, version),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.about_origin),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        HorizontalDivider()
        LinkRow(R.string.settings_feedback, AboutTestTags.FEEDBACK, onOpenFeedback)
        LinkRow(R.string.about_licenses, AboutTestTags.LICENSES, onOpenLicenses)
        LinkRow(R.string.about_privacy_policy, AboutTestTags.link(ExternalLink.PRIVACY_POLICY)) {
            onOpenLink(ExternalLink.PRIVACY_POLICY)
        }
        LinkRow(R.string.about_github, AboutTestTags.link(ExternalLink.GITHUB)) { onOpenLink(ExternalLink.GITHUB) }
        LinkRow(R.string.about_store, AboutTestTags.link(ExternalLink.STORE)) { onOpenLink(ExternalLink.STORE) }
    }
}

/** 押すと別の画面・ページを開く 1 行。 */
@Composable
fun LinkRow(
    title: Int,
    tag: String,
    onClick: () -> Unit,
) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onClick)
                .padding(16.dp)
                .testTag(tag),
    )
    HorizontalDivider()
}

object AboutTestTags {
    const val LICENSES = "about_licenses"
    const val FEEDBACK = "about_feedback"

    fun link(link: ExternalLink) = "about_link_${link.name.lowercase()}"
}
