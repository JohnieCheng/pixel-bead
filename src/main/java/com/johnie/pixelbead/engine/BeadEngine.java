package com.johnie.pixelbead.engine;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.quantizer.ColorDifference;
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
 * <p>
 * Optional dithering (Floyd-Steinberg / Atkinson) spreads the quantization
 * error so gradients are approximated with the limited palette, and orphan
 * pixel cleaning merges isolated single beads into their surrounding colour.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
public final class BeadEngine {

    /** Alpha below this value is treated as transparent (empty cell). */
    private static final int ALPHA_OPAQUE_THRESHOLD = 128;

    /** Error diffusion algorithm used when quantizing. */
    public enum Dithering {
        /** Plain nearest-colour mapping (pixel art / flat areas). */
        NONE("None"),
        /** Floyd-Steinberg: error spread to 4 neighbours (7/16, 3/16, 5/16, 1/16). */
        FLOYD_STEINBERG("Floyd-Steinberg"),
        /** Atkinson: gentler error spread to 6 neighbours (1/8 each), less noise. */
        ATKINSON("Atkinson");

        private final String label;

        Dithering(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * How a board cell picks its colour from the source.
     */
    public enum Quantization {
        /** Sample the interpolated source pixel (current behaviour). */
        NEAREST("Nearest"),
        /** Average the whole source region covered by the cell (anti-noise). */
        AVERAGE("Average");

        private final String label;

        Quantization(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** All conversion knobs, grouped. */
    public record ConversionOptions(ImageDownsampler.Interpolation interpolation,
                                    Quantization quantization,
                                    Dithering dithering,
                                    double ditheringStrength,
                                    int orphanTolerance,
                                    double mergeThreshold,
                                    int mergeMinShare) {
        public ConversionOptions {
            if (ditheringStrength < 0 || ditheringStrength > 1) {
                throw new IllegalArgumentException("dithering strength must be in [0,1]");
            }
            if (orphanTolerance < 0 || orphanTolerance > 3) {
                throw new IllegalArgumentException("orphan tolerance must be in [0,3]");
            }
            if (mergeThreshold < 0) {
                throw new IllegalArgumentException("merge threshold must be >= 0");
            }
            if (mergeMinShare < 0 || mergeMinShare > 100) {
                throw new IllegalArgumentException("merge min share must be in [0,100]");
            }
        }

        public static ConversionOptions plain(ImageDownsampler.Interpolation interpolation) {
            return new ConversionOptions(interpolation, Quantization.NEAREST, Dithering.NONE, 1.0, 0, 0.0, 1);
        }
    }

    private BeadEngine() {
    }

    /**
     * Converts an image into a bead pattern (legacy plain conversion).
     *
     * @param src     source image (ARGB)
     * @param board   target board configuration
     * @param palette palette used for color matching
     * @param mode    resampling mode
     * @return pattern project with the color-index grid
     */
    public static PatternProject processImage(BufferedImage src, BeadBoard board, BeadPalette palette,
                                              ImageDownsampler.Interpolation mode) {
        return processImage(src, board, palette, ConversionOptions.plain(mode));
    }

    /**
     * Converts an image into a bead pattern with the given options.
     *
     * @param src     source image (ARGB)
     * @param board   target board configuration
     * @param palette palette used for color matching
     * @param options conversion knobs (interpolation, dithering, cleaning)
     * @return pattern project with the color-index grid
     */
    public static PatternProject processImage(BufferedImage src, BeadBoard board, BeadPalette palette,
                                              ConversionOptions options) {
        int gridW = board.columns();
        int gridH = board.rows();

        // Fit scale preserving aspect ratio, centered on the board.
        double scale = Math.min((double) gridW / src.getWidth(), (double) gridH / src.getHeight());
        int scaledW = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int scaledH = Math.max(1, (int) Math.round(src.getHeight() * scale));
        int offsetX = (gridW - scaledW) / 2;
        int offsetY = (gridH - scaledH) / 2;

        int[][] grid;
        if (options.quantization() == Quantization.AVERAGE) {
            // Region-average mode averages the source area per cell, which
            // smooths noise; dithering is meaningless on averaged cells.
            grid = quantizeAverage(src, gridW, gridH, offsetX, offsetY, scale, palette);
        } else {
            BufferedImage scaled = ImageDownsampler.resize(src, scaledW, scaledH, options.interpolation());
            grid = options.dithering() == Dithering.NONE
                    ? quantizePlain(scaled, gridW, gridH, offsetX, offsetY, palette)
                    : quantizeDithered(scaled, gridW, gridH, offsetX, offsetY, palette, options);
        }
        if (options.mergeThreshold() > 0) {
            grid = mergeSimilarColors(grid, palette, options.mergeThreshold(), options.mergeMinShare());
        }
        if (options.orphanTolerance() > 0) {
            grid = cleanOrphans(grid, options.orphanTolerance() - 1);
        }
        return new PatternProject(board, palette, grid);
    }

    /**
     * Quantizes each cell from the average colour of the source region it
     * covers (scaled-coordinate aligned). Transparent regions stay empty.
     */
    private static int[][] quantizeAverage(BufferedImage src, int gridW, int gridH,
                                           int offsetX, int offsetY, double scale,
                                           BeadPalette palette) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        // Use the same rounded scaled size as the fit-scale step, otherwise
        // edge cells can fall outside the actual scaled image bounds.
        double scaledW = Math.round(srcW * scale);
        double scaledH = Math.round(srcH * scale);
        int[][] grid = new int[gridH][gridW];
        int[] row = new int[srcW];
        for (int y = 0; y < gridH; y++) {
            double sy0 = y - offsetY;
            double sy1 = sy0 + 1;
            if (sy1 <= 0 || sy0 >= scaledH) {
                java.util.Arrays.fill(grid[y], -1);
                continue;
            }
            int py0 = Math.max(0, (int) Math.floor(sy0 / scale));
            int py1 = Math.min(srcH, (int) Math.ceil(sy1 / scale));
            for (int x = 0; x < gridW; x++) {
                double sx0 = x - offsetX;
                double sx1 = sx0 + 1;
                if (sx1 <= 0 || sx0 >= scaledW) {
                    grid[y][x] = -1;
                    continue;
                }
                int px0 = Math.max(0, (int) Math.floor(sx0 / scale));
                int px1 = Math.min(srcW, (int) Math.ceil(sx1 / scale));
                long sumR = 0;
                long sumG = 0;
                long sumB = 0;
                long sumA = 0;
                int n = 0;
                for (int py = py0; py < py1; py++) {
                    src.getRGB(px0, py, px1 - px0, 1, row, 0, srcW);
                    for (int i = 0; i < px1 - px0; i++) {
                        int argb = row[i];
                        sumR += (argb >> 16) & 0xFF;
                        sumG += (argb >> 8) & 0xFF;
                        sumB += argb & 0xFF;
                        sumA += (argb >>> 24) & 0xFF;
                        n++;
                    }
                }
                if (n == 0 || sumA / n < ALPHA_OPAQUE_THRESHOLD) {
                    grid[y][x] = -1;
                    continue;
                }
                grid[y][x] = palette.nearestIndex(
                        (int) (sumR / n), (int) (sumG / n), (int) (sumB / n));
            }
        }
        return grid;
    }

    /**
     * Merges low-frequency colours into their perceptually closest existing
     * colour when the ΔE2000 gap is below the threshold. A colour is only
     * mergeable when its usage is below {@code minShare} percent of the filled
     * cells; larger colours are protected.
     */
    private static int[][] mergeSimilarColors(int[][] grid, BeadPalette palette,
                                              double threshold, int minShare) {
        int gridH = grid.length;
        int gridW = grid[0].length;
        int[] counts = new int[palette.size()];
        int totalCells = 0;
        for (int[] row : grid) {
            for (int cell : row) {
                if (cell >= 0) {
                    counts[cell]++;
                    totalCells++;
                }
            }
        }
        double minCount = totalCells * minShare / 100.0;
        // Palette indices ordered by usage, most used first.
        Integer[] order = new Integer[palette.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Integer.compare(counts[b], counts[a]));

        boolean[] removed = new boolean[palette.size()];
        boolean changed;
        do {
            changed = false;
            for (int i = order.length - 1; i >= 0; i--) {
                int from = order[i];
                if (removed[from] || counts[from] == 0 || counts[from] >= minCount) {
                    continue;
                }
                // Only more frequent colours are candidates (small merges into big).
                int best = -1;
                double bestDe = threshold;
                for (int j = 0; j < i; j++) {
                    int to = order[j];
                    if (removed[to] || counts[to] == 0) {
                        continue;
                    }
                    double de = ColorDifference.de2000(palette.colorAt(from).lab(), palette.colorAt(to).lab());
                    if (de < bestDe) {
                        bestDe = de;
                        best = to;
                    }
                }
                if (best >= 0) {
                    int total = counts[from];
                    for (int y = 0; y < gridH; y++) {
                        for (int x = 0; x < gridW; x++) {
                            if (grid[y][x] == from) {
                                grid[y][x] = best;
                            }
                        }
                    }
                    counts[best] += total;
                    counts[from] = 0;
                    removed[from] = true;
                    changed = true;
                }
            }
        } while (changed);
        return grid;
    }

    /**
     * Plain nearest-colour mapping. Results are cached by exact RGB: images
     * typically hold far fewer unique colours than cells, so the N×291 ΔE2000
     * evaluations collapse to ~uniqueColours×291 for the whole grid.
     */
    private static int[][] quantizePlain(BufferedImage scaled, int gridW, int gridH,
                                         int offsetX, int offsetY, BeadPalette palette) {
        Map<Integer, Integer> nearestCache = new HashMap<>();
        int[][] grid = new int[gridH][gridW];
        int[] rowPixels = new int[scaled.getWidth()];
        for (int y = 0; y < gridH; y++) {
            int sy = y - offsetY;
            if (sy < 0 || sy >= scaled.getHeight()) {
                Arrays.fill(grid[y], -1);
                continue;
            }
            scaled.getRGB(0, sy, scaled.getWidth(), 1, rowPixels, 0, scaled.getWidth());
            for (int x = 0; x < gridW; x++) {
                int sx = x - offsetX;
                if (sx < 0 || sx >= scaled.getWidth()) {
                    grid[y][x] = -1;
                    continue;
                }
                int argb = rowPixels[sx];
                if (((argb >>> 24) & 0xFF) < ALPHA_OPAQUE_THRESHOLD) {
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
        return grid;
    }

    /**
     * Error-diffusion quantizer. Each cell accumulates the quantization error
     * of its up-stream neighbours, so limited palette colours interleave to
     * fake gradients. The error is scaled by the dithering strength (0 = no
     * diffusion) before spreading.
     */
    private static int[][] quantizeDithered(BufferedImage scaled, int gridW, int gridH,
                                            int offsetX, int offsetY, BeadPalette palette,
                                            ConversionOptions options) {
        int[][] grid = new int[gridH][gridW];
        double[][][] error = new double[gridH][gridW][3];
        int[] rowPixels = new int[scaled.getWidth()];
        double strength = options.ditheringStrength();
        boolean floyd = options.dithering() == Dithering.FLOYD_STEINBERG;

        for (int y = 0; y < gridH; y++) {
            int sy = y - offsetY;
            if (sy < 0 || sy >= scaled.getHeight()) {
                Arrays.fill(grid[y], -1);
                continue;
            }
            scaled.getRGB(0, sy, scaled.getWidth(), 1, rowPixels, 0, scaled.getWidth());
            for (int x = 0; x < gridW; x++) {
                int sx = x - offsetX;
                if (sx < 0 || sx >= scaled.getWidth()) {
                    grid[y][x] = -1;
                    continue;
                }
                int argb = rowPixels[sx];
                if (((argb >>> 24) & 0xFF) < ALPHA_OPAQUE_THRESHOLD) {
                    grid[y][x] = -1;
                    continue;
                }
                double r = ((argb >> 16) & 0xFF) + error[y][x][0];
                double g = ((argb >> 8) & 0xFF) + error[y][x][1];
                double b = (argb & 0xFF) + error[y][x][2];
                int idx = palette.nearestIndex((int) Math.round(r), (int) Math.round(g), (int) Math.round(b));
                grid[y][x] = idx;

                if (strength <= 0) {
                    continue;
                }
                var color = palette.colorAt(idx);
                double dr = (r - color.r()) * strength;
                double dg = (g - color.g()) * strength;
                double db = (b - color.b()) * strength;
                if (floyd) {
                    spread(error, gridW, gridH, x, y, dr, dg, db, 1.0 / 16.0,
                            1, 0, 7, -1, 1, 3, 0, 1, 5, 1, 1, 1);
                } else {
                    spread(error, gridW, gridH, x, y, dr, dg, db, 1.0 / 8.0,
                            1, 0, 1, 2, 0, 1, -1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 2, 1);
                }
            }
        }
        return grid;
    }

    /** Adds a weighted share of the error to each listed neighbour (dx, dy, weight triples). */
    private static void spread(double[][][] error, int gridW, int gridH, int x, int y,
                               double dr, double dg, double db, double weight, int... offsets) {
        for (int i = 0; i + 2 < offsets.length; i += 3) {
            int nx = x + offsets[i];
            int ny = y + offsets[i + 1];
            if (nx < 0 || nx >= gridW || ny < 0 || ny >= gridH) {
                continue;
            }
            double w = weight * offsets[i + 2];
            error[ny][nx][0] += dr * w;
            error[ny][nx][1] += dg * w;
            error[ny][nx][2] += db * w;
        }
    }

    /**
     * Replaces every cell surrounded by few matching neighbours with the most
     * frequent neighbouring colour, removing isolated single beads. Higher
     * tolerance treats cells with up to that many matching neighbours as
     * orphaned as well, cleaning small clusters more aggressively.
     */
    private static int[][] cleanOrphans(int[][] grid, int tolerance) {
        int gridH = grid.length;
        int gridW = grid[0].length;
        int[][] cleaned = new int[gridH][];
        for (int y = 0; y < gridH; y++) {
            cleaned[y] = grid[y].clone();
        }
        for (int y = 0; y < gridH; y++) {
            for (int x = 0; x < gridW; x++) {
                int value = grid[y][x];
                if (value < 0) {
                    continue;
                }
                int[] neighbours = new int[8];
                int count = 0;
                int same = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx < 0 || nx >= gridW || ny < 0 || ny >= gridH) {
                            continue;
                        }
                        int nv = grid[ny][nx];
                        if (nv < 0) {
                            continue;
                        }
                        neighbours[count++] = nv;
                        if (nv == value) {
                            same++;
                        }
                    }
                }
                if (count > 0 && same <= tolerance) {
                    cleaned[y][x] = modeOf(neighbours, count);
                }
            }
        }
        return cleaned;
    }

    /** Most frequent value in the first {@code count} entries. */
    private static int modeOf(int[] values, int count) {
        int best = values[0];
        int bestCount = 1;
        for (int i = 0; i < count; i++) {
            int current = values[i];
            int currentCount = 0;
            for (int j = 0; j < count; j++) {
                if (values[j] == current) {
                    currentCount++;
                }
            }
            if (currentCount > bestCount) {
                best = current;
                bestCount = currentCount;
            }
        }
        return best;
    }
}
