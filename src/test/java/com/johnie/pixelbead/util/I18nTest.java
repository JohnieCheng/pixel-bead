package com.johnie.pixelbead.util;

import com.johnie.pixelbead.engine.BeadEngine;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Every enum translation key must exist in both bundles, so adding an enum
 * constant without a translation fails the build instead of showing raw keys.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/13
 */
class I18nTest {

    private static final Locale[] LOCALES = {Locale.ENGLISH, Locale.CHINESE};

    @Test
    void everyEnumKeyExistsInBothBundles() {
        for (Locale locale : LOCALES) {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", locale);
            for (I18n.Key key : keys()) {
                try {
                    assertNotNull(bundle.getString(key.getI18nKey()),
                            "missing translation for " + key.getI18nKey() + " in " + locale);
                } catch (MissingResourceException e) {
                    fail("missing translation for " + key.getI18nKey() + " in " + locale);
                }
            }
        }
    }

    private static I18n.Key[] keys() {
        return new I18n.Key[]{
                BeadEngine.Dithering.NONE, BeadEngine.Dithering.FLOYD_STEINBERG, BeadEngine.Dithering.ATKINSON,
                BeadEngine.Quantization.NEAREST, BeadEngine.Quantization.AVERAGE,
                ImageDownsampler.Interpolation.NEAREST, ImageDownsampler.Interpolation.BILINEAR,
        };
    }
}
