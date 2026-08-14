package com.johnie.pixelbead.util;
import com.johnie.pixelbead.enums.Dithering;
import com.johnie.pixelbead.enums.Quantization;
import com.johnie.pixelbead.enums.Interpolation;

import com.johnie.pixelbead.engine.BeadEngine;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;
import com.johnie.pixelbead.enums.Dithering;
import com.johnie.pixelbead.enums.ExportFormat;
import com.johnie.pixelbead.enums.Interpolation;
import com.johnie.pixelbead.enums.MergePreset;
import com.johnie.pixelbead.enums.OrphanLevel;
import com.johnie.pixelbead.enums.PaletteChoice;
import com.johnie.pixelbead.enums.Quantization;
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
                Dithering.NONE, Dithering.FLOYD_STEINBERG, Dithering.ATKINSON,
                Quantization.NEAREST, Quantization.AVERAGE,
                Interpolation.NEAREST, Interpolation.BILINEAR,
                OrphanLevel.OFF, OrphanLevel.LIGHT, OrphanLevel.MEDIUM, OrphanLevel.STRONG,
                MergePreset.OFF, MergePreset.CONSERVATIVE, MergePreset.STANDARD,
                MergePreset.AGGRESSIVE, MergePreset.EXTREME,
                ExportFormat.PNG, ExportFormat.PDF, ExportFormat.CSV,
                PaletteChoice.STANDARD_221, PaletteChoice.FULL_291,
        };
    }
}
