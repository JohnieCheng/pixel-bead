package com.johnie.pixelbead.enums;

import com.johnie.pixelbead.util.I18n;

/**
 * Orphan cleaning strength: matching neighbours still counted as orphaned.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/13
 */
public enum OrphanLevel implements I18n.Key {
    /**
     * Cleaning disabled.
     */
    OFF(0),
    /**
     * Only fully isolated cells (no matching neighbour) are cleaned.
     */
    LIGHT(1),
    /**
     * Cells with at most one matching neighbour are cleaned.
     */
    MEDIUM(2),
    /**
     * Cells with at most two matching neighbours are cleaned.
     */
    STRONG(3);

    private final int tolerance;

    OrphanLevel(int tolerance) {
        this.tolerance = tolerance;
    }

    /**
     * Engine tolerance value for this level.
     */
    public int tolerance() {
        return tolerance;
    }

    /**
     * Resolves the level for a stored tolerance value, defaulting to OFF.
     */
    public static OrphanLevel fromTolerance(int tolerance) {
        for (OrphanLevel level : values()) {
            if (level.tolerance == tolerance) {
                return level;
            }
        }
        return OFF;
    }

    /**
     * Full key prefix: enum.orphanlevel.
     */
    private static final String KEY_PREFIX = "enum.orphanlevel.";

    /**
     * i18n key, e.g. {@code enum.orphanlevel.medium}.
     */
    @Override
    public String getI18nKey() {
        return KEY_PREFIX + name().toLowerCase();
    }
}
