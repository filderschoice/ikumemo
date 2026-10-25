package com.ikumemo.mobile.reminder

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.db.PurchaseRecordEntity
import com.ikumemo.mobile.data.db.ReminderRow
import java.time.LocalDate

@Composable
fun ReminderScreen(viewModel: ReminderViewModel = viewModel(factory = AppViewModelFactory.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val refresh = {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        viewModel.onEvent(ReminderEvent.NotificationStateChanged(enabled))
    }
    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose {}
    }
    val permission = rememberNotificationPermission(state, viewModel::onEvent, refresh)
    val onEvent: (ReminderEvent) -> Unit = { event ->
        viewModel.onEvent(event)
        if (event is ReminderEvent.SetDue) permission.askAfterFirstDue()
    }
    ReminderContent(state = state, onEvent = onEvent, onEnableNotifications = permission::enable)
    state.dueTarget?.let { DueSheet(target = it, today = state.today, onEvent = onEvent) }
    permission.Dialog()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderContent(
    state: ReminderUiState,
    onEvent: (ReminderEvent) -> Unit,
    onEnableNotifications: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.tab_reminder)) }, windowInsets = WindowInsets(0))
        LazyColumn(Modifier.fillMaxSize()) {
            if (!state.notificationsEnabled) {
                item { NotificationsOffBanner(onEnableNotifications) }
            }
            item { SectionTitle(R.string.reminder_recent) }
            if (state.loaded && state.recent.isEmpty()) {
                item { Hint(R.string.reminder_recent_empty) }
            }
            items(state.recent, key = { "recent_${it.id}" }) { record ->
                RecentRow(record) { onEvent(ReminderEvent.OpenDue(DueTarget(record.id, record.displayName))) }
                HorizontalDivider()
            }
            item { SectionTitle(R.string.reminder_pending) }
            if (state.loaded && state.pending.isEmpty()) {
                item { Hint(R.string.reminder_pending_empty) }
            }
            items(state.pending, key = { "pending_${it.reminder.id}" }) { row ->
                PendingRow(row, state.today, onEvent)
                HorizontalDivider()
            }
        }
    }
}

/** 「通知がオフのため、おしらせが届きません」＋「通知をオンにする」（R-29）。 */
@Composable
private fun NotificationsOffBanner(onEnable: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag(ReminderTestTags.NOTIFICATIONS_OFF),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Rounded.NotificationsOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    stringResource(R.string.reminder_notifications_off),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Button(onClick = onEnable, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.reminder_enable_notifications))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
}

@Composable
private fun Hint(text: Int) {
    Text(
        stringResource(text),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** 最近買った物の 1 行。「期限を決める」（G-08 で照らす場所）。 */
@Composable
private fun RecentRow(
    record: PurchaseRecordEntity,
    onSetDue: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(record.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    R.string.reminder_bought_on,
                    record.purchasedDate.monthValue,
                    record.purchasedDate.dayOfMonth,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = onSetDue,
            modifier = Modifier.heightIn(min = 48.dp).testTag(ReminderTestTags.setDue(record.id)),
        ) {
            Text(stringResource(R.string.reminder_set_due))
        }
    }
}

/** 期限が近い順の 1 行。期限切れは目立つ色＋「期限切れ」の文字（F-53・R-22）。押すと期限を決め直せる。 */
@Composable
private fun PendingRow(
    row: ReminderRow,
    today: LocalDate,
    onEvent: (ReminderEvent) -> Unit,
) {
    val due = row.reminder.dueDate
    val overdue = due < today
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onEvent(ReminderEvent.OpenDue(DueTarget(row.reminder.purchaseRecordId, row.displayName, due)))
                }.padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(ReminderTestTags.pending(row.reminder.id)),
    ) {
        Text(row.displayName, style = MaterialTheme.typography.bodyLarge)
        Text(
            dueLabel(due, today),
            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = { onEvent(ReminderEvent.UsedUp(row.reminder.id)) },
                modifier = Modifier.heightIn(min = 48.dp).testTag(ReminderTestTags.usedUp(row.reminder.id)),
            ) {
                Text(stringResource(R.string.reminder_used_up))
            }
            TextButton(
                onClick = { onEvent(ReminderEvent.Discarded(row.reminder.id)) },
                modifier = Modifier.heightIn(min = 48.dp).testTag(ReminderTestTags.discarded(row.reminder.id)),
            ) {
                Text(stringResource(R.string.reminder_discarded))
            }
        }
    }
}

@Composable
private fun dueLabel(
    due: LocalDate,
    today: LocalDate,
): String =
    when {
        due < today -> stringResource(R.string.reminder_overdue, due.monthValue, due.dayOfMonth)
        due == today -> stringResource(R.string.reminder_due_today, due.monthValue, due.dayOfMonth)
        due == today.plusDays(1) -> stringResource(R.string.reminder_due_tomorrow, due.monthValue, due.dayOfMonth)
        else -> stringResource(R.string.reminder_due_on, due.monthValue, due.dayOfMonth)
    }

/** 端末の設定のこのアプリの通知の画面を開く（R-29）。 */
fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

object ReminderTestTags {
    const val NOTIFICATIONS_OFF = "reminder_notifications_off"

    fun setDue(recordId: Long) = "reminder_set_due_$recordId"

    fun pending(reminderId: Long) = "reminder_pending_$reminderId"

    fun usedUp(reminderId: Long) = "reminder_used_up_$reminderId"

    fun discarded(reminderId: Long) = "reminder_discarded_$reminderId"

    fun choice(choice: DueChoice) = "reminder_choice_${choice.name}"
}
