package com.johnie.pixelbead.engine.quantizer;

/**
 * CIEDE2000 color difference formula (CIE 2000, ΔE00).
 * <p>
 * Reference: Sharma, Wu &amp; Dalal, "The CIEDE2000 Color-Difference Formula:
 * Implementation Notes, Supplementary Test Data, and Mathematical Observations"
 * (2005). Test values from that paper are used in {@code ColorDifferenceTest}.
 */
public final class ColorDifference {

    private ColorDifference() {
    }

    /**
     * CIEDE2000 distance between two CIELAB colors.
     *
     * @param lab1 first color, {L, a, b}
     * @param lab2 second color, {L, a, b}
     * @return ΔE00, always &gt;= 0
     */
    public static double de2000(double[] lab1, double[] lab2) {
        return de2000(lab1[0], lab1[1], lab1[2], lab2[0], lab2[1], lab2[2]);
    }

    /**
     * CIEDE2000 distance between two CIELAB colors given as individual channels.
     */
    public static double de2000(double l1, double a1, double b1, double l2, double a2, double b2) {
        double c1 = Math.hypot(a1, b1);
        double c2 = Math.hypot(a2, b2);
        double cBar = (c1 + c2) / 2.0;
        double cBar7 = Math.pow(cBar, 7);
        double g = 0.5 * (1.0 - Math.sqrt(cBar7 / (cBar7 + 6103515625.0))); // 25^7 = 6103515625

        double a1p = (1.0 + g) * a1;
        double a2p = (1.0 + g) * a2;
        double c1p = Math.hypot(a1p, b1);
        double c2p = Math.hypot(a2p, b2);

        double h1p = hueDeg(a1p, b1);
        double h2p = hueDeg(a2p, b2);

        double dlp = l2 - l1;
        double dcp = c2p - c1p;

        double dhp;
        if (c1p * c2p == 0.0) {
            dhp = 0.0;
        } else {
            double dh = h2p - h1p;
            if (dh > 180.0) {
                dh -= 360.0;
            } else if (dh < -180.0) {
                dh += 360.0;
            }
            dhp = 2.0 * Math.sqrt(c1p * c2p) * Math.sin(Math.toRadians(dh) / 2.0);
        }

        double lpBar = (l1 + l2) / 2.0;
        double cpBar = (c1p + c2p) / 2.0;

        double hpBar;
        if (c1p * c2p == 0.0) {
            hpBar = h1p + h2p;
        } else {
            double dh = h2p - h1p;
            if (Math.abs(dh) > 180.0) {
                if (h2p <= h1p) {
                    hpBar = (h1p + h2p + 360.0) / 2.0;
                } else {
                    hpBar = (h1p + h2p - 360.0) / 2.0;
                }
            } else {
                hpBar = (h1p + h2p) / 2.0;
            }
        }

        double t = 1.0
                - 0.17 * Math.cos(Math.toRadians(hpBar - 30.0))
                + 0.24 * Math.cos(Math.toRadians(2.0 * hpBar))
                + 0.32 * Math.cos(Math.toRadians(3.0 * hpBar + 6.0))
                - 0.20 * Math.cos(Math.toRadians(4.0 * hpBar - 63.0));

        double dTheta = 30.0 * Math.exp(-Math.pow((hpBar - 275.0) / 25.0, 2.0));
        double cpBar7 = Math.pow(cpBar, 7);
        double rc = 2.0 * Math.sqrt(cpBar7 / (cpBar7 + 6103515625.0));

        double lpBarMinus50 = lpBar - 50.0;
        double sl = 1.0 + 0.015 * lpBarMinus50 * lpBarMinus50 / Math.sqrt(20.0 + lpBarMinus50 * lpBarMinus50);
        double sc = 1.0 + 0.045 * cpBar;
        double sh = 1.0 + 0.015 * cpBar * t;
        double rt = -Math.sin(Math.toRadians(2.0 * dTheta)) * rc;

        double dlpSl = dlp / sl;
        double dcpSc = dcp / sc;
        double dhpSh = dhp / sh;

        return Math.sqrt(dlpSl * dlpSl + dcpSc * dcpSc + dhpSh * dhpSh + rt * dcpSc * dhpSh);
    }

    /** Hue angle in degrees in [0, 360), or 0 when chroma is zero. */
    private static double hueDeg(double a, double b) {
        if (a == 0.0 && b == 0.0) {
            return 0.0;
        }
        double h = Math.toDegrees(Math.atan2(b, a));
        return h < 0.0 ? h + 360.0 : h;
    }
}
