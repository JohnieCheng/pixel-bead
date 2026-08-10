package com.johnie.pixelbead.engine.model;

/**
 * A bead pattern: the board configuration plus the color-index grid.
 * <p>
 * The grid stores palette indices instead of raw RGB values, keeping the
 * in-memory footprint small and making bead count statistics a single pass.
 * A value of -1 means an empty cell (no bead).
 */
public final class PatternProject {

    private final BeadBoard board;
    private final BeadPalette palette;
    private final int[][] grid;

    /**
     * @param board   board configuration this pattern is drawn on
     * @param palette palette the grid indices refer to
     * @param grid    color index grid, must match the board dimensions
     * @throws IllegalArgumentException if grid dimensions mismatch the board
     */
    public PatternProject(BeadBoard board, BeadPalette palette, int[][] grid) {
        if (grid == null || grid.length != board.rows()) {
            throw new IllegalArgumentException("Grid row count " + (grid == null ? "null" : grid.length)
                    + " does not match board rows " + board.rows());
        }
        for (int[] row : grid) {
            if (row == null || row.length != board.columns()) {
                throw new IllegalArgumentException("Grid column count does not match board columns " + board.columns());
            }
        }
        this.board = board;
        this.palette = palette;
        this.grid = grid;
    }

    public BeadBoard board() {
        return board;
    }

    public BeadPalette palette() {
        return palette;
    }

    public int[][] grid() {
        return grid;
    }

    /** Returns the color index at a cell, or -1 for empty. */
    public int cell(int x, int y) {
        return grid[y][x];
    }

    /**
     * Replaces every cell holding {@code fromIndex} with {@code toIndex}
     * and returns the number of replaced cells.
     */
    public int replaceColor(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return 0;
        }
        int count = 0;
        for (int[] row : grid) {
            for (int i = 0; i < row.length; i++) {
                if (row[i] == fromIndex) {
                    row[i] = toIndex;
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Sets the color index at a cell (-1 clears it). Editing is allowed in
     * place; callers must notify the view (AppState.editCount) to repaint.
     */
    public void setCell(int x, int y, int index) {
        if (x < 0 || y < 0 || x >= grid[0].length || y >= grid.length) {
            throw new IllegalArgumentException("cell out of range: " + x + "," + y);
        }
        if (index < -1 || index >= palette.size()) {
            throw new IllegalArgumentException("color index out of range: " + index);
        }
        grid[y][x] = index;
    }

    /**
     * Bead count per palette index, aligned with the palette order.
     * Empty cells (-1) are not counted.
     *
     * @return array of size {@code palette.size()}
     */
    public int[] colorCounts() {
        int[] counts = new int[palette.size()];
        for (int[] row : grid) {
            for (int idx : row) {
                if (idx >= 0) {
                    counts[idx]++;
                }
            }
        }
        return counts;
    }
}
