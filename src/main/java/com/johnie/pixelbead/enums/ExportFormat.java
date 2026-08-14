package com.johnie.pixelbead.enums;

import com.johnie.pixelbead.util.I18n;

/**
 * Supported export formats.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/13
 */
public enum ExportFormat implements I18n.Key {
    PNG, PDF, TEXT;

    /**
     * Full key prefix: enum.exportformat.
     */
    private static final String KEY_PREFIX = "enum.exportformat.";

    /**
     * i18n key, e.g. {@code enum.exportformat.text}.
     */
    @Override
    public String getI18nKey() {
        return KEY_PREFIX + name().toLowerCase();
    }
}
