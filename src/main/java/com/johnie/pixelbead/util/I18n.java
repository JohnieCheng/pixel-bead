package com.johnie.pixelbead.util;

import javafx.util.StringConverter;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Central access to the UI text bundle. The bundle follows the default
 * locale (system language) at startup; the app can swap it explicitly when a
 * language setting is added.
 */
public final class I18n {

    /**
     * Implemented by enums that carry a translation key.
     */
    public interface Key {

        /**
         * Translation key, e.g. {@code enum.orphanlevel.medium}.
         */
        String getI18nKey();
    }

    private static ResourceBundle bundle =
            ResourceBundle.getBundle("i18n.messages", Locale.getDefault());

    private I18n() {
    }

    /**
     * Replaces the active bundle (e.g. after a language preference change).
     */
    public static void setBundle(ResourceBundle newBundle) {
        bundle = newBundle;
    }

    public static ResourceBundle bundle() {
        return bundle;
    }

    /**
     * Returns the translated string; falls back to the key when missing.
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Formatted message ({0}, {1} placeholders).
     */
    public static String format(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }

    /**
     * One converter for every enum-backed combo: the item stays the enum
     * value while the displayed text is looked up via {@code getI18nKey()}.
     */
    public static <E extends Enum<E> & Key> StringConverter<E> enumConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(E e) {
                return e == null ? "" : I18n.get(e.getI18nKey());
            }

            @Override
            public E fromString(String s) {
                return null;
            }
        };
    }
}
