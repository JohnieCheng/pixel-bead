package com.johnie.pixelbead.engine.quantizer;

import com.johnie.pixelbead.enums.Interpolation;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Image downsampling via AWT Graphics2D.
 * <p>
 * BILINEAR smooths edges and is suited for photographic input; NEAREST keeps
 * hard pixel edges and is suited for pixel-art input.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
public final class ImageDownsampler {

    private ImageDownsampler() {
    }

    /**
     * Resizes an image to the given dimensions.
     *
     * @param src     source image
     * @param targetW target width, &gt; 0
     * @param targetH target height, &gt; 0
     * @param mode    resampling mode
     * @return new image of the target size (ARGB)
     */
    public static BufferedImage resize(BufferedImage src, int targetW, int targetH, Interpolation mode) {
        if (targetW <= 0 || targetH <= 0) {
            throw new IllegalArgumentException("Target size must be positive: " + targetW + "x" + targetH);
        }
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    mode == Interpolation.NEAREST
                            ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                            : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, targetW, targetH, null);
        } finally {
            g.dispose();
        }
        return out;
    }

}
