package com.johnie.pixelbead.engine.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the pattern grid model (counts, edits, replacement).
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
class PatternProjectTest {

    @Test
    void countsBeadsPerColor() throws IOException {
        BeadPalette palette = BeadPalette.loadResource("/palettes/mard_standard.json");
        // 29x29
        BeadBoard board = BeadBoard.MINI_SMALL;

        int[][] grid = new int[29][29];
        for (int[] row : grid) {
            Arrays.fill(row, -1);
        }
        grid[0][0] = 0;
        grid[0][1] = 0;
        grid[1][0] = 1;
        grid[28][28] = 5;

        PatternProject project = new PatternProject(board, palette, grid);
        int[] counts = project.colorCounts();

        assertEquals(291, counts.length);
        assertEquals(2, counts[0]);
        assertEquals(1, counts[1]);
        assertEquals(1, counts[5]);
        assertEquals(0, counts[2]);
    }

    @Test
    void emptyGridCountsAllZero() throws IOException {
        BeadPalette palette = BeadPalette.loadResource("/palettes/mard_standard.json");
        int[][] grid = new int[29][29];
        for (int[] row : grid) {
            Arrays.fill(row, -1);
        }
        PatternProject project = new PatternProject(BeadBoard.MINI_SMALL, palette, grid);
        int[] counts = project.colorCounts();
        for (int count : counts) {
            assertEquals(0, count);
        }
    }

    @Test
    void rejectsGridDimensionMismatch() throws IOException {
        BeadPalette palette = BeadPalette.loadResource("/palettes/mard_standard.json");
        assertThrows(IllegalArgumentException.class,
                () -> new PatternProject(BeadBoard.MINI_SMALL, palette, new int[5][5]));
    }

    @Test
    void replaceColorSwapsAllMatchingCells() throws IOException {
        BeadPalette palette = BeadPalette.loadResource("/palettes/mard_standard.json");
        int[][] grid = new int[29][29];
        for (int[] row : grid) {
            Arrays.fill(row, -1);
        }
        grid[0][0] = 5;
        grid[0][1] = 5;
        grid[1][0] = 12;
        grid[1][1] = 5;
        PatternProject project = new PatternProject(BeadBoard.MINI_SMALL, palette, grid);

        int replaced = project.replaceColor(5, 12);

        assertEquals(3, replaced);
        assertEquals(12, project.cell(0, 0));
        assertEquals(12, project.cell(1, 1));
        assertEquals(-1, project.cell(28, 28));
    }

    @Test
    void replaceColorSameIndexDoesNothing() throws IOException {
        BeadPalette palette = BeadPalette.loadResource("/palettes/mard_standard.json");
        int[][] grid = new int[29][29];
        for (int[] row : grid) {
            Arrays.fill(row, -1);
        }
        grid[0][0] = 5;
        PatternProject project = new PatternProject(BeadBoard.MINI_SMALL, palette, grid);

        assertEquals(0, project.replaceColor(5, 5));
        assertEquals(5, project.cell(0, 0));
    }
}
