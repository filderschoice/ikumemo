package com.ikumemo.mobile

import android.content.BroadcastReceiver
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 受信処理の中で suspend 関数を動かす（goAsync で処理が終わるまで受信を保つ）。 */
fun BroadcastReceiver.runAsync(
    context: Context,
    block: suspend (AppContainer) -> Unit,
) {
    val pending = goAsync()
    val container = (context.applicationContext as IkumemoApplication).container
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            block(container)
        } finally {
            pending.finish()
        }
    }
}
