package com.johnie.pixelbead.engine.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeadPaletteLoadTest {

    @Test
    void loadsMardPalette() throws IOException {
        BeadPalette palette = BeadPalette.loadResource("/palettes/mard_standard.json");
        assertEquals("Mard", palette.brand());
        assertEquals(291, palette.size());

        BeadColor first = palette.colorAt(0);
        assertEquals("A1", first.code());
        assertEquals(250, first.r());
        assertEquals(244, first.g());
        assertEquals(200, first.b());
        assertEquals(3, first.lab().length);

        assertEquals("ZG8", palette.colorAt(290).code());
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
