package com.ikumemo.mobile.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.ikumemo.mobile.R
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

/** 使っているオープンソースのライセンス一覧（AboutLibraries がビルド時に作る一覧を出す、PLAN §7）。 */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    SettingsScaffold(title = R.string.about_licenses, onBack = onBack) {
        LibrariesContainer(modifier = Modifier.fillMaxSize().testTag(LICENSES_TAG))
    }
}

const val LICENSES_TAG = "licenses_list"
