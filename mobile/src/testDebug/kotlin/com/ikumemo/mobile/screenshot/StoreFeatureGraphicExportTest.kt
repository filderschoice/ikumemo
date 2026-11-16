package com.ikumemo.mobile.screenshot

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.ikumemo.mobile.R
import com.ikumemo.mobile.testing.SlowTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Google Play のフィーチャーグラフィック（1024 × 500 の PNG、BL-215）の案を書き出す。
 * ポップのテーマの地色（背景 `#FFF4EC`）に、アプリのアイコン（`ic_launcher_background` と `ic_launcher_foreground`）、
 * アプリ名「いくメモ」、キャッチコピー「話すだけ、いく前メモ。」を置く。文字はテスト環境の既定のフォントで描き、外部のフォントは使わない。
 *
 * 書き出し先は `docs/store/images/feature-graphic.png`。再実行は
 * `./gradlew :mobile:testDebugUnitTest -PslowTests --tests "*StoreFeatureGraphicExportTest*"`（遅いテスト扱いで通常のゲートでは流さない）。
 */
@Category(SlowTest::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class StoreFeatureGraphicExportTest {
    @Test
    fun exportsFeatureGraphic() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BACKGROUND)

        drawIcon(context, canvas)

        val name = textPaint(NAME_COLOR, NAME_SIZE)
        val copy = textPaint(COPY_COLOR, COPY_SIZE)
        canvas.drawText("いくメモ", TEXT_LEFT, NAME_BASELINE, name)
        canvas.drawText("話すだけ、いく前メモ。", TEXT_LEFT, COPY_BASELINE, copy)

        // 文字が右端で切れないこと（余白を残す）と、文字が実際に描かれたことを確かめる
        val rightEdge = TEXT_LEFT + maxOf(name.measureText("いくメモ"), copy.measureText("話すだけ、いく前メモ。"))
        assertTrue("文字の右端 $rightEdge が余白を超えた", rightEdge <= WIDTH - MARGIN)
        assertEquals(BACKGROUND, bitmap.getPixel(WIDTH - 1, HEIGHT - 1))
        assertTrue(
            "コピーが描かれていない",
            (TEXT_LEFT.toInt() until rightEdge.toInt()).any { x ->
                (COPY_BASELINE.toInt() - COPY_SIZE.toInt()..COPY_BASELINE.toInt()).any { y ->
                    bitmap.getPixel(x, y) != BACKGROUND
                }
            },
        )

        val out = File("../docs/store/images/feature-graphic.png")
        out.parentFile?.mkdirs()
        out.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }

    private fun drawIcon(
        context: android.content.Context,
        canvas: Canvas,
    ) {
        // アイコンは見える 72dp を ICON_PX に拡大し、角を丸めて左に置く
        val icon = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
        val iconCanvas = Canvas(icon)
        iconCanvas.drawColor(ContextCompat.getColor(context, R.color.ic_launcher_background))
        val full = ICON_PX * ADAPTIVE_FULL_DP / ADAPTIVE_VISIBLE_DP
        val offset = ICON_PX * ADAPTIVE_OFFSET_DP / ADAPTIVE_VISIBLE_DP
        val foreground = requireNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground))
        foreground.setBounds(-offset, -offset, full - offset, full - offset)
        foreground.draw(iconCanvas)

        val left = MARGIN
        val top = (HEIGHT - ICON_PX) / 2f
        val clamp = Shader.TileMode.CLAMP
        val shader = BitmapShader(icon, clamp, clamp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        canvas.save()
        canvas.translate(left, top)
        canvas.drawRoundRect(0f, 0f, ICON_PX.toFloat(), ICON_PX.toFloat(), ICON_RADIUS, ICON_RADIUS, paint)
        canvas.restore()
    }

    private fun textPaint(
        argb: Int,
        size: Float,
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = argb
        textSize = size
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private companion object {
        const val WIDTH = 1024
        const val HEIGHT = 500
        const val MARGIN = 64f
        const val ICON_PX = 320
        const val ICON_RADIUS = 72f
        const val TEXT_LEFT = 448f
        const val NAME_SIZE = 96f
        const val COPY_SIZE = 44f
        const val NAME_BASELINE = 232f
        const val COPY_BASELINE = 320f
        const val BACKGROUND = 0xFFFFF4EC.toInt()
        const val NAME_COLOR = 0xFFB5381C.toInt()
        const val COPY_COLOR = 0xFF261813.toInt()
        const val ADAPTIVE_FULL_DP = 108
        const val ADAPTIVE_VISIBLE_DP = 72
        const val ADAPTIVE_OFFSET_DP = 18
    }
}
