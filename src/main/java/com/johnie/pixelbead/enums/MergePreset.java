package com.johnie.pixelbead.enums;

import com.johnie.pixelbead.util.I18n;

/**
 * Colour merge presets: the ΔE2000 similarity threshold for merging.
 *
 * @author johnie
 * @version 3.0.0
 * @since 2026/08/13
 */
public enum MergePreset implements I18n.Key {
    /**
     * Merging disabled.
     */
    OFF(0.0),
    CONSERVATIVE(2.0),
    STANDARD(4.0),
    AGGRESSIVE(7.0),
    EXTREME(12.0);

    private final double threshold;

    MergePreset(double threshold) {
        this.threshold = threshold;
    }

    /**
     * ΔE2000 threshold for this preset.
     */
    public double threshold() {
        return threshold;
    }

    /**
     * Resolves the preset for a stored threshold, defaulting to OFF.
     */
    public static MergePreset fromThreshold(double threshold) {
        for (MergePreset preset : values()) {
            if (preset.threshold == threshold) {
                return preset;
            }
        }
        return OFF;
    }

    /**
     * Full key prefix: enum.mergepreset.
     */
    private static final String KEY_PREFIX = "enum.mergepreset.";

    /**
     * i18n key, e.g. {@code enum.mergepreset.aggressive}.
     */
    @Override
    public String getI18nKey() {
        return KEY_PREFIX + name().toLowerCase();
    }
}
