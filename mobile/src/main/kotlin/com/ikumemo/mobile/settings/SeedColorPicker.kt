package com.ikumemo.mobile.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import com.ikumemo.mobile.designsystem.ContrastRatio
import com.ikumemo.mobile.designsystem.SeedSwatch
import com.ikumemo.mobile.designsystem.ThemeChoice

/**
 * 「見本の色から選ぶ」（BL-177）。丸い見本を並べ、押すとその色を種にした配色（[ThemeChoice.Seed]）を選ぶ。
 * 押せる大きさは 48dp 以上にし、読み上げでは色の名前を伝える。選んでいる色には印を付ける。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SeedColorPicker(
    selected: ThemeChoice,
    onSelect: (ThemeChoice.Seed) -> Unit,
) {
    Text(stringResource(R.string.appearance_theme_seed), style = MaterialTheme.typography.bodyMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SeedSwatch.entries.forEach { swatch ->
            SeedSwatchButton(
                swatch = swatch,
                selected = swatch.choice == selected,
                onClick = { onSelect(swatch.choice) },
            )
        }
    }
}

@Composable
private fun SeedSwatchButton(
    swatch: SeedSwatch,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(swatch.labelRes)
    val ring = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(SWATCH_TOUCH.dp)
                .then(if (ring != null) Modifier.border(ring, CircleShape) else Modifier)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = label }
                .testTag(AppearanceTestTags.theme(swatch.choice)),
    ) {
        Surface(
            color = Color(swatch.argb),
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.padding(SWATCH_INSET.dp).size(SWATCH_DOT.dp),
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(checkColorOn(swatch.argb)),
                    modifier = Modifier.padding(SWATCH_INSET.dp),
                )
            }
        }
    }
}

/** 見本の上の印の色。白と黒のうち、見本の色との差が大きい方。 */
internal fun checkColorOn(argb: Long): Long =
    if (ContrastRatio.of(WHITE, argb) >= ContrastRatio.of(BLACK, argb)) WHITE else BLACK

private const val WHITE = 0xFFFFFFFFL
private const val BLACK = 0xFF1B1B1FL
private const val SWATCH_TOUCH = 52
private const val SWATCH_DOT = 40
private const val SWATCH_INSET = 6
