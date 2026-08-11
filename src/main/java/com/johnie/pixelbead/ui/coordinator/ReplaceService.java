package com.johnie.pixelbead.ui.coordinator;

import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.ui.components.InteractiveCanvas;
import com.johnie.pixelbead.ui.components.Toasts;
import com.johnie.pixelbead.ui.state.AppState;

/**
 * Colour-replacement service shared by the palette and count-table panels:
 * drives the canvas preview and executes replacements as one undo step.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/11
 */
public final class ReplaceService {

    private final AppState state = AppState.get();
    private final InteractiveCanvas canvas;

    public ReplaceService(InteractiveCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Pulse-highlights every cell of the given palette index (-1 clears).
     */
    public void highlight(int paletteIndex) {
        canvas.setHighlight(paletteIndex);
    }

    public void clearHighlight() {
        canvas.clearHighlight();
    }

    /**
     * Shows a live replacement preview: from-index cells drawn as to-index.
     */
    public void preview(int fromIndex, int toIndex) {
        canvas.setReplacePreview(fromIndex, toIndex);
    }

    public void clearPreview() {
        canvas.clearReplacePreview();
    }

    /**
     * Replaces all cells of fromIndex with toIndex as one undo step.
     */
    public void execute(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return;
        }
        PatternProject project = state.currentProjectProperty().get();
        if (project == null) {
            return;
        }
        state.editHistory().push(project.grid());
        int replaced = project.replaceColor(fromIndex, toIndex);
        state.replaceFromIndexProperty().set(-1);
        canvas.clearReplacePreview();
        canvas.clearHighlight();
        state.editCountProperty().set(state.editCountProperty().get() + 1);
        Toasts.show(canvas, "Replaced " + replaced + " cells with " + project.palette().colorAt(toIndex).code());
    }
}
