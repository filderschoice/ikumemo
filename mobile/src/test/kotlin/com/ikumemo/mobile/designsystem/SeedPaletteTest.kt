package com.ikumemo.mobile.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** BL-177「見本の色から 1 色選ぶ」の配色の作り方と保存値。文字と面の色の比は ThemeContrastTest と同じ基準で確かめる。 */
class SeedPaletteTest {
    @Test
    fun everySwatchMeetsTextContrastInLightAndDark() {
        val failures = SeedSwatch.entries.flatMap { contrastFailures(it.name, it.choice) }
        assertTrue("コントラスト比 4.5:1 未満の組み合わせ: $failures", failures.isEmpty())
    }

    @Test
    fun extremeSeedsAlsoMeetTextContrast() {
        // 見本に無い色（バックアップから読んだ色など）でも、配色の作り方で 4.5:1 を保てる
        val seeds =
            listOf(0xFF000000, 0xFFFFFFFF, 0xFF808080, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF)
        val failures = seeds.flatMap { contrastFailures("%08X".format(it), ThemeChoice.seed(it)) }
        assertTrue("コントラスト比 4.5:1 未満の組み合わせ: $failures", failures.isEmpty())
    }

    @Test
    fun swatchCountAndColorsAreDistinct() {
        assertTrue(SeedSwatch.entries.size in 8..12)
        val primaries = SeedSwatch.entries.map { it.choice.palette(darkTheme = false).primary }
        assertEquals("見本ごとに主の色が違う", primaries.size, primaries.toSet().size)
    }

    @Test
    fun paletteIsOpaqueAndLightDiffersFromDark() {
        SeedSwatch.entries.forEach { swatch ->
            val light = swatch.choice.palette(darkTheme = false)
            val dark = swatch.choice.palette(darkTheme = true)
            assertNotEquals(light, dark)
            (light.textPairs().values + dark.textPairs().values).forEach { (fg, bg) ->
                assertEquals(OPAQUE, fg and OPAQUE)
                assertEquals(OPAQUE, bg and OPAQUE)
            }
            assertTrue("明るい背景は明るい", ContrastRatio.relativeLuminance(light.background) > HALF)
            assertTrue("暗い背景は暗い", ContrastRatio.relativeLuminance(dark.background) < DARK_LIMIT)
        }
    }

    @Test
    fun seedKeysRoundTrip() {
        SeedSwatch.entries.forEach { swatch ->
            assertEquals(swatch.choice, ThemeChoice.fromStorageKey(swatch.choice.storageKey))
            assertEquals(swatch, SeedSwatch.of(swatch.choice))
        }
        assertEquals("seed:#E53950", SeedSwatch.STRAWBERRY.choice.storageKey)
        assertEquals(ThemeChoice.seed(0xFFE53950), ThemeChoice.fromStorageKey("seed:#e53950"))
        // 見本に無い色も、形が正しければそのまま読む
        assertEquals(ThemeChoice.seed(0xFF123456), ThemeChoice.fromStorageKey("seed:#123456"))
    }

    @Test
    fun malformedSeedKeysFallBackToDefault() {
        listOf("seed:#", "seed:#12345", "seed:#1234567", "seed:#GG0000", "seed:#+12345", "seed:123456").forEach {
            assertEquals(it, ThemeChoice.DEFAULT, ThemeChoice.fromStorageKey(it))
        }
    }

    @Test
    fun seedKeysDoNotCollideWithOtherChoices() {
        val choices = ThemePreset.entries + ThemeChoice.Wallpaper + SeedSwatch.entries.map { it.choice }
        val keys = choices.map { it.storageKey }
        assertEquals(keys.size, keys.toSet().size)
    }

    private fun contrastFailures(
        name: String,
        choice: ThemeChoice.Seed,
    ): List<String> =
        listOf(false, true).flatMap { dark ->
            choice.palette(dark).textPairs().mapNotNull { (pairName, pair) ->
                val ratio = ContrastRatio.of(pair.first, pair.second)
                val mode = if (dark) "dark" else "light"
                if (ratio < ContrastRatio.MIN_TEXT_RATIO) "$name/$mode $pairName = %.2f".format(ratio) else null
            }
        }

    private companion object {
        const val OPAQUE = 0xFF000000L
        const val HALF = 0.5
        const val DARK_LIMIT = 0.05
    }
}
