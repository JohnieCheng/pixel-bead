package com.johnie.pixelbead.ui.panel;

import com.johnie.pixelbead.engine.BeadEngine;
import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;
import com.johnie.pixelbead.ui.state.AppState;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Left panel: tools, undo/redo, board presets, custom board inputs,
 * interpolation and the pattern info line. Everything is written into the
 * shared AppState; the conversion coordinator reacts to state changes.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/11
 */
public class LeftPanelController {

    private static final String BOARD_MINI_STANDARD = "2.6mm 50x50";
    private static final String BOARD_MINI_SMALL = "2.6mm 29x29";
    private static final String BOARD_MIDI_STANDARD = "5mm 29x29";
    private static final String BOARD_MIDI_SMALL = "5mm 14x14";
    private static final String BOARD_CUSTOM = "Custom...";

    private final AppState state = AppState.get();

    @FXML
    private ToggleButton brushTool;
    @FXML
    private ToggleButton eraserTool;
    @FXML
    private ToggleButton pickerTool;
    @FXML
    private Button undoButton;
    @FXML
    private Button redoButton;
    @FXML
    private ComboBox<String> boardCombo;
    @FXML
    private VBox customBoardPane;
    @FXML
    private Spinner<Integer> columnsSpinner;
    @FXML
    private Spinner<Integer> rowsSpinner;
    @FXML
    private Spinner<Double> beadSizeSpinner;
    @FXML
    private Spinner<Integer> subGridSpinner;
    @FXML
    private ComboBox<Integer> boardColsCombo;
    @FXML
    private ComboBox<Integer> boardRowsCombo;
    @FXML
    private ComboBox<ImageDownsampler.Interpolation> interpolationCombo;
    @FXML
    private ComboBox<BeadEngine.Dithering> ditheringCombo;
    @FXML
    private VBox intensityGroup;
    @FXML
    private Slider intensitySlider;
    @FXML
    private Label intensityValue;
    @FXML
    private ComboBox<Integer> orphanCombo;
    @FXML
    private ComboBox<Double> mergeCombo;
    @FXML
    private VBox minShareGroup;
    @FXML
    private Spinner<Integer> minBeadsSpinner;
    @FXML
    private Label patternInfoLabel;

    private boolean customBoardInitialized = false;

    @FXML
    private void initialize() {
        setupTools();
        setupBoardControls();
        setupInterpolationControl();
        setupDitheringControls();
        setupPatternInfo();
        refreshHistoryButtons();
        state.editCountProperty().addListener(obs -> refreshHistoryButtons());
    }

    @FXML
    private void onUndo() {
        state.undo();
    }

    @FXML
    private void onRedo() {
        state.redo();
    }

    private void setupTools() {
        ToggleGroup tools = new ToggleGroup();
        brushTool.setToggleGroup(tools);
        eraserTool.setToggleGroup(tools);
        pickerTool.setToggleGroup(tools);
        tools.selectedToggleProperty().addListener((obs, old, sel) -> {
            if (sel == eraserTool) {
                state.activeToolProperty().set(AppState.ToolType.ERASER);
            } else if (sel == pickerTool) {
                state.activeToolProperty().set(AppState.ToolType.EYEDROPPER);
            } else {
                state.activeToolProperty().set(AppState.ToolType.BRUSH);
            }
        });
    }

    private void setupBoardControls() {
        boardCombo.getItems().addAll(
                BOARD_MINI_STANDARD,
                BOARD_MINI_SMALL,
                BOARD_MIDI_STANDARD,
                BOARD_MIDI_SMALL,
                BOARD_CUSTOM);
        boardCombo.setValue(nameFor(state.boardProperty().get()));
        boardCombo.valueProperty().addListener((obs, oldName, name) -> {
            if (name == null) {
                return;
            }
            if (name.equals(BOARD_CUSTOM)) {
                showCustomBoardControls();
            } else {
                hideCustomBoardControls();
                state.boardProperty().set(boardForName(name));
            }
        });

        boardColsCombo.getItems().addAll(1, 2, 3, 4);
        boardRowsCombo.getItems().addAll(1, 2, 3, 4);
        boardColsCombo.valueProperty().bindBidirectional(state.boardColumnsProperty());
        boardRowsCombo.valueProperty().bindBidirectional(state.boardRowsProperty());
    }

