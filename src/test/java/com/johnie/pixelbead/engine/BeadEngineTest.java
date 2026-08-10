package com.johnie.pixelbead.engine;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeadEngineTest {

    private static BeadPalette palette;
    private static final BeadBoard BOARD = BeadBoard.MINI_SMALL; // 29x29

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
        PatternProject project = BeadEngine.processImage(src, BOARD, palette, ImageDownsampler.Interpolation.BILINEAR);

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
        PatternProject project = BeadEngine.processImage(src, BOARD, palette, ImageDownsampler.Interpolation.NEAREST);
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
        PatternProject project = BeadEngine.processImage(src, BOARD, palette, ImageDownsampler.Interpolation.NEAREST);
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
        PatternProject project = BeadEngine.processImage(src, BOARD, palette, ImageDownsampler.Interpolation.BILINEAR);
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
        PatternProject project = BeadEngine.processImage(src, BOARD, palette, ImageDownsampler.Interpolation.BILINEAR);
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

        PatternProject project = BeadEngine.processImage(src, BOARD, palette, ImageDownsampler.Interpolation.NEAREST);
        int[][] grid = project.grid();

        int blackIdx = palette.nearestIndex(0, 0, 0);
        int whiteIdx = palette.nearestIndex(255, 255, 255);
        assertNotEquals(blackIdx, whiteIdx);

        // Top-left quadrant should be black, top-right white (approximately).
        assertEquals(blackIdx, grid[0][0]);
        assertEquals(whiteIdx, grid[0][28]);
        assertTrue(grid[28][28] == blackIdx || grid[28][28] == whiteIdx);
    }
}
