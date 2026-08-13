package com.johnie.pixelbead.engine.recommend;

/**
 * Magic numbers of the conversion recommender, collected in one place so the
 * decision boundaries can be tuned together after real-image calibration.
 */
public final class RecommendThresholds {

    private RecommendThresholds() {
    }

    // --- Feature computation ranges (ΔE2000) ---
    /** Adjacent pairs within [min, max] count as smooth gradient steps. */
    public static final double GRADIENT_MIN_DE = 1.0;
    public static final double GRADIENT_MAX_DE = 8.0;
    /** Adjacent pairs above this ΔE count as hard edges. */
    public static final double SHARP_EDGE_MIN_DE = 12.0;
    /** Pixels with alpha below this value count as transparent. */
    public static final int TRANSPARENT_ALPHA = 128;
    /** Feature sampling resolution; independent of the target board. */
    public static final int SAMPLE_SIZE = 64;

    // --- Decision thresholds (initial baselines, pending calibration) ---
    /** Unique colour ratio below this classifies as pixel art / icons. */
    public static final double PIXEL_ART_UNIQUE_MAX = 0.02;
    /** Smooth gradient ratio above this classifies as photo-like. */
    public static final double PHOTO_GRADIENT_MIN = 0.08;
    /** Hard edge ratio above this classifies as cartoon / line art. */
    public static final double CARTOON_EDGE_MIN = 0.12;
    /** Transparency shortcut also requires a simple colour set. */
    public static final double TRANSPARENT_SIMPLE_UNIQUE_MAX = 0.10;
}
