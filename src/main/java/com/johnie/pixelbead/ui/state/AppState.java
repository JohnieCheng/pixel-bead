package com.johnie.pixelbead.ui.state;

import com.johnie.pixelbead.engine.BeadEngine;
import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Global application state, shared between controllers and views.
 * <p>
 * UI components observe these properties and react to changes; the engine
 * layer never touches this class.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
public final class AppState {

    /**
     * Editing tools.
     */
    public enum ToolType {
        BRUSH, ERASER, EYEDROPPER
    }

    /**
     * Color themes; the UI root carries the matching CSS class.
     */
    public enum Theme {
        DARK, LIGHT
    }

    private static final AppState INSTANCE = new AppState();

    public static AppState get() {
        return INSTANCE;
    }

    private final ObjectProperty<BeadPalette> palette = new SimpleObjectProperty<>();
    private final ObjectProperty<BeadBoard> board = new SimpleObjectProperty<>(BeadBoard.MINI_STANDARD);
    private final ObjectProperty<Integer> boardColumns = new SimpleObjectProperty<>(1);
    private final ObjectProperty<Integer> boardRows = new SimpleObjectProperty<>(1);
    private final ObjectProperty<ImageDownsampler.Interpolation> interpolation =
            new SimpleObjectProperty<>(ImageDownsampler.Interpolation.BILINEAR);
    private final ObjectProperty<PatternProject> currentProject = new SimpleObjectProperty<>();

    /**
     * Currently selected palette index (the brush color).
     */
    private final IntegerProperty selectedColorIndex = new SimpleIntegerProperty(0);
    /**
     * Active editing tool.
     */
    private final ObjectProperty<ToolType> activeTool = new SimpleObjectProperty<>(ToolType.BRUSH);
    /**
     * Bumped on every grid edit; views repaint on change without re-fitting.
     */
    private final IntegerProperty editCount = new SimpleIntegerProperty();
    /**
     * Undo/redo snapshots for the current pattern.
     */
    private final EditHistory editHistory = new EditHistory();
    /**
     * Source colour of a pending replacement; -1 when not picking a target.
     */
    private final IntegerProperty replaceFromIndex = new SimpleIntegerProperty(-1);
    /**
     * Error diffusion algorithm used for the conversion.
     */
    private final ObjectProperty<BeadEngine.Dithering> dithering =
            new SimpleObjectProperty<>(BeadEngine.Dithering.NONE);
    /**
     * Error diffusion share in [0,1]; 0 disables the diffusion effect.
     * Defaults to 0.25: light diffusion keeps the pattern clean.
     */
    private final DoubleProperty ditheringStrength = new SimpleDoubleProperty(0.25);
    /**
     * Merges isolated single beads into their surrounding colour.
     */
    private final BooleanProperty orphanClean = new SimpleBooleanProperty(false);
    /**
     * Active color theme; light is the default.
     */
    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>(Theme.LIGHT);

    private AppState() {
    }

    public ObjectProperty<BeadPalette> paletteProperty() {
        return palette;
    }

    public ObjectProperty<BeadBoard> boardProperty() {
        return board;
    }

    public ObjectProperty<Integer> boardColumnsProperty() {
        return boardColumns;
    }

    public ObjectProperty<Integer> boardRowsProperty() {
        return boardRows;
    }

    public ObjectProperty<ImageDownsampler.Interpolation> interpolationProperty() {
        return interpolation;
    }

    public ObjectProperty<PatternProject> currentProjectProperty() {
        return currentProject;
    }

    public IntegerProperty selectedColorIndexProperty() {
        return selectedColorIndex;
    }

    public ObjectProperty<ToolType> activeToolProperty() {
        return activeTool;
    }

    public IntegerProperty editCountProperty() {
        return editCount;
    }

    public EditHistory editHistory() {
        return editHistory;
    }

    public IntegerProperty replaceFromIndexProperty() {
        return replaceFromIndex;
    }

    public ObjectProperty<BeadEngine.Dithering> ditheringProperty() {
        return dithering;
    }

    public DoubleProperty ditheringStrengthProperty() {
        return ditheringStrength;
    }

    public BooleanProperty orphanCleanProperty() {
        return orphanClean;
    }

    public ObjectProperty<Theme> themeProperty() {
        return theme;
    }

    /**
     * Undoes the last edit step; returns true when something was undone.
     */
    public boolean undo() {
        PatternProject p = currentProject.get();
        if (p == null) {
            return false;
        }
        if (editHistory.undo(p.grid())) {
            editCount.set(editCount.get() + 1);
            return true;
        }
        return false;
    }

    /**
     * Redoes the last undone edit step; returns true when something was redone.
     */
    public boolean redo() {
        PatternProject p = currentProject.get();
        if (p == null) {
            return false;
        }
        if (editHistory.redo(p.grid())) {
            editCount.set(editCount.get() + 1);
            return true;
        }
        return false;
    }

    /**
     * Clears edit history and edit counter (called when a new pattern loads).
     */
    public void resetEditState() {
        editHistory.clear();
        editCount.set(0);
    }
}
