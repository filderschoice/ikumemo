package com.ikumemo.mobile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import com.ikumemo.mobile.designsystem.photoInk

/** よくある質問（問いと答え）。先に読んでもらい、解決しなければ送る方法へ進む（F-77 ①）。 */
private val FAQ =
    listOf(
        R.string.faq_notification_q to R.string.faq_notification_a,
        R.string.faq_voice_q to R.string.faq_voice_a,
        R.string.faq_device_change_q to R.string.faq_device_change_a,
        R.string.faq_widget_q to R.string.faq_widget_a,
    )

/**
 * ご意見・ご要望（F-77）。①よくある質問 → ②送る方法。送る方法はフォーム（主）と GitHub（従、BL-206）。
 * フォームは転送ページ経由で、版・Android の版・機種名が自動で入る。GitHub の Issue には自動で入れられないため、画面に出してコピーできるようにする。
 */
@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val deviceInfo = remember { context.feedbackDeviceInfo() }
    SettingsScaffold(title = R.string.settings_feedback, onBack = onBack) {
        FeedbackContent(
            deviceInfo = deviceInfo,
            onCopyDeviceInfo = { clipboard.setText(AnnotatedString(deviceInfo)) },
            onOpenForm = { context.openFeedbackForm() },
            onOpenGitHub = { context.openExternal(ExternalLink.GITHUB_ISSUE) },
        )
    }
}

@Composable
fun FeedbackContent(
    deviceInfo: String,
    onCopyDeviceInfo: () -> Unit,
    onOpenForm: () -> Unit,
    onOpenGitHub: () -> Unit,
) {
    var opened by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionTitle(R.string.feedback_faq_title)
        FAQ.forEachIndexed { index, (question, answer) ->
            val isOpen = opened == index
            // 読み上げで開閉の状態と、押すと何が起きるかを伝える（BL-115）
            val state = stringResource(if (isOpen) R.string.feedback_faq_expanded else R.string.feedback_faq_collapsed)
            val clickLabel = stringResource(if (isOpen) R.string.feedback_faq_close else R.string.feedback_faq_open)
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(onClickLabel = clickLabel) { opened = if (isOpen) null else index }
                    .semantics { stateDescription = state }
                    .padding(16.dp)
                    .testTag(FeedbackTestTags.faq(index)),
            ) {
                Text(stringResource(question), style = MaterialTheme.typography.bodyLarge)
                if (isOpen) {
                    Text(
                        stringResource(answer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = photoInk(MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(top = 8.dp).testTag(FeedbackTestTags.answer(index)),
                    )
                }
            }
            HorizontalDivider()
        }
        SectionTitle(R.string.feedback_send_title)
        SendMethods(deviceInfo, onCopyDeviceInfo, onOpenForm, onOpenGitHub)
    }
}

/** 送る方法。フォームを主（先・目立つ形）、GitHub を従にする（F-77）。 */
@Composable
private fun SendMethods(
    deviceInfo: String,
    onCopyDeviceInfo: () -> Unit,
    onOpenForm: () -> Unit,
    onOpenGitHub: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.feedback_send_detail), style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = onOpenForm,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag(FeedbackTestTags.FORM),
        ) {
            Text(stringResource(R.string.feedback_open_form))
        }
        Text(
            stringResource(R.string.feedback_github_detail),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            deviceInfo,
            style = MaterialTheme.typography.bodyMedium,
            color = photoInk(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(vertical = 8.dp),
        )
        OutlinedButton(onClick = onCopyDeviceInfo, modifier = Modifier.testTag(FeedbackTestTags.COPY)) {
            Text(stringResource(R.string.feedback_copy_device_info))
        }
        OutlinedButton(
            onClick = onOpenGitHub,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag(FeedbackTestTags.GITHUB),
        ) {
            Text(stringResource(R.string.feedback_open_github))
        }
    }
}

@Composable
private fun SectionTitle(title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        color = photoInk(MaterialTheme.colorScheme.primary),
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp),
    )
}

object FeedbackTestTags {
    const val FORM = "feedback_form"
    const val COPY = "feedback_copy"
    const val GITHUB = "feedback_github"

    fun faq(index: Int) = "feedback_faq_$index"

    fun answer(index: Int) = "feedback_answer_$index"
}
