package com.johnie.pixelbead.enums;

import com.johnie.pixelbead.util.I18n;

/**
 * Bundled palette files offered by the palette selector.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/13
 */
public enum PaletteChoice implements I18n.Key {
    /**
     * Mard standard range (A-H + M series, 221 colours).
     */
    STANDARD_221("/palettes/mard_standard_221.json"),
    /**
     * Full Mard catalogue (221 standard + 70 extended, 291 colours).
     */
    FULL_291("/palettes/mard_standard.json");

    private final String resource;

    PaletteChoice(String resource) {
        this.resource = resource;
    }

    /**
     * Classpath palette resource for this choice.
     */
    public String resource() {
        return resource;
    }

    /**
     * Full key prefix: enum.palettechoice.
     */
    private static final String KEY_PREFIX = "enum.palettechoice.";

    /**
     * i18n key, e.g. {@code enum.palettechoice.full_291}.
     */
    @Override
    public String getI18nKey() {
        return KEY_PREFIX + name().toLowerCase();
    }
}
