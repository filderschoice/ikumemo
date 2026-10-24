package com.ikumemo.mobile.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ikumemo.mobile.R
import com.ikumemo.mobile.designsystem.CalendarColors
import java.time.LocalDate
import java.time.YearMonth

private const val DAYS_IN_WEEK = 7
private const val MAX_DOTS = 3

/** 月の見出し（◀ 2026年10月 ▶）。 */
@Composable
fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, modifier = Modifier.testTag(CalendarTestTags.PREVIOUS)) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.calendar_previous_month),
            )
        }
        Text(
            stringResource(R.string.calendar_month, month.year, month.monthValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, modifier = Modifier.testTag(CalendarTestTags.NEXT)) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.calendar_next_month),
            )
        }
    }
}

/**
 * 月のカレンダー（F-40）。日曜始まり。日曜は赤・土曜は青の文字色＋曜日名。買い物のあった日にお店の色のドット
 * （G-07 で照らす場所）。祝日は通信が要るため出さない（PLAN §2.7）。
 */
@Composable
fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    shoppingDays: Map<LocalDate, List<Long?>>,
    onSelect: (LocalDate) -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val weekdays = stringResource(R.string.calendar_weekdays).split(",")
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEachIndexed { index, label ->
                Text(
                    label,
                    color = weekdayColor(index, dark),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // 日曜始まりにするため、1 日の曜日（日曜 = 0）の数だけ空ける
        val leading = month.atDay(1).dayOfWeek.value % DAYS_IN_WEEK
        val cells = List(leading) { null } + (1..month.lengthOfMonth()).map(month::atDay)
        cells.chunked(DAYS_IN_WEEK).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEachIndexed { index, date ->
                    Box(Modifier.weight(1f)) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                selected = date == selectedDate,
                                isToday = date == today,
                                stores = shoppingDays[date].orEmpty(),
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
                repeat(DAYS_IN_WEEK - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun weekdayColor(
    indexFromSunday: Int,
    dark: Boolean,
): Color =
    when (indexFromSunday) {
        0 -> Color(CalendarColors.sunday(dark))
        DAYS_IN_WEEK - 1 -> Color(CalendarColors.saturday(dark))
        else -> MaterialTheme.colorScheme.onSurface
    }

@Composable
private fun DayCell(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    stores: List<Long?>,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val textColor = weekdayColor(date.dayOfWeek.value % DAYS_IN_WEEK, dark)
    val description =
        if (stores.isEmpty()) {
            stringResource(R.string.calendar_day, date.monthValue, date.dayOfMonth)
        } else {
            stringResource(R.string.calendar_day_with_shopping, date.monthValue, date.dayOfMonth)
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .heightIn(min = 48.dp)
                .padding(2.dp)
                .background(if (selected) colors.secondaryContainer else Color.Transparent, MaterialTheme.shapes.small)
                .then(if (isToday) Modifier.border(2.dp, colors.primary, MaterialTheme.shapes.small) else Modifier)
                .clickable(onClick = onClick)
                .semantics { contentDescription = description }
                .testTag(CalendarTestTags.day(date)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            color = if (selected) colors.onSecondaryContainer else textColor,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            stores.take(MAX_DOTS).forEach { storeId ->
                Box(Modifier.size(6.dp).background(Color(CalendarColors.storeDot(storeId, dark)), CircleShape))
            }
        }
    }
}

object CalendarTestTags {
    const val PREVIOUS = "calendar_previous"
    const val NEXT = "calendar_next"
    const val SEARCH = "calendar_search"
    const val SEARCH_INPUT = "calendar_search_input"
    const val RECORD_DELETE = "calendar_record_delete"
    const val RECORD_SAVE = "calendar_record_save"

    fun day(date: LocalDate) = "calendar_day_$date"

    fun record(id: Long) = "calendar_record_$id"

    fun result(id: Long) = "calendar_result_$id"
}
