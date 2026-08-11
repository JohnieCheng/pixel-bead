package com.johnie.pixelbead.engine.renderer;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the AWT pattern renderer and legend layout.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
class PatternRendererTest {

    private static BeadPalette palette;
    private static PatternProject project;

    @BeforeAll
    static void setUp() throws IOException {
        palette = BeadPalette.loadResource("/palettes/mard_standard.json");
        BeadBoard board = new BeadBoard(3, 3, 5.0, 1);
        int[][] grid = new int[3][3];
        for (int[] row : grid) {
            Arrays.fill(row, -1);
        }
        grid[1][1] = 5;
        grid[0][2] = 5;
        grid[2][0] = 12;
        project = new PatternProject(board, palette, grid);
    }

    @Test
    void renderProducesExpectedDimensions() {
        BufferedImage img = PatternRenderer.render(project, 24);
        assertEquals(36 + 3 * 24 + 12, img.getWidth());
        // 3 used colors -> one legend line -> 36 + 3*24 + 28 + 18 + 10
        assertEquals(36 + 72 + 56, img.getHeight());
    }

    @Test
    void legendHeaderHasGapBelowIt() {
        // The first legend swatch must start below the "Bead Count" header text.
        BufferedImage img = PatternRenderer.render(project, 24);
        int legendY = 36 + 3 * 24 + 24;
        // One pixel below the old swatch area: plain white background, no swatch
        // colour and no header text (header descends to ~legendY - 4).
        assertEquals(0xFFFFFF, img.getRGB(36 + 2, legendY - 1) & 0xFFFFFF);
    }

    @Test
    void cellCornersMatchPaletteColor() {
        BufferedImage img = PatternRenderer.render(project, 24);
        // Cell (1,1) holds palette index 5. Sample the four corners (the
        // center carries the color-code label, grid lines sit on the edges).
        Color expected = new Color(palette.colorAt(5).r(), palette.colorAt(5).g(), palette.colorAt(5).b());
        int px = 36 + 1 * 24;
        int py = 36 + 1 * 24;
        assertEquals(expected.getRGB(), img.getRGB(px + 3, py + 3));
        assertEquals(expected.getRGB(), img.getRGB(px + 3, py + 20));
        assertEquals(expected.getRGB(), img.getRGB(px + 20, py + 3));
        assertEquals(expected.getRGB(), img.getRGB(px + 20, py + 20));
    }

    @Test
    void emptyCellIsWhite() {
        BufferedImage img = PatternRenderer.render(project, 24);
        // Cell (0,0) is empty.
        int rgb = img.getRGB(48, 48);
        assertEquals(Color.WHITE.getRGB(), rgb);
    }

    @Test
    void legendAreaIsNotEmpty() {
        BufferedImage img = PatternRenderer.render(project, 24);
        // Bottom strip (legend area) must contain non-white pixels.
        boolean hasInk = false;
        for (int y = img.getHeight() - 40; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (img.getRGB(x, y) != Color.WHITE.getRGB()) {
                    hasInk = true;
                    break;
                }
            }
        }
        assertEquals(true, hasInk);
    }
}
