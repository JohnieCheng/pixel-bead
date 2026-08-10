package com.johnie.pixelbead.ui.state;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Global application state, shared between controllers and views.
 * <p>
 * UI components observe these properties and react to changes; the engine
 * layer never touches this class.
 */
public final class AppState {

    /** Color themes; the UI root carries the matching CSS class. */
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

    /** Active color theme; light is the default. */
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

    public ObjectProperty<Theme> themeProperty() {
        return theme;
    }
}
