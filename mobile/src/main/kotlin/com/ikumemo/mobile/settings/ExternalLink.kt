package com.ikumemo.mobile.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.net.toUri
import com.ikumemo.mobile.R

/**
 * アプリの外（ブラウザ・ストア）で開くページ。アプリ自体は通信せず、利用者が押した時だけブラウザで開く（PLAN §5・§7.3）。
 * ご意見フォームは転送ページ経由で開くため [openFeedbackForm] に分ける（URL に情報を付けるので固定の URL にならない）。
 */
enum class ExternalLink(
    val url: String,
) {
    PRIVACY_POLICY("https://github.com/filderschoice/ikumemo/blob/main/docs/store/PRIVACY_POLICY.md"),
    GITHUB("https://github.com/filderschoice/ikumemo"),
    GITHUB_ISSUE("https://github.com/filderschoice/ikumemo/issues/new/choose"),

    /** 評価のお願いはストアのページを開くだけにする（Play In-App Review は使わない、PLAN §5）。 */
    STORE("https://play.google.com/store/apps/details?id=$RELEASE_APPLICATION_ID"),
    ;

    fun intent(): Intent = Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/** ご意見フォームへの転送ページ（site/feedback/）。Google フォームの URL と entry 番号は転送ページだけが持つ（PLAN §7.3）。 */
private const val FEEDBACK_FORM_PAGE = "https://filderschoice.github.io/ikumemo/feedback/"

/** 転送ページが読むクエリ名（site/feedback/index.html と合わせる。変えない）。 */
private const val FEEDBACK_INFO_QUERY = "info"

/** 転送ページの URL に、ご意見に添える情報をエンコードして付ける（日本語・「／」・空白・& ・+ ・= を含んでも元の文字列に戻る）。 */
fun feedbackFormUrl(deviceInfo: String): String =
    FEEDBACK_FORM_PAGE
        .toUri()
        .buildUpon()
        .appendQueryParameter(FEEDBACK_INFO_QUERY, deviceInfo)
        .build()
        .toString()

/** ご意見フォームを外部のブラウザで開く。版・Android の版・機種名は転送ページ経由でフォームへ入る。 */
fun Context.openFeedbackForm() {
    openUrl(feedbackFormUrl(feedbackDeviceInfo()).toUri())
}

/** ストアに出す版の applicationId（debug は末尾に .debug が付くため、ストアのページは常にこちらを開く）。 */
private const val RELEASE_APPLICATION_ID = "com.ikumemo.mobile"

/** ブラウザを開く。開けるアプリが無ければ知らせる。 */
fun Context.openExternal(link: ExternalLink) {
    openUrl(link.url.toUri())
}

private fun Context.openUrl(uri: Uri) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.about_no_browser, Toast.LENGTH_SHORT).show()
    }
}

/** アプリの版（例 1.0.0）。 */
fun Context.appVersionName(): String = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

/** ご意見に添える情報（アプリの版・Android の版・機種名）。個人を特定する情報は含めない（D-21）。 */
fun Context.feedbackDeviceInfo(): String =
    getString(
        R.string.feedback_device_info,
        appVersionName(),
        Build.VERSION.RELEASE,
        "${Build.MANUFACTURER} ${Build.MODEL}",
    )
