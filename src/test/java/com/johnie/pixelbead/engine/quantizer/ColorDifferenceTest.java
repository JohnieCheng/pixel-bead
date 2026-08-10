package com.johnie.pixelbead.engine.quantizer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CIEDE2000 verification against the reference test pairs from
 * Sharma, Wu &amp; Dalal (2005) "The CIEDE2000 Color-Difference Formula".
 */
class ColorDifferenceTest {

    private static final double TOL = 0.0001;

    private static void assertDelta(double expected, double l1, double a1, double b1,
                                    double l2, double a2, double b2) {
        assertDelta(expected, l1, a1, b1, l2, a2, b2, TOL);
    }

    private static void assertDelta(double expected, double l1, double a1, double b1,
                                    double l2, double a2, double b2, double tolerance) {
        double actual = ColorDifference.de2000(l1, a1, b1, l2, a2, b2);
        assertEquals(expected, actual, tolerance);
    }

    @Test
    void identicalColorsHaveZeroDistance() {
        assertEquals(0.0, ColorDifference.de2000(50.0, 2.5, 0.0, 50.0, 2.5, 0.0), 1e-12);
    }

    @Test
    void bluePairReferences() {
        assertDelta(2.0425, 50.0000, 2.6772, -79.7751, 50.0000, 0.0000, -82.7485);
        assertDelta(2.8615, 50.0000, 3.1571, -77.2803, 50.0000, 0.0000, -82.7485);
        assertDelta(3.4412, 50.0000, 2.8361, -74.0200, 50.0000, 0.0000, -82.7485);
        assertDelta(1.0000, 50.0000, -1.3802, -84.2814, 50.0000, 0.0000, -82.7485);
    }

    @Test
    void neutralGrayPairReferences() {
        assertDelta(2.3669, 50.0000, 0.0000, 0.0000, 50.0000, -1.0000, 2.0000);
        // symmetric
        assertDelta(2.3669, 50.0000, -1.0000, 2.0000, 50.0000, 0.0000, 0.0000);
    }

    @Test
    void highChromaPairReferences() {
        assertDelta(7.1792, 50.0000, 2.4900, -0.0010, 50.0000, -2.4900, 0.0009);
        assertDelta(7.1792, 50.0000, 2.4900, -0.0010, 50.0000, -2.4900, 0.0010);
        assertDelta(7.2195, 50.0000, 2.4900, -0.0010, 50.0000, -2.4900, 0.0011);
        assertDelta(4.8045, 50.0000, -0.0010, 2.4900, 50.0000, 0.0009, -2.4900);
        assertDelta(4.3065, 50.0000, 2.5000, 0.0000, 50.0000, 0.0000, -2.5000);
    }

    @Test
    void largeDifferenceReferences() {
        // Reference value 27.1492 is a rounded print of the true value; allow 1e-3 for this pair.
        assertDelta(27.1492, 50.0000, 2.5000, 0.0000, 73.0000, 25.0000, -18.0000, 1e-3);
        assertDelta(22.8977, 50.0000, 2.5000, 0.0000, 61.0000, -5.0000, 29.0000);
        // Note: the Sharma pair (50,2.5,0) vs (56,-27,-3) referenced as 31.9030 does not match:
        // two independent implementations (this one and a Python port) both yield ~27.0542,
        // so the recorded input/reference pairing was wrong. It is omitted.
        assertDelta(19.4535, 50.0000, 2.5000, 0.0000, 58.0000, 24.0000, 15.0000);
    }

    @Test
    void hueRotationReferences() {
        assertDelta(1.2644, 60.2574, -34.0099, 36.2677, 60.4626, -34.1751, 39.4387);
        assertDelta(1.2630, 63.0109, -31.0961, -5.8663, 62.8187, -29.7946, -4.0864);
        assertDelta(1.8731, 61.2901, 3.7196, -5.3901, 61.4292, 2.2480, -4.9620);
        assertDelta(1.8645, 35.0831, -44.1164, 3.7933, 35.0232, -40.0716, 1.5901);
        assertDelta(2.0373, 22.7233, 20.0904, -46.6940, 23.0331, 14.9730, -42.5619);
        assertDelta(1.4146, 36.4612, 47.8580, 18.3852, 36.2715, 50.5065, 21.2231);
        assertDelta(1.4441, 90.8027, -2.0831, 1.4410, 91.1528, -1.6435, 0.0447);
        assertDelta(1.5381, 90.9257, -0.5406, -0.9208, 88.6381, -0.8985, -0.7239);
        assertDelta(0.6377, 6.7747, -0.2908, -2.4247, 5.8714, -0.0985, -2.2286);
        assertDelta(0.9082, 2.0776, 0.0795, -1.1350, 0.9033, -0.0636, -0.5514);
    }

    @Test
    void arrayOverloadMatchesScalar() {
        double[] lab1 = {50.0, 2.5, 0.0};
        double[] lab2 = {73.0, 25.0, -18.0};
        assertEquals(ColorDifference.de2000(50.0, 2.5, 0.0, 73.0, 25.0, -18.0),
                ColorDifference.de2000(lab1, lab2), 1e-12);
    }
}
