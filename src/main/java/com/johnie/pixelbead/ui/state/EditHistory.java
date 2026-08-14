package com.johnie.pixelbead.ui.state;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo/redo history for grid edits, stored as full grid snapshots.
 * <p>
 * One snapshot is pushed per edit gesture (e.g. a single mouse stroke)
 * before the first mutation; undo/redo swaps the grid content with the
 * stored snapshots. Keeps at most {@link #LIMIT} snapshots.
 *
 * @author johnie
 * @version 3.0.0
 * @since 2026/08/10
 */
public final class EditHistory {

    private static final int LIMIT = 50;

    private final Deque<int[][]> undoStack = new ArrayDeque<>();
    private final Deque<int[][]> redoStack = new ArrayDeque<>();

    /**
     * Records the grid state before an edit stroke starts.
     */
    public void push(int[][] grid) {
        undoStack.push(copy(grid));
        if (undoStack.size() > LIMIT) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    /**
     * Restores the most recent pre-edit state; returns false if empty.
     */
    public boolean undo(int[][] grid) {
        if (undoStack.isEmpty()) {
            return false;
        }
        redoStack.push(copy(grid));
        copyInto(undoStack.pop(), grid);
        return true;
    }

    /**
     * Re-applies the most recently undone state; returns false if empty.
     */
    public boolean redo(int[][] grid) {
        if (redoStack.isEmpty()) {
            return false;
        }
        undoStack.push(copy(grid));
        copyInto(redoStack.pop(), grid);
        return true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Drops all history (e.g. when a new pattern is loaded).
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    private static int[][] copy(int[][] grid) {
        int[][] clone = new int[grid.length][];
        for (int y = 0; y < grid.length; y++) {
            clone[y] = grid[y].clone();
        }
        return clone;
    }

    private static void copyInto(int[][] from, int[][] to) {
        for (int y = 0; y < from.length; y++) {
            System.arraycopy(from[y], 0, to[y], 0, from[y].length);
        }
    }
}
