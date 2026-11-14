package com.ikumemo.mobile.designsystem

import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * 種の色（見本の色、BL-177）から、明るい・暗いの配色（[IkumemoPalette] の全項目）を作る。
 *
 * Material 3 の配色の作り方（種の色の色相から色の系統を 5 つ作り、役割ごとに決まった明るさの色を取る）にならい、
 * 明るさには CIELAB の L*（Material の HCT の Tone と同じ尺度）を使う。L* が決まれば輝度も決まるため、
 * 文字と面の明るさの差を Material と同じ組み合わせ（例 40 と 100、10 と 90）にすれば、どの種の色でも
 * コントラスト比 4.5:1 以上になる（色が sRGB に収まらないときは、明るさを保ったまま彩度だけを下げる）。
 * 依存を増やさないため、Material の配色計算ライブラリではなく CIELAB の近似で実装している。
 */
internal object SeedPalette {
    fun light(seed: Long): IkumemoPalette = Scheme.of(seed).light()

    fun dark(seed: Long): IkumemoPalette = Scheme.of(seed).dark()

    private class Scheme(
        val primary: Tonal,
        val secondary: Tonal,
        val tertiary: Tonal,
        val neutral: Tonal,
        val neutralVariant: Tonal,
    ) {
        fun light(): IkumemoPalette =
            IkumemoPalette(
                primary = primary.tone(40.0),
                onPrimary = primary.tone(100.0),
                primaryContainer = primary.tone(90.0),
                onPrimaryContainer = primary.tone(10.0),
                secondary = secondary.tone(40.0),
                onSecondary = secondary.tone(100.0),
                secondaryContainer = secondary.tone(90.0),
                onSecondaryContainer = secondary.tone(10.0),
                tertiary = tertiary.tone(40.0),
                onTertiary = tertiary.tone(100.0),
                tertiaryContainer = tertiary.tone(90.0),
                onTertiaryContainer = tertiary.tone(10.0),
                background = neutral.tone(98.0),
                onBackground = neutral.tone(10.0),
                surface = neutral.tone(98.0),
                onSurface = neutral.tone(10.0),
                surfaceVariant = neutralVariant.tone(90.0),
                onSurfaceVariant = neutralVariant.tone(30.0),
                outline = neutralVariant.tone(50.0),
            )

        fun dark(): IkumemoPalette =
            IkumemoPalette(
                primary = primary.tone(80.0),
                onPrimary = primary.tone(20.0),
                primaryContainer = primary.tone(30.0),
                onPrimaryContainer = primary.tone(90.0),
                secondary = secondary.tone(80.0),
                onSecondary = secondary.tone(20.0),
                secondaryContainer = secondary.tone(30.0),
                onSecondaryContainer = secondary.tone(90.0),
                tertiary = tertiary.tone(80.0),
                onTertiary = tertiary.tone(20.0),
                tertiaryContainer = tertiary.tone(30.0),
                onTertiaryContainer = tertiary.tone(90.0),
                background = neutral.tone(6.0),
                onBackground = neutral.tone(90.0),
                surface = neutral.tone(6.0),
                onSurface = neutral.tone(90.0),
                surfaceVariant = neutralVariant.tone(30.0),
                onSurfaceVariant = neutralVariant.tone(80.0),
                outline = neutralVariant.tone(60.0),
            ).withDarkError()

        companion object {
            /** 彩度は CIELAB の C*。主の色は種の彩度を活かしつつ、くすんだ種でも色が分かる程度に下限を設ける。 */
            fun of(seed: Long): Scheme {
                val lch = Lab.fromArgb(seed).toLch()
                val hue = lch.hue
                return Scheme(
                    primary = Tonal(hue, lch.chroma.coerceIn(PRIMARY_MIN_CHROMA, PRIMARY_MAX_CHROMA)),
                    secondary = Tonal(hue, SECONDARY_CHROMA),
                    tertiary = Tonal(hue + TERTIARY_HUE_SHIFT, TERTIARY_CHROMA),
                    neutral = Tonal(hue, NEUTRAL_CHROMA),
                    neutralVariant = Tonal(hue, NEUTRAL_VARIANT_CHROMA),
                )
            }

            private const val PRIMARY_MIN_CHROMA = 40.0
            private const val PRIMARY_MAX_CHROMA = 80.0
            private const val SECONDARY_CHROMA = 20.0
            private const val TERTIARY_HUE_SHIFT = 60.0
            private const val TERTIARY_CHROMA = 30.0
            private const val NEUTRAL_CHROMA = 5.0
            private const val NEUTRAL_VARIANT_CHROMA = 10.0
        }
    }

