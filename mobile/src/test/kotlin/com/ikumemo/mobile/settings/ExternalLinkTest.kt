package com.ikumemo.mobile.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** BL-093 の完了条件「外部のページを開く Intent」。ご意見に添える情報に個人を特定する物を入れない（D-21）。 */
@RunWith(RobolectricTestRunner::class)
class ExternalLinkTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun linksOpenInBrowser() {
        ExternalLink.entries.forEach { link ->
            val intent = link.intent()
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals(link.url, intent.dataString)
            assertTrue(link.url, link.url.startsWith("https://"))
        }
        assertEquals(
            "https://github.com/filderschoice/ikumemo/issues/new/choose",
            ExternalLink.GITHUB_ISSUE.intent().dataString,
        )
    }

    @Test
    fun storePageIsTheReleaseApp() {
        // debug 版（.debug 付き）で開いても、ストアに出す版のページを開く
        assertTrue(ExternalLink.STORE.url.endsWith("?id=com.ikumemo.mobile"))
    }

    @Test
    fun feedbackFormUrlEncodesDeviceInfoAndRestoresIt() {
        val info = "アプリの版 1.0.0 ／ Android 14 ／ 機種 A&B+C=D"

        val url = feedbackFormUrl(info)

        assertTrue(url, url.startsWith("https://filderschoice.github.io/ikumemo/feedback/?info="))
        // 日本語・空白・「／」・& ・+ ・= は生のまま残らない
        val query = url.substringAfter("?info=")
        listOf(" ", "／", "&", "+", "=", "ア").forEach { assertTrue("$it in $query", it !in query) }
        // 転送ページ（URLSearchParams）と同じ読み方で元の文字列に戻る
        assertEquals(info, url.toUri().getQueryParameter("info"))
    }

    @Test
    fun deviceInfoHasOnlyVersionsAndModel() {
        val info = context.feedbackDeviceInfo()

        val model = "${Build.MANUFACTURER} ${Build.MODEL}"
        assertEquals("アプリの版 ${context.appVersionName()} ／ Android ${Build.VERSION.RELEASE} ／ 機種 $model", info)
    }
}
