package com.johnie.pixelbead.engine.quantizer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests RGB/Lab color space conversions.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
class ColorSpaceTest {

    @Test
    void whiteIsL100Neutral() {
        double[] lab = ColorSpace.rgbToLab(255, 255, 255);
        assertEquals(100.0, lab[0], 0.01);
        assertEquals(0.0, lab[1], 0.01);
        assertEquals(0.0, lab[2], 0.01);
    }

    @Test
    void blackIsOrigin() {
        double[] lab = ColorSpace.rgbToLab(0, 0, 0);
        assertEquals(0.0, lab[0], 0.01);
        assertEquals(0.0, lab[1], 0.01);
        assertEquals(0.0, lab[2], 0.01);
    }

    @Test
    void redMatchesReferenceValues() {
        double[] lab = ColorSpace.rgbToLab(255, 0, 0);
        assertEquals(53.24, lab[0], 0.1);
        assertEquals(80.09, lab[1], 0.1);
        assertEquals(67.20, lab[2], 0.1);
    }

    @Test
    void grayStaysNeutral() {
        double[] lab = ColorSpace.rgbToLab(128, 128, 128);
        assertEquals(53.6, lab[0], 0.2);
        assertEquals(0.0, lab[1], 0.1);
        assertEquals(0.0, lab[2], 0.1);
    }
}