    /**
     * Display name for a preset board; Custom for anything else.
     */
    private String nameFor(BeadBoard board) {
        if (board == BeadBoard.MINI_STANDARD) {
            return BOARD_MINI_STANDARD;
        }
        if (board == BeadBoard.MINI_SMALL) {
            return BOARD_MINI_SMALL;
        }
        if (board == BeadBoard.MIDI_STANDARD) {
            return BOARD_MIDI_STANDARD;
        }
        if (board == BeadBoard.MIDI_SMALL) {
            return BOARD_MIDI_SMALL;
        }
        return BOARD_CUSTOM;
    }

    private BeadBoard boardForName(String name) {
        return switch (name) {
            case BOARD_MINI_STANDARD -> BeadBoard.MINI_STANDARD;
            case BOARD_MINI_SMALL -> BeadBoard.MINI_SMALL;
            case BOARD_MIDI_STANDARD -> BeadBoard.MIDI_STANDARD;
            case BOARD_MIDI_SMALL -> BeadBoard.MIDI_SMALL;
            default -> BeadBoard.MINI_STANDARD;
        };
    }

    private void showCustomBoardControls() {
        initCustomBoardSpinners();
        customBoardPane.setVisible(true);
        customBoardPane.setManaged(true);
        applyCustomBoard();
    }

    private void hideCustomBoardControls() {
        customBoardPane.setVisible(false);
        customBoardPane.setManaged(false);
    }

