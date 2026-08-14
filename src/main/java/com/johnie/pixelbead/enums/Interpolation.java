package com.johnie.pixelbead.enums;

import com.johnie.pixelbead.util.I18n;

/**
 * Resampling mode when scaling the source image.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/13
 */
public enum Interpolation implements I18n.Key {
    /**
     * Keep hard pixel edges (pixel art sources).
     */
    NEAREST,
    /**
     * Smooth interpolation (photographic sources).
     */
    BILINEAR;

    /**
     * Full key prefix: enum.interpolation.
     */
    private static final String KEY_PREFIX = "enum.interpolation.";

    /**
     * i18n key, e.g. {@code enum.interpolation.bilinear}.
     */
    @Override
    public String getI18nKey() {
        return KEY_PREFIX + name().toLowerCase();
    }
}
