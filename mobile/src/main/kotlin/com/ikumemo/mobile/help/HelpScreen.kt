package com.ikumemo.mobile.help

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.R
import com.ikumemo.mobile.guide.GuideId
import com.ikumemo.mobile.guide.GuideViewModel
import com.ikumemo.mobile.settings.SettingsScaffold

/** 使い方の目次（H-01）。先頭に「はじめての案内をもう一度見る」（H-02）。 */
@Composable
fun HelpIndexScreen(
    onBack: () -> Unit,
    onOpenPage: (String) -> Unit,
    guideViewModel: GuideViewModel = viewModel(factory = AppViewModelFactory.Factory),
) {
    val context = LocalContext.current
    val chapters = remember(context) { HelpRepository(context.assets).chapters() }
    SettingsScaffold(title = R.string.settings_help, onBack = onBack) {
        HelpIndexContent(chapters, onOpenPage, onShowOnboarding = guideViewModel::showOnboardingAgain)
    }
}

@Composable
fun HelpIndexContent(
    chapters: List<HelpChapter>,
    onOpenPage: (String) -> Unit,
    onShowOnboarding: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        OutlinedButton(
            onClick = onShowOnboarding,
            modifier = Modifier.padding(16.dp).heightIn(min = 48.dp).testTag(HelpTestTags.ONBOARDING),
        ) {
            Text(stringResource(R.string.help_show_onboarding))
        }
        chapters.forEach { chapter ->
            Text(
                chapter.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 4.dp,
                    ).semantics { heading() },
            )
            chapter.pages.forEach { page ->
                Text(
                    page.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onOpenPage(page.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag(HelpTestTags.page(page.id)),
                )
                HorizontalDivider()
            }
        }
    }
}

/**
 * 使い方の 1 ページ（H-01）。下に「ガイドをもう一度見る」（その場のガイドを出し直してその画面を開く）と「やってみる」（その画面を開く）（H-02）。
 */
@Composable
fun HelpPageScreen(
    pageId: String,
    onBack: () -> Unit,
    onOpenScreen: (String) -> Unit,
    guideViewModel: GuideViewModel = viewModel(factory = AppViewModelFactory.Factory),
) {
    val context = LocalContext.current
    val page = remember(context, pageId) { HelpRepository(context.assets).page(pageId) }
    SettingsScaffold(title = R.string.settings_help, onBack = onBack) {
        if (page == null) {
            Text(stringResource(R.string.help_not_found), modifier = Modifier.padding(16.dp))
        } else {
            HelpPageContent(
                page = page,
                onTry = onOpenScreen,
                onShowGuide = { guide, screen ->
                    guideViewModel.showGuideAgain(guide)
                    onOpenScreen(screen)
                },
            )
        }
    }
}

@Composable
fun HelpPageContent(
    page: HelpPage,
    onTry: (String) -> Unit,
    onShowGuide: (GuideId, String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(page.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        page.blocks.forEach { HelpBlockView(it) }
        val screen = page.tryScreen ?: return@Column
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            page.guide?.let { guide ->
                OutlinedButton(
                    onClick = { onShowGuide(guide, screen) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag(HelpTestTags.SHOW_GUIDE),
                ) {
                    Text(stringResource(R.string.help_show_guide))
                }
            }
            Button(onClick = { onTry(screen) }, modifier = Modifier.heightIn(min = 48.dp).testTag(HelpTestTags.TRY)) {
                Text(stringResource(R.string.help_try))
            }
        }
    }
}

@Composable
private fun HelpBlockView(block: HelpBlock) {
    when (block) {
        is HelpBlock.Heading ->
            Text(block.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        is HelpBlock.Paragraph -> Text(block.text, style = MaterialTheme.typography.bodyLarge)
        is HelpBlock.Bullets ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                block.items.forEach { item ->
                    Row {
                        Text("・", style = MaterialTheme.typography.bodyLarge)
                        Text(item, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
    }
}

/** 「やってみる」で開く画面（正本の `try:` に書く名前）。 */
object HelpScreens {
    const val MEMO = "memo"
    const val ADD_TEXT = "add_text"
    const val CALENDAR = "calendar"
    const val REMINDER = "reminder"
    const val APPEARANCE = "appearance"
    const val HOME_SCREEN = "home_screen"
    const val FEEDBACK = "feedback"
    const val DATA = "data"
}

/** 困ったときの入口（H-03）から開くページ。 */
object HelpPages {
    const val VOICE = "voice"
    const val NO_VOICE = "no-voice"
    const val NOT_ARRIVING = "not-arriving"
}

object HelpTestTags {
    const val ONBOARDING = "help_onboarding"
    const val SHOW_GUIDE = "help_show_guide"
    const val TRY = "help_try"

    fun page(id: String) = "help_page_$id"
}
