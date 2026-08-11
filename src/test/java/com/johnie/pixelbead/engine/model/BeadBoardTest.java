package com.johnie.pixelbead.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests board presets, multi-board sizing and validation.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
class BeadBoardTest {

    @Test
    void miniStandardBoardConfiguration() {
        BeadBoard board = BeadBoard.MINI_STANDARD;
        assertEquals(50, board.columns());
        assertEquals(50, board.rows());
        assertEquals(2.6, board.beadSizeMm(), 1e-9);
        assertEquals(10, board.subGridInterval());
    }

    @Test
    void midiStandardBoardConfiguration() {
        BeadBoard board = BeadBoard.MIDI_STANDARD;
        assertEquals(29, board.columns());
        assertEquals(29, board.rows());
        assertEquals(5.0, board.beadSizeMm(), 1e-9);
        assertEquals(5, board.subGridInterval());
    }

    @Test
    void smallBoardsHaveExpectedDimensions() {
        assertEquals(29, BeadBoard.MINI_SMALL.columns());
        assertEquals(29, BeadBoard.MINI_SMALL.rows());
        assertEquals(14, BeadBoard.MIDI_SMALL.columns());
        assertEquals(14, BeadBoard.MIDI_SMALL.rows());
    }

    @Test
    void boardTilingScalesGrid() {
        assertEquals(100, BeadBoard.MINI_STANDARD.columns(2));
        assertEquals(100, BeadBoard.MINI_STANDARD.rows(2));
        assertEquals(58, BeadBoard.MIDI_STANDARD.columns(2));
    }

    @Test
    void invalidDimensionsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BeadBoard(0, 5, 2.6, 10));
        assertThrows(IllegalArgumentException.class, () -> new BeadBoard(5, 5, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> new BeadBoard(5, 5, 2.6, 0));
    }
}
