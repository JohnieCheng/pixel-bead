package com.johnie.pixelbead.engine;
import com.johnie.pixelbead.enums.Dithering;
import com.johnie.pixelbead.enums.Quantization;
import com.johnie.pixelbead.enums.Interpolation;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.quantizer.ColorDifference;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the image-to-pattern conversion pipeline.
 *
 * @author johnie
 * @version 3.0.0
 * @since 2026/08/10
 */
class BeadEngineTest {

    private static BeadPalette palette;
    // 29x29
    private static final BeadBoard BOARD = BeadBoard.MINI_SMALL;

    @BeforeAll
    static void setUp() throws IOException {
        palette = BeadPalette.loadResource("/palettes/mard_standard.json");
    }

    private static BufferedImage solidImage(int w, int h, int r, int g, int b) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int argb = (0xFF << 24) | (r << 16) | (g << 8) | b;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, argb);
            }
        }
        return img;
    }

    @Test
    void solidColorImageMapsEntireBoardToOneColor() {
        BufferedImage src = solidImage(64, 64, 200, 40, 40);
        PatternProject project = BeadEngine.processImage(src, BOARD, palette, Interpolation.BILINEAR);

        int expected = palette.nearestIndex(200, 40, 40);
        int[][] grid = project.grid();
        for (int[] row : grid) {
            for (int cell : row) {
                assertEquals(expected, cell);
            }
        }
        assertEquals(29 * 29, project.colorCounts()[expected]);
    }

    @Test
    void halfRedHalfBlueImageMapsEachSide() {
        BufferedImage src = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                int argb = x < 32
                        ? (0xFF << 24) | (200 << 16)
                        : (0xFF << 24) | (40 << 16) | (40 << 8) | 200;
                src.setRGB(x, y, argb);
            }
        }
        PatternProject project = BeadEngine.processImage(src, BOARD, palette, Interpolation.NEAREST);
        int[][] grid = project.grid();

        int redIdx = palette.nearestIndex(200, 0, 0);
        int blueIdx = palette.nearestIndex(40, 40, 200);

        // Left edge is red, right edge is blue.
        assertEquals(redIdx, grid[14][0]);
        assertEquals(blueIdx, grid[14][28]);
    }

    @Test
    void transparentPixelsBecomeEmptyCells() {
        // Fully semi-transparent image: every cell must be empty.
        BufferedImage src = new BufferedImage(29, 29, BufferedImage.TYPE_INT_ARGB);
        int argb = (100 << 24) | (200 << 16);
        for (int y = 0; y < 29; y++) {
            for (int x = 0; x < 29; x++) {
                src.setRGB(x, y, argb);
            }
        }
        PatternProject project = BeadEngine.processImage(src, BOARD, palette, Interpolation.NEAREST);
        int[][] grid = project.grid();
        for (int[] row : grid) {
            for (int cell : row) {
                assertEquals(-1, cell);
            }
        }
        int[] counts = project.colorCounts();
        for (int count : counts) {
            assertEquals(0, count);
        }
    }

    @Test
    void wideImageIsCenteredWithEmptyMargins() {
        // 3:1 strip on a 29x29 board: fit scale leaves empty top/bottom margins.
        BufferedImage src = solidImage(300, 100, 30, 200, 30);
        PatternProject project = BeadEngine.processImage(src, BOARD, palette, Interpolation.BILINEAR);
        int[][] grid = project.grid();

        // Corners are outside the scaled image -> empty.
        assertEquals(-1, grid[0][0]);
        assertEquals(-1, grid[28][28]);

        int expected = palette.nearestIndex(30, 200, 30);
        // Vertical center band (rows 9..18) is fully filled.
        for (int y = 9; y <= 18; y++) {
            for (int x = 0; x < 29; x++) {
                assertEquals(expected, grid[y][x]);
            }
        }
        // Margin rows are empty across the full width.
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 29; x++) {
                assertEquals(-1, grid[y][x]);
            }
        }
        for (int y = 19; y < 29; y++) {
            for (int x = 0; x < 29; x++) {
                assertEquals(-1, grid[y][x]);
            }
        }
    }

    @Test
    void tallImageLeavesHorizontalMargins() {
        // 1:3 strip: fit scale leaves empty left/right margins.
        BufferedImage src = solidImage(100, 300, 240, 200, 30);
        PatternProject project = BeadEngine.processImage(src, BOARD, palette, Interpolation.BILINEAR);
        int[][] grid = project.grid();

        assertEquals(-1, grid[0][0]);
        assertEquals(-1, grid[28][28]);

        int expected = palette.nearestIndex(240, 200, 30);
        // Horizontal center band (columns 9..18) is fully filled.
        for (int x = 9; x <= 18; x++) {
            for (int y = 0; y < 29; y++) {
                assertEquals(expected, grid[y][x]);
            }
        }
        // Margin columns are empty across the full height.
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 29; y++) {
                assertEquals(-1, grid[y][x]);
            }
        }
        for (int x = 19; x < 29; x++) {
            for (int y = 0; y < 29; y++) {
                assertEquals(-1, grid[y][x]);
            }
        }
    }

    @Test
    void pixelArtUsesNearestInterpolation() {
        // 2x2 black/white checker: each source pixel maps to one board cell region.
        BufferedImage src = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        src.setRGB(0, 0, 0xFF000000);
        src.setRGB(1, 0, 0xFFFFFFFF);
        src.setRGB(0, 1, 0xFFFFFFFF);
        src.setRGB(1, 1, 0xFF000000);

        PatternProject project = BeadEngine.processImage(src, BOARD, palette, Interpolation.NEAREST);
        int[][] grid = project.grid();

        int blackIdx = palette.nearestIndex(0, 0, 0);
        int whiteIdx = palette.nearestIndex(255, 255, 255);
        assertNotEquals(blackIdx, whiteIdx);

        // Top-left quadrant should be black, top-right white (approximately).
        assertEquals(blackIdx, grid[0][0]);
        assertEquals(whiteIdx, grid[0][28]);
        assertTrue(grid[28][28] == blackIdx || grid[28][28] == whiteIdx);
    }

    @Test
    void plainOptionsMatchLegacyConversion() {
        BufferedImage img = gradientImage();
        PatternProject legacy = BeadEngine.processImage(img, BOARD, palette, Interpolation.BILINEAR);
        PatternProject options = BeadEngine.processImage(img, BOARD, palette,
                new BeadEngine.ConversionOptions(Interpolation.BILINEAR,
                        Quantization.NEAREST, Dithering.NONE, 1.0, 0, 0.0, 1));
        assertArrayEquals(legacy.grid(), options.grid());
    }

    @Test
    void zeroStrengthEqualsNoDithering() {
        BufferedImage img = gradientImage();
        PatternProject none = BeadEngine.processImage(img, BOARD, palette,
                new BeadEngine.ConversionOptions(Interpolation.BILINEAR,
                        Quantization.NEAREST, Dithering.FLOYD_STEINBERG, 0.0, 0, 0.0, 1));
        PatternProject plain = BeadEngine.processImage(img, BOARD, palette,
                new BeadEngine.ConversionOptions(Interpolation.BILINEAR,
                        Quantization.NEAREST, Dithering.NONE, 1.0, 0, 0.0, 1));
        assertArrayEquals(plain.grid(), none.grid());
    }

    @Test
    void ditheringChangesOutput() {
        BufferedImage img = gradientImage();
        PatternProject plain = BeadEngine.processImage(img, BOARD, palette,
                new BeadEngine.ConversionOptions(Interpolation.BILINEAR,
                        Quantization.NEAREST, Dithering.NONE, 1.0, 0, 0.0, 1));
        PatternProject dithered = BeadEngine.processImage(img, BOARD, palette,
                new BeadEngine.ConversionOptions(Interpolation.BILINEAR,
                        Quantization.NEAREST, Dithering.FLOYD_STEINBERG, 1.0, 0, 0.0, 1));
        assertFalse(Arrays.equals(plain.grid(), dithered.grid()),
                "dithering should alter the quantized grid");
    }

    @Test
    void orphanCleanRemovesIsolatedCell() {
        BeadBoard small = new BeadBoard(3, 3, 2.6, 10);
        int r1 = palette.colorAt(1).r(), g1 = palette.colorAt(1).g(), b1 = palette.colorAt(1).b();
        int r2 = palette.colorAt(2).r(), g2 = palette.colorAt(2).g(), b2 = palette.colorAt(2).b();
        BufferedImage img = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                boolean centre = x == 1 && y == 1;
                int argb = centre ? (0xFF000000 | (r2 << 16) | (g2 << 8) | b2)
                        : (0xFF000000 | (r1 << 16) | (g1 << 8) | b1);
                img.setRGB(x, y, argb);
            }
        }
        PatternProject cleaned = BeadEngine.processImage(img, small, palette,
                new BeadEngine.ConversionOptions(Interpolation.NEAREST,
                        Quantization.NEAREST, Dithering.NONE, 1.0, 1, 0.0, 1));
        int[][] result = cleaned.grid();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                assertEquals(1, result[y][x], "isolated cell should merge into colour 1");
            }
        }
    }

    @Test
    void mergeCollapsesLowFrequencyColour() {
        // Find the closest palette pair to guarantee a sub-threshold merge.
        double bestDe = Double.MAX_VALUE;
        int a = 0;
        int b = 1;
        for (int i = 0; i < palette.size(); i++) {
            for (int j = i + 1; j < palette.size(); j++) {
                double de = ColorDifference.de2000(palette.colorAt(i).lab(), palette.colorAt(j).lab());
                if (de < bestDe) {
                    bestDe = de;
                    a = i;
                    b = j;
                }
            }
        }
        assertTrue(bestDe < 4.0, "palette should contain a pair within ΔE 4");
        // 5x5 image: colour a everywhere except a single cell of colour b.
        BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        int argbA = colorArgb(a);
        int argbB = colorArgb(b);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                img.setRGB(x, y, (x == 2 && y == 2) ? argbB : argbA);
            }
        }
        PatternProject merged = BeadEngine.processImage(img, new BeadBoard(5, 5, 2.6, 10), palette,
                new BeadEngine.ConversionOptions(Interpolation.NEAREST,
                        Quantization.NEAREST, Dithering.NONE, 1.0, 0, 4.0, 10));
        for (int[] row : merged.grid()) {
            for (int cell : row) {
                assertNotEquals(b, cell, "low-frequency colour should be merged away");
            }
        }
    }

    @Test
    void mergeProtectsHighFrequencyColour() {
        // Find a close pair again (a = majority, b = minority).
        double bestDe = Double.MAX_VALUE;
        int a = 0;
        int b = 1;
        for (int i = 0; i < palette.size(); i++) {
            for (int j = i + 1; j < palette.size(); j++) {
                double de = ColorDifference.de2000(palette.colorAt(i).lab(), palette.colorAt(j).lab());
                if (de < bestDe) {
                    bestDe = de;
                    a = i;
                    b = j;
                }
            }
        }
        // 10x10 image: 60 cells of colour a (>= minBeads), 1 cell of b.
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                img.setRGB(x, y, (x + y < 1) ? colorArgb(b) : colorArgb(a));
            }
        }
        PatternProject merged = BeadEngine.processImage(img, new BeadBoard(10, 10, 2.6, 10), palette,
                new BeadEngine.ConversionOptions(Interpolation.NEAREST,
                        Quantization.NEAREST, Dithering.NONE, 1.0, 0, 4.0, 10));
        int countA = 0;
        for (int[] row : merged.grid()) {
            for (int cell : row) {
                if (cell == a) {
                    countA++;
                }
            }
        }
        assertEquals(100, countA, "majority colour must survive and absorb the minority");
    }

    @Test
    void mergeReducesColourCountAfterDithering() {
        BufferedImage img = gradientImage();
        PatternProject dithered = BeadEngine.processImage(img, BOARD, palette,
                new BeadEngine.ConversionOptions(Interpolation.BILINEAR,
                        Quantization.NEAREST, Dithering.FLOYD_STEINBERG, 1.0, 0, 0.0, 1));
        PatternProject merged = BeadEngine.processImage(img, BOARD, palette,
                new BeadEngine.ConversionOptions(Interpolation.BILINEAR,
                        Quantization.NEAREST, Dithering.FLOYD_STEINBERG, 1.0, 0, 7.0, 1));
        int coloursD = distinctColours(dithered.grid());
        int coloursM = distinctColours(merged.grid());
        assertTrue(coloursM <= coloursD,
                "merging should not increase the colour count (" + coloursM + " > " + coloursD + ")");
    }

    @Test
    void orphanToleranceControlsCleaning() {
        // Colour 2 occupies two adjacent cells; each has exactly one matching
        // neighbour. Light (tolerance 0) keeps them, Strong (tolerance 2) merges.
        int r1 = palette.colorAt(1).r(), g1 = palette.colorAt(1).g(), b1 = palette.colorAt(1).b();
        int r2 = palette.colorAt(2).r(), g2 = palette.colorAt(2).g(), b2 = palette.colorAt(2).b();
        BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                boolean pair = (x == 1 || x == 2) && y == 2;
                int argb = pair ? (0xFF000000 | (r2 << 16) | (g2 << 8) | b2)
                        : (0xFF000000 | (r1 << 16) | (g1 << 8) | b1);
                img.setRGB(x, y, argb);
            }
        }
        PatternProject light = BeadEngine.processImage(img, new BeadBoard(5, 5, 2.6, 10), palette,
                new BeadEngine.ConversionOptions(Interpolation.NEAREST,
                        Quantization.NEAREST, Dithering.NONE, 1.0, 1, 0.0, 1));
        PatternProject strong = BeadEngine.processImage(img, new BeadBoard(5, 5, 2.6, 10), palette,
                new BeadEngine.ConversionOptions(Interpolation.NEAREST,
                        Quantization.NEAREST, Dithering.NONE, 1.0, 3, 0.0, 1));
        assertEquals(2, light.grid()[2][1], "light cleaning must keep the pair");
        assertEquals(1, strong.grid()[2][1], "strong cleaning must merge the pair");
    }

    @Test
    void averageModeSmoothsNoise() {
        // Noisy gradient: region average quantizes to fewer distinct colours.
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(7);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int base = 120 + 20 * (x / 16);
                int r = Math.max(0, Math.min(255, base + random.nextInt(40) - 20));
                img.setRGB(x, y, 0xFF000000 | (r << 16) | ((255 - r) << 8) | 100);
            }
        }
        PatternProject nearest = BeadEngine.processImage(img, new BeadBoard(16, 16, 2.6, 10), palette,
                new BeadEngine.ConversionOptions(Interpolation.BILINEAR,
                        Quantization.NEAREST, Dithering.NONE, 1.0, 0, 0.0, 1));
        PatternProject average = BeadEngine.processImage(img, new BeadBoard(16, 16, 2.6, 10), palette,
                new BeadEngine.ConversionOptions(Interpolation.BILINEAR,
                        Quantization.AVERAGE, Dithering.NONE, 1.0, 0, 0.0, 1));
        assertTrue(distinctColours(average.grid()) <= distinctColours(nearest.grid()),
                "average mode should not increase the colour count");
    }

    @Test
    void averageModeFlatBlocksEqualNearest() {
        // Flat colour blocks: region average reproduces the same palette hits.
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                int idx = (x / 16) + (y / 16) * 2;
                int r = palette.colorAt(idx).r();
                int g = palette.colorAt(idx).g();
                int b = palette.colorAt(idx).b();
                img.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
        PatternProject nearest = BeadEngine.processImage(img, new BeadBoard(8, 8, 2.6, 10), palette,
                new BeadEngine.ConversionOptions(Interpolation.NEAREST,
                        Quantization.NEAREST, Dithering.NONE, 1.0, 0, 0.0, 1));
        PatternProject average = BeadEngine.processImage(img, new BeadBoard(8, 8, 2.6, 10), palette,
                new BeadEngine.ConversionOptions(Interpolation.NEAREST,
                        Quantization.AVERAGE, Dithering.NONE, 1.0, 0, 0.0, 1));
        assertArrayEquals(nearest.grid(), average.grid(),
                "flat blocks should produce identical grids in both modes");
    }

    private static int distinctColours(int[][] grid) {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int[] row : grid) {
            for (int cell : row) {
                if (cell >= 0) {
                    seen.add(cell);
                }
            }
        }
        return seen.size();
    }

    private static int colorArgb(int index) {
        return 0xFF000000 | (palette.colorAt(index).r() << 16)
                | (palette.colorAt(index).g() << 8) | palette.colorAt(index).b();
    }

    private static BufferedImage gradientImage() {
        BufferedImage img = new BufferedImage(64, 48, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 48; y++) {
            for (int x = 0; x < 64; x++) {
                int r = (int) (255.0 * x / 64);
                int g = (int) (255.0 * y / 48);
                int b = (r + g) / 2;
                img.setRGB(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }
}
