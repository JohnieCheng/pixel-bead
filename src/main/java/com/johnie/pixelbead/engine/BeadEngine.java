package com.johnie.pixelbead.engine;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Image to bead pattern conversion pipeline.
 * <p>
 * Steps: fit-scale the source to the board grid (aspect preserved, centered),
 * then map every cell to the perceptually closest palette color.
 * Transparent pixels (alpha below threshold) become empty cells.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
public final class BeadEngine {

    /**
     * Alpha below this value is treated as transparent (empty cell).
     */
    private static final int ALPHA_OPAQUE_THRESHOLD = 128;

    private BeadEngine() {
    }

    /**
     * Converts an image into a bead pattern.
     *
     * @param src     source image (ARGB)
     * @param board   target board configuration
     * @param palette palette used for color matching
     * @param mode    resampling mode
     * @return pattern project with the color-index grid
     */
    public static PatternProject processImage(BufferedImage src, BeadBoard board, BeadPalette palette,
                                              ImageDownsampler.Interpolation mode) {
        int gridW = board.columns();
        int gridH = board.rows();

        // Fit scale preserving aspect ratio, centered on the board.
        double scale = Math.min((double) gridW / src.getWidth(), (double) gridH / src.getHeight());
        int scaledW = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int scaledH = Math.max(1, (int) Math.round(src.getHeight() * scale));
        int offsetX = (gridW - scaledW) / 2;
        int offsetY = (gridH - scaledH) / 2;

        BufferedImage scaled = ImageDownsampler.resize(src, scaledW, scaledH, mode);

        // Nearest-color results are cached by exact RGB: images typically hold
        // far fewer unique colours than cells, so the N×291 ΔE2000 evaluations
        // collapse to ~uniqueColours×291 for the whole grid.
        Map<Integer, Integer> nearestCache = new HashMap<>();

        int[][] grid = new int[gridH][gridW];
        int[] rowPixels = new int[scaledW];
        for (int y = 0; y < gridH; y++) {
            int sy = y - offsetY;
            if (sy < 0 || sy >= scaledH) {
                Arrays.fill(grid[y], -1);
                continue;
            }
            scaled.getRGB(0, sy, scaledW, 1, rowPixels, 0, scaledW);
            for (int x = 0; x < gridW; x++) {
                int sx = x - offsetX;
                if (sx < 0 || sx >= scaledW) {
                    grid[y][x] = -1;
                    continue;
                }
                int argb = rowPixels[sx];
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha < ALPHA_OPAQUE_THRESHOLD) {
                    grid[y][x] = -1;
                } else {
                    int rgb = argb & 0xFFFFFF;
                    Integer nearest = nearestCache.get(rgb);
                    if (nearest == null) {
                        nearest = palette.nearestIndex((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
                        nearestCache.put(rgb, nearest);
                    }
                    grid[y][x] = nearest;
                }
            }
        }
        return new PatternProject(board, palette, grid);
    }
}
