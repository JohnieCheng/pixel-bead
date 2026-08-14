package com.johnie.pixelbead.enums;

import com.johnie.pixelbead.util.I18n;

/**
 * Error diffusion algorithm used when quantizing.
 *
 * @author johnie
 * @version 3.0.0
 * @since 2026/08/13
 */
public enum Dithering implements I18n.Key {
    /**
     * Plain nearest-colour mapping (pixel art / flat areas).
     */
    NONE,
    /**
     * Floyd-Steinberg: error spread to 4 neighbours (7/16, 3/16, 5/16, 1/16).
     */
    FLOYD_STEINBERG,
    /**
     * Atkinson: gentler error spread to 6 neighbours (1/8 each), less noise.
     */
    ATKINSON;

    /**
     * Full key prefix: enum.dithering.
     */
    private static final String KEY_PREFIX = "enum.dithering.";

    /**
     * i18n key, e.g. {@code enum.dithering.floyd_steinberg}.
     */
    @Override
    public String getI18nKey() {
        return KEY_PREFIX + name().toLowerCase();
    }
}
