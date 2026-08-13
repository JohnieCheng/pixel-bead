package com.johnie.pixelbead.engine.recommend;

import com.johnie.pixelbead.engine.BeadEngine;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Synthetic baselines for the recommender: each fixture has a known image
 * type, so threshold changes are guarded by these classifications.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/13
 */
class ConversionRecommenderTest {

    @Test
    void flatColourBlocksRecommendPixelArt() {
        // Four large flat blocks: no gradients, tiny colour set.
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        int[][] blocks = {{230, 60, 60}, {60, 200, 90}, {70, 80, 220}, {240, 210, 60}};
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int[] c = blocks[(y / 64) * 2 + (x / 64)];
                img.setRGB(x, y, 0xFF000000 | (c[0] << 16) | (c[1] << 8) | c[2]);
            }
        }
        ConversionRecommender.RecommendedSettings rec = ConversionRecommender.recommend(img);
        assertEquals(BeadEngine.Dithering.NONE, rec.dithering());
        assertEquals(ImageDownsampler.Interpolation.NEAREST, rec.interpolation());
    }

    @Test
    void smoothGradientRecommendsPhotoDithering() {
        // Smooth gradient with light noise: photo-like.
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(42);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int r = clamp(255 * x / 128 + random.nextInt(3) - 1);
                int g = clamp(255 * y / 128 + random.nextInt(3) - 1);
                int b = clamp((r + g) / 2);
                img.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
        ConversionRecommender.RecommendedSettings rec = ConversionRecommender.recommend(img);
        assertEquals(BeadEngine.Dithering.FLOYD_STEINBERG, rec.dithering());
        assertEquals(ImageDownsampler.Interpolation.BILINEAR, rec.interpolation());
    }

    @Test
    void transparentCutoutShortcutsToPixelArt() {
        // Transparent background with a simple opaque icon: shortcut fires.
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                boolean inside = x >= 32 && x < 96 && y >= 32 && y < 96;
                img.setRGB(x, y, inside ? 0xFF3060C0 : 0x00000000);
            }
        }
        ConversionRecommender.RecommendedSettings rec = ConversionRecommender.recommend(img);
        assertEquals(BeadEngine.Dithering.NONE, rec.dithering());
        assertEquals(ImageDownsampler.Interpolation.NEAREST, rec.interpolation());
    }

    @Test
    void transparentComplexSubjectFallsThrough() {
        // Transparent background but a gradient subject: must NOT shortcut
        // into pixel art; the gradient should classify it as photo.
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                boolean inside = x >= 16 && x < 112 && y >= 16 && y < 112;
                if (!inside) {
                    img.setRGB(x, y, 0x00000000);
                    continue;
                }
                int r = clamp(255 * (x - 16) / 96);
                int g = clamp(255 * (y - 16) / 96);
                int b = clamp((r + g) / 2);
                img.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
        ConversionRecommender.RecommendedSettings rec = ConversionRecommender.recommend(img);
        assertNotEquals(BeadEngine.Dithering.NONE, rec.dithering(),
                "gradient subject must not be classified as flat pixel art");
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
