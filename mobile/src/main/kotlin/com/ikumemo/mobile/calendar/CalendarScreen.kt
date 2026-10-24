package com.ikumemo.mobile.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R

/** カレンダー（F-40〜F-42）。画面の骨格だけ置き、中身は BL-042 で作る。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.tab_calendar)) }, windowInsets = WindowInsets(0))
        Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.calendar_preparing), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
