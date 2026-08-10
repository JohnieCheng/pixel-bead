package com.johnie.pixelbead.ui.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditHistoryTest {

    private static int[][] grid(int... values) {
        int[][] g = new int[1][values.length];
        for (int x = 0; x < values.length; x++) {
            g[0][x] = values[x];
        }
        return g;
    }

    @Test
    void undoRestoresPreEditState() {
        EditHistory history = new EditHistory();
        int[][] grid = grid(0, 1, 2);
        history.push(grid);
        grid[0][1] = 9;
        assertTrue(history.undo(grid));
        assertEquals(1, grid[0][1]);
        assertFalse(history.canUndo());
    }

    @Test
    void redoReappliesEdit() {
        EditHistory history = new EditHistory();
        int[][] grid = grid(0, 1, 2);
        history.push(grid);
        grid[0][1] = 9;
        history.undo(grid);
        assertTrue(history.redo(grid));
        assertEquals(9, grid[0][1]);
    }

    @Test
    void newEditClearsRedo() {
        EditHistory history = new EditHistory();
        int[][] grid = grid(0, 1);
        history.push(grid);
        grid[0][0] = 5;
        history.undo(grid);
        history.push(grid);
        assertFalse(history.canRedo());
    }

    @Test
    void emptyHistoryDoesNothing() {
        EditHistory history = new EditHistory();
        int[][] grid = grid(0, 1);
        assertFalse(history.undo(grid));
        assertFalse(history.redo(grid));
    }

    @Test
    void limitKeepsNewestFifty() {
        EditHistory history = new EditHistory();
        int[][] grid = grid(0);
        for (int i = 0; i < 60; i++) {
            history.push(grid);
        }
        int undos = 0;
        while (history.undo(grid)) {
            undos++;
        }
        assertEquals(50, undos);
    }

    @Test
    void clearDropsEverything() {
        EditHistory history = new EditHistory();
        int[][] grid = grid(0);
        history.push(grid);
        history.clear();
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
    }
}
