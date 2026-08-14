package com.johnie.pixelbead.enums;

import com.johnie.pixelbead.util.I18n;

/**
 * How a board cell picks its colour from the source.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/13
 */
public enum Quantization implements I18n.Key {
    /**
     * Sample the interpolated source pixel (current behaviour).
     */
    NEAREST,
    /**
     * Average the whole source region covered by the cell (anti-noise).
     */
    AVERAGE;

    /**
     * Full key prefix: enum.quantization.
     */
    private static final String KEY_PREFIX = "enum.quantization.";

    /**
     * i18n key, e.g. {@code enum.quantization.average}.
     */
    @Override
    public String getI18nKey() {
        return KEY_PREFIX + name().toLowerCase();
    }
}
