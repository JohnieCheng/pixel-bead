package com.johnie.pixelbead.engine.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests palette JSON loading and color data.
 *
 * @author johnie
 * @version 3.0.0
 * @since 2026/08/10
 */
class BeadPaletteLoadTest {

    @Test
    void loadsMardPalette() throws IOException {
        BeadPalette palette = BeadPalette.loadResource("/palettes/mard_standard.json");
        assertEquals("Mard", palette.brand());
        assertEquals(291, palette.size());

        BeadColor a1 = palette.colorAt(0);
        assertEquals("A1", a1.code());
        assertEquals(249, a1.r());
        assertEquals(240, a1.g());
        assertEquals(205, a1.b());
        assertEquals(3, a1.lab().length);
        assertEquals("#F9F0CD", a1.hex());

        assertEquals("ZG8", palette.colorAt(290).code());
    }

    @Test
    void loadsStandard221Palette() throws IOException {
        BeadPalette p221 = BeadPalette.loadResource("/palettes/mard_standard_221.json");
        assertEquals(221, p221.size());
        BeadColor a1 = p221.colorAt(0);
        assertEquals(249, a1.r());
        assertEquals(240, a1.g());
        assertEquals(205, a1.b());
        assertEquals("#F9F0CD", a1.hex());
        BeadColor m15 = p221.colorAt(220);
        assertEquals("M15", m15.code());
    }

    @Test
    void allColorsHaveValidCachedLab() throws IOException {
        BeadPalette palette = BeadPalette.loadResource("/palettes/mard_standard.json");
        for (BeadColor color : palette.colors()) {
            double[] lab = color.lab();
            // L can exceed 100 by float rounding for pure white (e.g. T1 = 255,255,255).
            assertTrue(lab[0] >= 0.0 && lab[0] <= 100.0001, color.code() + " L out of range: " + lab[0]);
            assertFalse(Double.isNaN(lab[1]) || Double.isNaN(lab[2]), color.code() + " NaN in lab");
        }
    }

    @Test
    void missingResourceThrows() {
        assertThrows(IOException.class, () -> BeadPalette.loadResource("/palettes/nonexistent.json"));
    }
}
