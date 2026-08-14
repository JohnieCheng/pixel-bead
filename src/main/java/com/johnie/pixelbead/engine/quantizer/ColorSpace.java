package com.johnie.pixelbead.engine.quantizer;

/**
 * Color space conversion utilities.
 * <p>
 * Provides the standard sRGB -> CIELAB (D65) conversion used for
 * perceptually accurate color matching. Pure math, no AWT/JavaFX dependencies.
 *
 * @author johnie
 * @version 3.0.0
 * @since 2026/08/10
 */
public final class ColorSpace {

    /**
     * D65 white point (CIE 1931 2-degree observer).
     */
    private static final double[] D65 = {95.047, 100.0, 108.883};

    private static final double DELTA = 6.0 / 29.0;

    private ColorSpace() {
    }

    /**
     * Converts an sRGB color to CIELAB (D65).
     *
     * @param r red channel 0-255
     * @param g green channel 0-255
     * @param b blue channel 0-255
     * @return {L, a, b} with L in [0, 100]
     */
    public static double[] rgbToLab(int r, int g, int b) {
        double rl = linearize(r / 255.0);
        double gl = linearize(g / 255.0);
        double bl = linearize(b / 255.0);

        // sRGB -> XYZ (D65), scaled to 0-100
        double x = (0.4124564 * rl + 0.3575761 * gl + 0.1804375 * bl) * 100.0;
        double y = (0.2126729 * rl + 0.7151522 * gl + 0.0721750 * bl) * 100.0;
        double z = (0.0193339 * rl + 0.1191920 * gl + 0.9503041 * bl) * 100.0;

        double fx = f(x / D65[0]);
        double fy = f(y / D65[1]);
        double fz = f(z / D65[2]);

        double l = 116.0 * fy - 16.0;
        double a = 500.0 * (fx - fy);
        double bb = 200.0 * (fy - fz);
        return new double[]{l, a, bb};
    }

    /**
     * sRGB transfer function: gamma-encoded channel -> linear light.
     */
    private static double linearize(double c) {
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /**
     * CIELAB helper function.
     */
    private static double f(double t) {
        return t > DELTA * DELTA * DELTA ? Math.cbrt(t) : t / (3 * DELTA * DELTA) + 4.0 / 29.0;
    }
}
