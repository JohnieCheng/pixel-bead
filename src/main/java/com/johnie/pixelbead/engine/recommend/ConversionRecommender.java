package com.johnie.pixelbead.engine.recommend;

import com.johnie.pixelbead.engine.BeadEngine;
import com.johnie.pixelbead.engine.quantizer.ColorDifference;
import com.johnie.pixelbead.engine.quantizer.ColorSpace;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Analyses a source image and picks sensible conversion settings, so users
 * get a good pattern without understanding dithering or colour merging.
 * <p>
 * Features are computed on a fixed 64x64 sample: unique colour ratio,
 * smooth-gradient and hard-edge ratios of adjacent ΔE2000 pairs, plus a
 * transparency shortcut for cut-out icons / sprites.
 */
public final class ConversionRecommender {

    /** Recommended conversion settings for a source image. */
    public record RecommendedSettings(ImageDownsampler.Interpolation interpolation,
                                      BeadEngine.Dithering dithering,
                                      double ditheringStrength,
                                      int orphanTolerance,
                                      double mergeThreshold) {

        public static RecommendedSettings pixelArt() {
            return new RecommendedSettings(ImageDownsampler.Interpolation.NEAREST,
                    BeadEngine.Dithering.NONE, 0.0, 1, 7.0);
        }

        public static RecommendedSettings photo() {
            return new RecommendedSettings(ImageDownsampler.Interpolation.BILINEAR,
                    BeadEngine.Dithering.FLOYD_STEINBERG, 0.4, 1, 4.0);
        }

        public static RecommendedSettings cartoon() {
            return new RecommendedSettings(ImageDownsampler.Interpolation.BILINEAR,
                    BeadEngine.Dithering.ATKINSON, 0.25, 2, 4.0);
        }

        public static RecommendedSettings generic() {
            return new RecommendedSettings(ImageDownsampler.Interpolation.BILINEAR,
                    BeadEngine.Dithering.NONE, 0.0, 1, 4.0);
        }
    }

    private ConversionRecommender() {
    }

    /**
     * Analyses the image and returns the recommended settings. The source is
     * sampled down to a fixed size first, so the result is board independent.
     */
    public static RecommendedSettings recommend(BufferedImage source) {
        BufferedImage sample = ImageDownsampler.resize(source,
                RecommendThresholds.SAMPLE_SIZE, RecommendThresholds.SAMPLE_SIZE,
                ImageDownsampler.Interpolation.BILINEAR);

        int w = sample.getWidth();
        int h = sample.getHeight();
        int total = w * h;
        int[] argb = sample.getRGB(0, 0, w, h, null, 0, w);

        Set<Integer> unique = new HashSet<>();
        boolean hasTransparent = false;
        for (int value : argb) {
            int alpha = (value >>> 24) & 0xFF;
            if (alpha < RecommendThresholds.TRANSPARENT_ALPHA) {
                hasTransparent = true;
            }
            unique.add(value & 0xFFFFFF);
        }
        double uniqueRatio = (double) unique.size() / total;

        // Transparent cut-outs are almost always icons/sprites: shortcut out
        // when the colour set is also simple, otherwise fall through.
        if (hasTransparent && uniqueRatio < RecommendThresholds.TRANSPARENT_SIMPLE_UNIQUE_MAX) {
            return RecommendedSettings.pixelArt();
        }

        int pairs = 0;
        int gradientSteps = 0;
        int hardEdges = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                if (isTransparent(argb[idx])) {
                    continue;
                }
                if (x + 1 < w && !isTransparent(argb[idx + 1])) {
                    pairs++;
                    double de = de(argb[idx], argb[idx + 1]);
                    if (de >= RecommendThresholds.GRADIENT_MIN_DE && de <= RecommendThresholds.GRADIENT_MAX_DE) {
                        gradientSteps++;
                    } else if (de >= RecommendThresholds.SHARP_EDGE_MIN_DE) {
                        hardEdges++;
                    }
                }
                if (y + 1 < h && !isTransparent(argb[idx + w])) {
                    pairs++;
                    double de = de(argb[idx], argb[idx + w]);
                    if (de >= RecommendThresholds.GRADIENT_MIN_DE && de <= RecommendThresholds.GRADIENT_MAX_DE) {
                        gradientSteps++;
                    } else if (de >= RecommendThresholds.SHARP_EDGE_MIN_DE) {
                        hardEdges++;
                    }
                }
            }
        }
        double gradientRatio = pairs == 0 ? 0 : (double) gradientSteps / pairs;
        double edgeRatio = pairs == 0 ? 0 : (double) hardEdges / pairs;

        if (uniqueRatio < RecommendThresholds.PIXEL_ART_UNIQUE_MAX) {
            return RecommendedSettings.pixelArt();
        }
        if (gradientRatio >= RecommendThresholds.PHOTO_GRADIENT_MIN) {
            return RecommendedSettings.photo();
        }
        if (edgeRatio >= RecommendThresholds.CARTOON_EDGE_MIN) {
            return RecommendedSettings.cartoon();
        }
        return RecommendedSettings.generic();
    }

    /** ΔE2000 between two ARGB pixels (ignoring alpha). */
    private static double de(int argbA, int argbB) {
        double[] labA = ColorSpace.rgbToLab(
                (argbA >> 16) & 0xFF, (argbA >> 8) & 0xFF, argbA & 0xFF);
        double[] labB = ColorSpace.rgbToLab(
                (argbB >> 16) & 0xFF, (argbB >> 8) & 0xFF, argbB & 0xFF);
        return ColorDifference.de2000(labA, labB);
    }

    private static boolean isTransparent(int argb) {
        return ((argb >>> 24) & 0xFF) < RecommendThresholds.TRANSPARENT_ALPHA;
    }
}