    /** 1 つの色の系統（色相と彩度を固定し、明るさだけを変える）。 */
    private class Tonal(
        val hue: Double,
        val chroma: Double,
    ) {
        /** 明るさ [tone]（L*、0〜100）の色。sRGB に収まらなければ彩度を下げる（明るさは変えない）。 */
        fun tone(tone: Double): Long {
            val full = rgbAt(tone, chroma)
            if (full.inGamut()) return full.toArgb()
            // 彩度 0（灰色）は必ず収まるので、収まる最大の彩度を二分探索で探す
            var low = 0.0
            var high = chroma
            repeat(SEARCH_STEPS) {
                val mid = (low + high) / 2
                if (rgbAt(tone, mid).inGamut()) low = mid else high = mid
            }
            return rgbAt(tone, low).toArgb()
        }

        private fun rgbAt(
            tone: Double,
            chroma: Double,
        ): LinearRgb = Lch(tone, chroma, hue).toLab().toLinearRgb()

        private companion object {
            const val SEARCH_STEPS = 24
        }
    }
}

/** CIELAB の色（D65）。 */
internal data class Lab(
    val l: Double,
    val a: Double,
    val b: Double,
) {
    fun toLch(): Lch {
        val hue = Math.toDegrees(atan2(b, a))
        return Lch(l, hypot(a, b), if (hue < 0) hue + FULL_TURN else hue)
    }

    fun toLinearRgb(): LinearRgb {
        val fy = (l + LAB_OFFSET) / LAB_SCALE
        val x = WHITE_X * finv(fy + a / A_SCALE)
        val y = WHITE_Y * finv(fy)
        val z = WHITE_Z * finv(fy - b / B_SCALE)
        return LinearRgb(
            r = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z,
            g = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z,
            b = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z,
        )
    }

    companion object {
        fun fromArgb(argb: Long): Lab {
            val r = linearize((argb shr 16) and 0xFF)
            val g = linearize((argb shr 8) and 0xFF)
            val b = linearize(argb and 0xFF)
            val x = 0.4124564 * r + 0.3575761 * g + 0.1804375 * b
            val y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
            val z = 0.0193339 * r + 0.1191920 * g + 0.9503041 * b
            val fx = f(x / WHITE_X)
            val fy = f(y / WHITE_Y)
            val fz = f(z / WHITE_Z)
            return Lab(LAB_SCALE * fy - LAB_OFFSET, A_SCALE * (fx - fy), B_SCALE * (fy - fz))
        }

        private fun linearize(channel: Long): Double {
            val c = channel / CHANNEL_MAX
            return if (c <= SRGB_THRESHOLD) c / SRGB_LINEAR_SLOPE else ((c + SRGB_A) / (1 + SRGB_A)).pow(SRGB_GAMMA)
        }

        private fun f(t: Double): Double = if (t > EPSILON) cbrt(t) else t / (3 * DELTA * DELTA) + 4.0 / 29.0

        private fun finv(t: Double): Double = if (t > DELTA) t * t * t else 3 * DELTA * DELTA * (t - 4.0 / 29.0)

        private const val DELTA = 6.0 / 29.0
        private const val EPSILON = DELTA * DELTA * DELTA
        private const val LAB_SCALE = 116.0
        private const val LAB_OFFSET = 16.0
        private const val A_SCALE = 500.0
        private const val B_SCALE = 200.0
        private const val WHITE_X = 0.95047
        private const val WHITE_Y = 1.0
        private const val WHITE_Z = 1.08883
    }
}

/** CIELAB を明るさ・彩度・色相（度）で表した色。 */
internal data class Lch(
    val lightness: Double,
    val chroma: Double,
    val hue: Double,
) {
    fun toLab(): Lab {
        val radians = Math.toRadians(hue)
        return Lab(lightness, chroma * cos(radians), chroma * sin(radians))
    }
}

/** 線形の sRGB（0〜1。範囲外なら sRGB で表せない色）。 */
internal data class LinearRgb(
    val r: Double,
    val g: Double,
    val b: Double,
) {
    fun inGamut(): Boolean = listOf(r, g, b).all { it in -GAMUT_TOLERANCE..1 + GAMUT_TOLERANCE }

    fun toArgb(): Long = OPAQUE or (encode(r) shl 16) or (encode(g) shl 8) or encode(b)

    private fun encode(linear: Double): Long {
        val c = linear.coerceIn(0.0, 1.0)
        val srgb = if (c <= SRGB_LINEAR_LIMIT) c * SRGB_LINEAR_SLOPE else (1 + SRGB_A) * c.pow(1 / SRGB_GAMMA) - SRGB_A
        return (srgb * CHANNEL_MAX).roundToLong().coerceIn(0L, CHANNEL_MAX.toLong())
    }

    private companion object {
        const val GAMUT_TOLERANCE = 1e-4
        const val OPAQUE = 0xFF000000L
        const val SRGB_LINEAR_LIMIT = 0.0031308
    }
}

private const val FULL_TURN = 360.0
private const val CHANNEL_MAX = 255.0
private const val SRGB_THRESHOLD = 0.04045
private const val SRGB_LINEAR_SLOPE = 12.92
private const val SRGB_A = 0.055
private const val SRGB_GAMMA = 2.4
