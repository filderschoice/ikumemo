package com.ikumemo.mobile.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.R
import com.ikumemo.mobile.testing.SlowTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Google Play のストア掲載用アプリアイコン（512 × 512 の 32 ビット PNG、BL-214）を書き出す。
 * アダプティブアイコンの背景色（`ic_launcher_background`）と前景（`ic_launcher_foreground`）を重ね、
 * ランチャーで見える中央 72dp（108dp の 18〜90）を 512px に拡大する。角の丸めは Play が行うため四隅まで背景色で塗る。
 *
 * 書き出し先は `docs/store/images/icon-512.png`。再実行は
 * `./gradlew :mobile:testDebugUnitTest -PslowTests --tests "*StoreIconExportTest*"`（遅いテスト扱いで通常のゲートでは流さない）。
 */
@Category(SlowTest::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class StoreIconExportTest {
    @Test
    fun exportsStoreIcon() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bitmap = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(ContextCompat.getColor(context, R.color.ic_launcher_background))

        // 前景は 108dp 全体を 768px で描き、見える範囲（18〜90）が 512px に収まるよう左上をずらす
        val full = ICON_PX * ADAPTIVE_FULL_DP / ADAPTIVE_VISIBLE_DP
        val offset = ICON_PX * ADAPTIVE_OFFSET_DP / ADAPTIVE_VISIBLE_DP
        val foreground = requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground))
        foreground.setBounds(-offset, -offset, full - offset, full - offset)
        foreground.draw(canvas)

        // 四隅が背景色のまま（透明でも吹き出しでもない）ことを確かめる
        val background = ContextCompat.getColor(context, R.color.ic_launcher_background)
        listOf(0 to 0, ICON_PX - 1 to 0, 0 to ICON_PX - 1, ICON_PX - 1 to ICON_PX - 1).forEach { (x, y) ->
            assertEquals(background, bitmap.getPixel(x, y))
        }
        assertEquals(Color.WHITE, bitmap.getPixel(ICON_PX / 2, ICON_PX / 4))

        val out = File("../docs/store/images/icon-512.png")
        out.parentFile?.mkdirs()
        out.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }

    private companion object {
        const val ICON_PX = 512
        const val ADAPTIVE_FULL_DP = 108
        const val ADAPTIVE_VISIBLE_DP = 72
        const val ADAPTIVE_OFFSET_DP = 18
    }
}
