package com.ikumemo.mobile

import android.content.Context
import com.ikumemo.mobile.data.db.IkumemoDatabase
import java.time.Clock

/**
 * 手動 DI の入れ物（PLAN §4.1）。[IkumemoApplication] が 1 つだけ持ち、ViewModel・Worker・受信処理へ渡す。
 */
class AppContainer(
    context: Context,
    val clock: Clock = Clock.systemDefaultZone(),
) {
    private val appContext = context.applicationContext

    val database: IkumemoDatabase by lazy { IkumemoDatabase.create(appContext) }
}
