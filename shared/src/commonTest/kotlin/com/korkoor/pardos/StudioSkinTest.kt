package com.korkoor.pardos

import com.korkoor.pardos.domain.shop.Hsl
import com.korkoor.pardos.domain.shop.ParticleKind
import com.korkoor.pardos.domain.shop.SkinInventory
import com.korkoor.pardos.domain.shop.SkinSource
import com.korkoor.pardos.domain.shop.StudioBackground
import com.korkoor.pardos.domain.shop.StudioConfig
import com.korkoor.pardos.domain.shop.StudioPresets
import com.korkoor.pardos.domain.shop.StudioSkin
import com.korkoor.pardos.domain.shop.StudioTone
import com.korkoor.pardos.domain.shop.TileFinish
import com.korkoor.pardos.domain.shop.TileSkin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StudioSkinTest {
    private fun lightness(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        return (maxOf(r, g, b) + minOf(r, g, b)) / 2
    }

    @Test fun hslPrimaryColorsAreExact() {
        assertEquals(0xFFFF0000L, Hsl.argb(0f, 1f, 0.5f))
        assertEquals(0xFF00FF00L, Hsl.argb(120f, 1f, 0.5f))
        assertEquals(0xFF0000FFL, Hsl.argb(240f, 1f, 0.5f))
        assertEquals(0xFFFFFFFFL, Hsl.argb(77f, 0.4f, 1f))
        assertEquals(0xFF000000L, Hsl.argb(77f, 0.4f, 0f))
    }

    @Test fun hueInterpolationTakesTheShortWay() {
        assertEquals(355f, Hsl.lerpHue(350f, 0f, 0.5f), 0.01f)
        assertEquals(5f, Hsl.lerpHue(10f, 0f, 0.5f), 0.01f)
        assertEquals(0f, Hsl.lerpHue(0f, 0f, 0.7f), 0.01f)
    }

    @Test fun everyCombinationProducesAFullStyle() {
        for (finish in TileFinish.entries) for (tone in StudioTone.entries) for (bg in StudioBackground.entries) {
            val st = StudioConfig(finish, 40, 300, tone, bg).toStyle()
            assertEquals(12, st.tilePalette!!.size)
            assertTrue(st.changesTheme)
            assertTrue(st.tilePalette!!.all { (it ushr 24) == 0xFFL }, "colores opacos")
            assertTrue(st.ink != null && st.accent != null && st.surface != null)
        }
    }

    @Test fun tilesGetDarkerAsValuesGrow() {
        StudioTone.entries.forEach { tone ->
            val p = StudioConfig(tone = tone).toStyle().tilePalette!!
            assertTrue(lightness(p.first()) > lightness(p.last()), "tono $tone")
            assertTrue(p.zipWithNext().all { (a, b) -> lightness(b) <= lightness(a) + 0.02 }, "monótono en $tone")
        }
    }

    @Test fun neonAlwaysGetsADarkBackground() {
        val st = StudioConfig(finish = TileFinish.NEON, background = StudioBackground.LIGHT).toStyle()
        assertTrue(lightness(st.bgTop!!) < 0.3)
        assertEquals(StudioBackground.DARK, StudioConfig(finish = TileFinish.NEON, background = StudioBackground.LIGHT).effectiveBackground)
    }

    @Test fun glassOnDarkUsesLightText() {
        assertEquals(0, StudioConfig(finish = TileFinish.GLASS, background = StudioBackground.DARK).toStyle().lightTextFromPower)
        assertEquals(99, StudioConfig(finish = TileFinish.GLASS, background = StudioBackground.LIGHT).toStyle().lightTextFromPower)
    }

    @Test fun darkBackgroundsAreDarkAndLightOnesAreLight() {
        assertTrue(lightness(StudioConfig(background = StudioBackground.DARK).toStyle().bgTop!!) < 0.25)
        assertTrue(lightness(StudioConfig(background = StudioBackground.LIGHT).toStyle().bgTop!!) > 0.9)
    }

    @Test fun configSurvivesEncodingAndGarbageFallsBack() {
        val c = StudioConfig(TileFinish.METAL, 123, 321, StudioTone.DEEP, StudioBackground.DARK, ParticleKind.EMBERS)
        assertEquals(c, StudioConfig.decode(c.encode()))
        assertEquals(StudioConfig(), StudioConfig.decode(null))
        assertEquals(StudioConfig(), StudioConfig.decode("basura"))
        assertEquals(StudioConfig(), StudioConfig.decode("NOPE|1|2|VIVID|DARK|STARS"))
        assertEquals(StudioConfig(hue = 340, hue2 = 10).hue, StudioConfig.decode("JELLY|-20|370|VIVID|TINTED|STARS").hue)
    }

    @Test fun studioSkinReadsTheActiveConfig() {
        val before = StudioSkin.config
        try {
            StudioSkin.config = StudioConfig(hue = 10)
            val a = TileSkin.STUDIO.style.tilePalette!!.first()
            StudioSkin.config = StudioConfig(hue = 200)
            val b = TileSkin.STUDIO.style.tilePalette!!.first()
            assertNotEquals(a, b)
        } finally {
            StudioSkin.config = before
        }
    }

    @Test fun studioCannotBeBoughtWithInGameCurrency() {
        assertEquals(SkinSource.PURCHASE, TileSkin.STUDIO.source)
        assertEquals(SkinInventory.Purchase.NotPurchasable, SkinInventory().buy(TileSkin.STUDIO, 999_999, 999_999))
    }

    @Test fun presetsAreAllValid() {
        assertTrue(StudioPresets.all.size >= 6)
        assertEquals(StudioPresets.all.size, StudioPresets.all.map { it.name }.toSet().size)
        StudioPresets.all.forEach { assertEquals(12, it.config.toStyle().tilePalette!!.size) }
    }
}
