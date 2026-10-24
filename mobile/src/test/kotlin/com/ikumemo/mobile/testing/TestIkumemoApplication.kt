package com.ikumemo.mobile.testing

import com.ikumemo.mobile.IkumemoApplication

/** 単体テスト用の Application（robolectric.properties で指定）。起動時の通知の確認などを行わない。 */
class TestIkumemoApplication : IkumemoApplication() {
    override fun onStartup() = Unit
}
