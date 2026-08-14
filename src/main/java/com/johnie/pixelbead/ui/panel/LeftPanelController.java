package com.johnie.pixelbead.ui.panel;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.enums.*;
import com.johnie.pixelbead.ui.state.AppState;
import com.johnie.pixelbead.util.I18n;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

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
    private ComboBox<Interpolation> interpolationCombo;
    @FXML
    private ComboBox<Quantization> quantizationCombo;
    @FXML
    private ComboBox<Dithering> ditheringCombo;
    @FXML
    private VBox intensityGroup;
    @FXML
    private Slider intensitySlider;
    @FXML
    private Label intensityValue;
    @FXML
    private ComboBox<OrphanLevel> orphanCombo;
    @FXML
    private ComboBox<MergePreset> mergeCombo;
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
        setupQuantizationControl();
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
                state.activeToolProperty().set(ToolType.ERASER);
            } else if (sel == pickerTool) {
                state.activeToolProperty().set(ToolType.EYEDROPPER);
            } else {
                state.activeToolProperty().set(ToolType.BRUSH);
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
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, Math.max(current.columns(), current.rows()), current.subGridInterval()));
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
        interpolationCombo.setConverter(I18n.enumConverter());
        interpolationCombo.getItems().addAll(Interpolation.values());
        interpolationCombo.valueProperty().bindBidirectional(state.interpolationProperty());
    }

    /**
     * Pixel mode (nearest sample vs region average); average disables dithering.
     */
    private void setupQuantizationControl() {
        quantizationCombo.setConverter(I18n.enumConverter());
        quantizationCombo.getItems().addAll(Quantization.values());
        quantizationCombo.valueProperty().bindBidirectional(state.quantizationProperty());
        quantizationCombo.valueProperty().addListener((obs, old, quantization) ->
                applyQuantizationDisables(quantization));
        applyQuantizationDisables(state.quantizationProperty().get());
    }

    /**
     * Dithering is meaningless on region-averaged cells.
     */
    private void applyQuantizationDisables(Quantization quantization) {
        boolean average = quantization == Quantization.AVERAGE;
        ditheringCombo.setDisable(average);
        setIntensityVisible(!average && state.ditheringProperty().get() != Dithering.NONE);
    }

    /**
     * Dithering algorithm, intensity slider and orphan cleaning (all live in AppState).
     */
    private void setupDitheringControls() {
        ditheringCombo.setConverter(I18n.enumConverter());
        ditheringCombo.getItems().addAll(Dithering.values());
        ditheringCombo.valueProperty().bindBidirectional(state.ditheringProperty());
        ditheringCombo.valueProperty().addListener((obs, old, dithering) ->
                setIntensityVisible(dithering != Dithering.NONE));

        intensitySlider.setMin(0);
        intensitySlider.setMax(1);
        intensitySlider.valueProperty().bindBidirectional(state.ditheringStrengthProperty());
        intensityValue.textProperty().bind(intensitySlider.valueProperty()
                .map(v -> Math.round(v.doubleValue() * 100) + "%"));

        setupOrphanControls();
        setIntensityVisible(state.ditheringProperty().get() != Dithering.NONE);

        setupMergeControls();
    }

    private void setupOrphanControls() {
        orphanCombo.setConverter(I18n.enumConverter());
        orphanCombo.getItems().addAll(OrphanLevel.values());
        orphanCombo.valueProperty().addListener((obs, old, level) -> {
            if (level != null) {
                state.orphanToleranceProperty().set(level.tolerance());
            }
        });
        orphanCombo.setValue(OrphanLevel.fromTolerance(state.orphanToleranceProperty().get()));
    }

    private void setupMergeControls() {
        mergeCombo.setConverter(I18n.enumConverter());
        mergeCombo.getItems().addAll(MergePreset.values());
        // ComboBox item is the enum; sync its value to the primitive property.
        mergeCombo.valueProperty().addListener((obs, old, preset) -> {
            if (preset != null) {
                state.mergeThresholdProperty().set(preset.threshold());
                setMinShareVisible(preset.threshold() > 0);
            }
        });
        mergeCombo.setValue(MergePreset.fromThreshold(state.mergeThresholdProperty().get()));
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

    /**
     * Min share is only meaningful when colour merging is enabled.
     */
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
