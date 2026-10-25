package com.ikumemo.mobile.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParser

/**
 * BL-083 のバックアップの対象（F-71、PLAN §4.3）。クラウドからは写真のフォルダだけを外し、端末間転送は全部を移す。
 * 写真の保存先の名前（[PhotoStore.PHOTOS_DIR]）を変えたときに、ここが追従していないことに気づけるようにする。
 */
@RunWith(RobolectricTestRunner::class)
class BackupRulesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val photosPath = "${PhotoStore.PHOTOS_DIR}/"

    /** 区分（cloud-backup など）ごとに、外す・含める指定を「exclude file:photos/」の形で集める。 */
    private fun rules(xml: Int): Map<String, List<String>> {
        val parser = context.resources.getXml(xml)
        val result = linkedMapOf<String, MutableList<String>>()
        var section = ""
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "include", "exclude" -> {
                    val domain = parser.getAttributeValue(null, "domain") ?: parser.getAttributeValue(ANDROID, "domain")
                    val path = parser.getAttributeValue(null, "path") ?: parser.getAttributeValue(ANDROID, "path")
                    result.getOrPut(section) { mutableListOf() } += "${parser.name} $domain:$path"
                }
                else -> section = parser.name.also { result.getOrPut(it) { mutableListOf() } }
            }
        }
        return result
    }

    @Test
    fun cloudBackupSkipsOnlyPhotosAndDeviceTransferKeepsEverything() {
        val rules = rules(R.xml.data_extraction_rules)

        assertEquals(listOf("exclude file:$photosPath"), rules["cloud-backup"])
        assertEquals(emptyList<String>(), rules["device-transfer"])
    }

    @Test
    fun olderAndroidSkipsOnlyPhotos() {
        val rules = rules(R.xml.backup_rules)

        assertEquals(listOf("exclude file:$photosPath"), rules["full-backup-content"])
    }

    private companion object {
        const val ANDROID = "http://schemas.android.com/apk/res/android"
    }
}
