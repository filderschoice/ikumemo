package com.ikumemo.mobile.calendar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import com.ikumemo.mobile.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 「元に戻す」を出しておく時間。読み上げを使っているときは端末の推奨に合わせて延ばす（メモの「元に戻す」と同じ）。 */
private const val UNDO_MILLIS = 5_000L

/**
 * 記録を消した後に「消しました：品名 ／ 元に戻す」を出す（F-16）。押したら [CalendarEvent.UndoDelete]、
 * 時間が過ぎたら [CalendarEvent.UndoExpired] を送る。メモの「買った」「やめた」の元に戻す（F-12）と同じ作り。
 */
@Composable
internal fun RecordUndoEffect(
    offer: RecordUndoOffer?,
    snackbarHostState: SnackbarHostState,
    onEvent: (CalendarEvent) -> Unit,
) {
    val deletedFormat = stringResource(R.string.calendar_record_deleted_message)
    val undoLabel = stringResource(R.string.memo_undo)
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(offer?.serial) {
        offer ?: return@LaunchedEffect
        val timeout =
            accessibilityManager?.calculateRecommendedTimeoutMillis(
                UNDO_MILLIS,
                containsIcons = false,
                containsText = true,
                containsControls = true,
            ) ?: UNDO_MILLIS
        snackbarHostState.currentSnackbarData?.dismiss()
        val dismissJob =
            launch {
                delay(timeout)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        val text = deletedFormat.format(offer.deleted.record.displayName)
        val result = snackbarHostState.showSnackbar(text, undoLabel, duration = SnackbarDuration.Indefinite)
        dismissJob.cancel()
        if (result == SnackbarResult.ActionPerformed) {
            onEvent(CalendarEvent.UndoDelete(offer.serial))
        } else {
            onEvent(CalendarEvent.UndoExpired(offer.serial))
        }
    }
}
