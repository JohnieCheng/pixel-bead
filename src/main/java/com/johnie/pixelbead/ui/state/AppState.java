package com.johnie.pixelbead.ui.state;

import com.johnie.pixelbead.engine.BeadEngine;
import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;
import javafx.beans.property.*;

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

    /** UI language; native names are shown untranslated. */
    public enum Language {
        ZH("中文"), EN("English");

        private final String nativeName;

        Language(String nativeName) {
            this.nativeName = nativeName;
        }

        @Override
        public String toString() {
            return nativeName;
        }
    }

    private static final AppState INSTANCE = new AppState();
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
     * How a board cell picks its colour: nearest sample or region average.
     * Region average is the default: it smooths noise and keeps patterns clean.
     */
    private final ObjectProperty<BeadEngine.Quantization> quantization =
            new SimpleObjectProperty<>(BeadEngine.Quantization.AVERAGE);
    /**
     * Error diffusion algorithm used when quantizing.
     */
    private final ObjectProperty<BeadEngine.Dithering> dithering =
            new SimpleObjectProperty<>(BeadEngine.Dithering.NONE);
    /**
     * Error diffusion share in [0,1]; 0 disables the diffusion effect.
     * Advanced parameters default to off; users opt in via the panel.
     */
    private final DoubleProperty ditheringStrength = new SimpleDoubleProperty(0.0);
    /**
     * Orphan cleaning strength: 0 off, 1 light, 2 medium, 3 strong (tolerance
     * of 0/1/2 matching neighbours still counts as orphaned).
     */
    private final IntegerProperty orphanTolerance = new SimpleIntegerProperty(0);
    /**
     * Similarity tolerance for colour merging (ΔE2000); 0 disables merging.
     * Advanced parameters default to off; users opt in via the panel.
     */
    private final DoubleProperty mergeThreshold = new SimpleDoubleProperty(0.0);
    /**
     * Colours used by fewer than this share (percent of filled cells) may be
     * merged away; larger colours are protected.
     */
    private final IntegerProperty mergeMinShare = new SimpleIntegerProperty(1);
    /**
     * Active color theme; light is the default.
     */
    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>(Theme.LIGHT);
    /**
     * UI language, resolved at startup from the persisted settings.
     */
    private final ObjectProperty<Language> language = new SimpleObjectProperty<>(Language.EN);

    private AppState() {
    }

    public static AppState get() {
        return INSTANCE;
    }

    public ObjectProperty<Language> languageProperty() {
        return language;
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

    public ObjectProperty<BeadEngine.Quantization> quantizationProperty() {
        return quantization;
    }

    public DoubleProperty ditheringStrengthProperty() {
        return ditheringStrength;
    }

    public IntegerProperty orphanToleranceProperty() {
        return orphanTolerance;
    }

    public DoubleProperty mergeThresholdProperty() {
        return mergeThreshold;
    }

    public IntegerProperty mergeMinShareProperty() {
        return mergeMinShare;
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
}