    /**
     * Creates the spinners once; values persist while switching presets.
     */
    private void initCustomBoardSpinners() {
        if (customBoardInitialized) {
            return;
        }
        customBoardInitialized = true;
        BeadBoard current = state.boardProperty().get();
        columnsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, current.columns()));
        rowsSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, current.rows()));
        beadSizeSpinner.setValueFactory(
                new SpinnerValueFactory.DoubleSpinnerValueFactory(1.0, 10.0, current.beadSizeMm(), 0.1));
        subGridSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 25, current.subGridInterval()));
        columnsSpinner.valueProperty().addListener(obs -> applyCustomBoard());
        rowsSpinner.valueProperty().addListener(obs -> applyCustomBoard());
        beadSizeSpinner.valueProperty().addListener(obs -> applyCustomBoard());
        subGridSpinner.valueProperty().addListener(obs -> applyCustomBoard());
    }

    /**
     * Rebuilds the board from the custom inputs (conversion reacts via AppState).
     */
    private void applyCustomBoard() {
        if (!customBoardInitialized) {
            return;
        }
        state.boardProperty().set(new BeadBoard(
                columnsSpinner.getValue(),
                rowsSpinner.getValue(),
                beadSizeSpinner.getValue(),
                subGridSpinner.getValue()));
    }

    private void setupInterpolationControl() {
        interpolationCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ImageDownsampler.Interpolation mode) {
                return mode == ImageDownsampler.Interpolation.NEAREST ? "Nearest (pixel art)" : "Bilinear (photo)";
            }

            @Override
            public ImageDownsampler.Interpolation fromString(String s) {
                return null;
            }
        });
        interpolationCombo.getItems().addAll(ImageDownsampler.Interpolation.values());
        interpolationCombo.valueProperty().bindBidirectional(state.interpolationProperty());
    }

    /**
     * Dithering algorithm, intensity slider and orphan cleaning (all live in AppState).
     */
    private void setupDitheringControls() {
        ditheringCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(BeadEngine.Dithering d) {
                return switch (d) {
                    case NONE -> "None";
                    case FLOYD_STEINBERG -> "Floyd-Steinberg";
                    case ATKINSON -> "Atkinson";
                };
            }

            @Override
            public BeadEngine.Dithering fromString(String s) {
                return null;
            }
        });
        ditheringCombo.getItems().addAll(BeadEngine.Dithering.values());
        ditheringCombo.valueProperty().bindBidirectional(state.ditheringProperty());
        ditheringCombo.valueProperty().addListener((obs, old, dithering) ->
                setIntensityVisible(dithering != BeadEngine.Dithering.NONE));

        intensitySlider.setMin(0);
        intensitySlider.setMax(1);
        intensitySlider.valueProperty().bindBidirectional(state.ditheringStrengthProperty());
        intensityValue.textProperty().bind(intensitySlider.valueProperty()
                .map(v -> Math.round(v.doubleValue() * 100) + "%"));

        setupOrphanControls();
        setIntensityVisible(state.ditheringProperty().get() != BeadEngine.Dithering.NONE);

        setupMergeControls();
    }

    /** Orphan cleaning strength: Off / Light / Medium / Strong. */
    private void setupOrphanControls() {
        record Level(String label, int tolerance) {
        }
        List<Level> levels = List.of(
                new Level("Off", 0),
                new Level("Light", 1),
                new Level("Medium", 2),
                new Level("Strong", 3));
        for (Level level : levels) {
            orphanCombo.getItems().add(level.tolerance());
        }
        orphanCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer tolerance) {
                for (Level level : levels) {
                    if (level.tolerance() == tolerance) {
                        return level.label();
                    }
                }
                return "Off";
            }

            @Override
            public Integer fromString(String s) {
                return null;
            }
        });
        orphanCombo.valueProperty().addListener((obs, old, tolerance) -> {
            if (tolerance != null) {
                state.orphanToleranceProperty().set(tolerance);
            }
        });
        if (!orphanCombo.getItems().contains(state.orphanToleranceProperty().get())) {
            state.orphanToleranceProperty().set(0);
        }
        orphanCombo.setValue(state.orphanToleranceProperty().get());
    }

    /**
     * Colour merge presets and the low-frequency share threshold.
     */
    private void setupMergeControls() {
        record Preset(String label, double threshold) {
        }
        List<Preset> presets = List.of(
                new Preset("Off", 0.0),
                new Preset("Conservative (ΔE 2)", 2.0),
                new Preset("Standard (ΔE 4)", 4.0),
                new Preset("Aggressive (ΔE 7)", 7.0),
                new Preset("Extreme (ΔE 12)", 12.0));
        for (Preset preset : presets) {
            mergeCombo.getItems().add(preset.threshold());
        }
        mergeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Double threshold) {
                for (Preset preset : presets) {
                    if (preset.threshold() == threshold) {
                        return preset.label();
                    }
                }
                return "Off";
            }

            @Override
            public Double fromString(String s) {
                return null;
            }
        });
        // ComboBox holds boxed Double; sync to the primitive property via listener.
        mergeCombo.valueProperty().addListener((obs, old, threshold) -> {
            if (threshold != null) {
                state.mergeThresholdProperty().set(threshold);
                setMinShareVisible(threshold > 0);
            }
        });
        if (!mergeCombo.getItems().contains(state.mergeThresholdProperty().get())) {
            state.mergeThresholdProperty().set(0.0);
        }
        mergeCombo.setValue(state.mergeThresholdProperty().get());
        setMinShareVisible(state.mergeThresholdProperty().get() > 0);

        minBeadsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 1));
        // Spinner value is read-only; sync to the shared state via listener.
        // Editable spinners can commit out-of-range values (e.g. 0), so clamp
        // and echo the corrected value back.
        minBeadsSpinner.valueProperty().addListener((obs, old, minShare) -> {
            if (minShare == null) {
                return;
            }
            int clamped = Math.max(0, Math.min(100, minShare));
            state.mergeMinShareProperty().set(clamped);
            if (clamped != minShare) {
                minBeadsSpinner.getValueFactory().setValue(clamped);
            }
        });
    }

    /**
     * Intensity is only meaningful when a dithering algorithm is active.
     */
    private void setIntensityVisible(boolean visible) {
        intensityGroup.setVisible(visible);
        intensityGroup.setManaged(visible);
    }

    /** Min share is only meaningful when colour merging is enabled. */
    private void setMinShareVisible(boolean visible) {
        minShareGroup.setVisible(visible);
        minShareGroup.setManaged(visible);
    }

    /**
     * Pattern info follows the current project and any grid edit.
     */
    private void setupPatternInfo() {
        Runnable update = () -> {
            PatternProject project = state.currentProjectProperty().get();
            if (project == null) {
                return;
            }
            BeadBoard board = project.board();
            int[] counts = project.colorCounts();
            int used = 0;
            int beadCount = 0;
            for (int count : counts) {
                if (count > 0) {
                    used++;
                    beadCount += count;
                }
            }
            String size = String.format("%.1fmm", board.beadSizeMm());
            patternInfoLabel.setText(size + "  " + board.columns() + "x" + board.rows()
                    + "  \u00b7  " + beadCount + " beads  \u00b7  " + used + " colors");
        };
        state.currentProjectProperty().addListener(obs -> update.run());
        state.editCountProperty().addListener(obs -> update.run());
        update.run();
    }

    private void refreshHistoryButtons() {
        undoButton.setDisable(!state.editHistory().canUndo());
        redoButton.setDisable(!state.editHistory().canRedo());
    }
}
