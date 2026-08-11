package com.johnie.pixelbead.engine.model;

/**
 * Configuration of a bead pegboard (底板).
 * <p>
 * Encapsulates the physical grid properties of a standard board:
 * bead diameter class, grid dimensions and the sub-grid line interval
 * used by the renderer to draw helper lines.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
public final class BeadBoard {

    /**
     * Mard 2.6mm fine bead, large standard board: 50x50, sub-grid every 10 cells.
     */
    public static final BeadBoard MINI_STANDARD = new BeadBoard(50, 50, 2.6, 10);

    /**
     * Mard 2.6mm fine bead, small board: 29x29, sub-grid every 5 cells.
     */
    public static final BeadBoard MINI_SMALL = new BeadBoard(29, 29, 2.6, 5);

    /**
     * Mard 5.0mm midi bead, large standard board: 29x29, sub-grid every 5 cells.
     */
    public static final BeadBoard MIDI_STANDARD = new BeadBoard(29, 29, 5.0, 5);

    /**
     * Mard 5.0mm midi bead, small board: 14x14, sub-grid every 7 cells.
     */
    public static final BeadBoard MIDI_SMALL = new BeadBoard(14, 14, 5.0, 7);

    private final int columns;
    private final int rows;
    private final double beadSizeMm;
    private final int subGridInterval;

    public BeadBoard(int columns, int rows, double beadSizeMm, int subGridInterval) {
        if (columns <= 0 || rows <= 0) {
            throw new IllegalArgumentException("Grid dimensions must be positive: " + columns + "x" + rows);
        }
        if (beadSizeMm <= 0) {
            throw new IllegalArgumentException("Bead size must be positive: " + beadSizeMm);
        }
        if (subGridInterval <= 0) {
            throw new IllegalArgumentException("Sub-grid interval must be positive: " + subGridInterval);
        }
        this.columns = columns;
        this.rows = rows;
        this.beadSizeMm = beadSizeMm;
        this.subGridInterval = subGridInterval;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    public double beadSizeMm() {
        return beadSizeMm;
    }

    public int subGridInterval() {
        return subGridInterval;
    }

    /**
     * Total columns when several boards are tiled horizontally.
     *
     * @param boardCount number of boards side by side
     * @return total column count
     */
    public int columns(int boardCount) {
        return columns * boardCount;
    }

    /**
     * Total rows when several boards are tiled vertically.
     *
     * @param boardCount number of boards stacked
     * @return total row count
     */
    public int rows(int boardCount) {
        return rows * boardCount;
    }
}
