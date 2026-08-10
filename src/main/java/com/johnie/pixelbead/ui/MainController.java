package com.johnie.pixelbead.ui;

import com.johnie.pixelbead.engine.BeadEngine;
import com.johnie.pixelbead.engine.quantizer.ColorDifference;
import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadColor;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;
import com.johnie.pixelbead.engine.renderer.PatternExporter;
import com.johnie.pixelbead.ui.components.InteractiveCanvas;
import com.johnie.pixelbead.ui.dialogs.CropDialog;
import com.johnie.pixelbead.ui.state.AppState;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Main window controller: wires the AppState to the FXML layout and drives
 * the engine conversion pipeline.
 */
public class MainController {

    /** One row of the bead count table. */
    public record BeadCountRow(int colorIndex, BeadColor color, int count) {
    }

    /** Supported export formats. */
    private enum ExportFormat {
        PNG, PDF, TEXT
    }

    private static final int SWATCH_SIZE = 20;

    private final AppState state = AppState.get();

    @FXML
    private Button importButton;
    @FXML
    private Button exportButton;
    @FXML
    private ComboBox<ExportFormat> formatCombo;
    @FXML
    private InteractiveCanvas canvas;
    @FXML
    private ComboBox<String> boardCombo;
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
    private Button themeButton;
    @FXML
    private ToggleButton previewButton;
    @FXML
    private BorderPane root;
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
    private FlowPane palettePane;
    @FXML
    private TableView<BeadCountRow> countTable;
    @FXML
    private TableColumn<BeadCountRow, String> codeColumn;
    @FXML
    private TableColumn<BeadCountRow, BeadColor> swatchColumn;
    @FXML
    private TableColumn<BeadCountRow, Integer> countColumn;
    @FXML
    private Label hoverLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label patternInfoLabel;
    @FXML
    private Label boardSizeLabel;
    @FXML
    private StackPane loadingOverlay;

    private BufferedImage sourceImage;
    private Task<PatternProject> conversionTask;
    private Task<Void> exportTask;
    /** Incremented per conversion; stale results (older generation) are dropped. */
    private int conversionGeneration;

    @FXML
    private void initialize() {
        try {
            state.paletteProperty().set(BeadPalette.loadResource("/palettes/mard_standard.json"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load palette", e);
        }

        setupBoardControls();
        setupInterpolationControl();
        setupCountTable();
        setupPalettePane();
        setupExportControls();
        setupTools();
        setupTheme();

        canvas.projectProperty().bind(state.currentProjectProperty());
        hoverLabel.textProperty().bind(canvas.hoverInfoProperty());

        sourceImage = createDemoImage();
        regenerate();
    }

    private static final String BOARD_MINI_STANDARD = "2.6mm 50x50";
    private static final String BOARD_MINI_SMALL = "2.6mm 29x29";
    private static final String BOARD_MIDI_STANDARD = "5mm 29x29";
    private static final String BOARD_MIDI_SMALL = "5mm 14x14";
    private static final String BOARD_CUSTOM = "Custom...";

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
                regenerate();
            }
        });

        boardColsCombo.getItems().addAll(1, 2, 3, 4);
        boardRowsCombo.getItems().addAll(1, 2, 3, 4);
        boardColsCombo.valueProperty().bindBidirectional(state.boardColumnsProperty());
        boardRowsCombo.valueProperty().bindBidirectional(state.boardRowsProperty());
        boardColsCombo.valueProperty().addListener(obs -> regenerate());
        boardRowsCombo.valueProperty().addListener(obs -> regenerate());
    }

    /** Display name for a preset board; Custom for anything else. */
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

    private boolean customBoardInitialized = false;

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

    /** Creates the spinners once; values persist while switching presets. */
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

    /** Rebuilds the board from the custom inputs and re-runs the conversion. */
    private void applyCustomBoard() {
        if (!customBoardInitialized) {
            return;
        }
        state.boardProperty().set(new BeadBoard(
                columnsSpinner.getValue(),
                rowsSpinner.getValue(),
                beadSizeSpinner.getValue(),
                subGridSpinner.getValue()));
        regenerate();
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
        interpolationCombo.valueProperty().addListener(obs -> regenerate());
    }

    private void setupCountTable() {
        codeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().color().code()));
        swatchColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().color()));
        swatchColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BeadColor color, boolean empty) {
                super.updateItem(color, empty);
                if (empty || color == null) {
                    setGraphic(null);
                } else {
                    Rectangle rect = new Rectangle(16, 16);
                    rect.setFill(Color.rgb(color.r(), color.g(), color.b()));
                    rect.setStroke(Color.web("#999999"));
                    setGraphic(rect);
                }
            }
        });
        countColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().count()));
        setupCountTableInteractions();
    }

    /** Hover highlights the colour on canvas; right-click opens the replace picker. */
    private void setupCountTableInteractions() {
        countTable.setRowFactory(tv -> {
            TableRow<BeadCountRow> row = new TableRow<>();
            row.hoverProperty().addListener((obs, was, hover) -> {
                if (hover && !row.isEmpty()) {
                    canvas.setHighlight(row.getItem().colorIndex());
                } else if (was) {
                    canvas.clearHighlight();
                }
            });
            row.setOnContextMenuRequested(e -> {
                if (row.isEmpty()) {
                    return;
                }
                showReplacePicker(row.getItem(), e.getScreenX(), e.getScreenY());
            });
            return row;
        });
    }

    /** Popup listing similar colours (ΔE2000) plus a palette picker entry. */
    private void showReplacePicker(BeadCountRow source, double screenX, double screenY) {
        PatternProject project = state.currentProjectProperty().get();
        if (project == null) {
            return;
        }
        BeadPalette palette = project.palette();
        int from = source.colorIndex();

        List<Integer> similar = IntStream.range(0, palette.size())
                .filter(i -> i != from)
                .boxed()
                .sorted(Comparator.comparingDouble(
                        i -> ColorDifference.de2000(palette.colorAt(from).lab(), palette.colorAt(i).lab())))
                .limit(8)
                .toList();

        VBox box = new VBox(4);
        // Popup content lives outside the main root: carry the theme classes
        // so -pixel-* variables resolve (same pattern as CropDialog).
        box.getStyleClass().addAll("root", "replace-picker",
                state.themeProperty().get() == AppState.Theme.DARK ? "theme-dark" : "theme-light");
        Label header = new Label(source.color().code() + " \u00b7 " + source.count() + " beads");
        header.getStyleClass().add("replace-picker-header");
        box.getChildren().add(header);

        Popup popup = new Popup();
        popup.setAutoHide(true);
        for (int idx : similar) {
            box.getChildren().add(buildPickerItem(popup, palette, from, idx));
        }
        box.getChildren().add(buildPickerItem(popup, palette, from, -1));

        popup.setOnHiding(e -> {
            canvas.clearHighlight();
            canvas.clearReplacePreview();
        });
        popup.getContent().add(box);
        popup.show(countTable.getScene().getWindow(), screenX, screenY);
    }

    private HBox buildPickerItem(Popup popup, BeadPalette palette, int from, int target) {
        HBox item = new HBox(8);
        item.getStyleClass().add("replace-picker-item");
        String text;
        if (target >= 0) {
            BeadColor color = palette.colorAt(target);
            Rectangle swatch = new Rectangle(14, 14);
            swatch.setFill(Color.rgb(color.r(), color.g(), color.b()));
            swatch.setStroke(Color.web("#999999"));
            swatch.setStrokeWidth(0.5);
            item.getChildren().add(swatch);
            double dE = ColorDifference.de2000(palette.colorAt(from).lab(), color.lab());
            text = color.code() + "  \u0394E " + String.format("%.1f", dE);
        } else {
            text = "Pick from palette...";
        }
        item.getChildren().add(new Label(text));

        item.setOnMouseEntered(e -> {
            canvas.clearHighlight();
            if (target >= 0) {
                canvas.setReplacePreview(from, target);
            }
        });
        item.setOnMouseExited(e -> canvas.clearReplacePreview());
        item.setOnMouseClicked(e -> {
            popup.hide();
            if (target >= 0) {
                executeReplace(from, target);
            } else {
                state.replaceFromIndexProperty().set(from);
            }
        });
        return item;
    }

    /** Replaces all cells of fromIndex with toIndex as one undo step. */
    private void executeReplace(int fromIndex, int toIndex) {
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
        showToast("Replaced " + replaced + " cells with " + project.palette().colorAt(toIndex).code());
    }

    private void cancelReplaceMode() {
        state.replaceFromIndexProperty().set(-1);
        canvas.clearReplacePreview();
        updateReplaceHint(false);
    }

    private void updateReplaceHint(boolean picking) {
        statusLabel.setText(picking
                ? "Pick a target colour in the palette (Esc to cancel)"
                : "");
    }

    private void setupPalettePane() {
        BeadPalette palette = state.paletteProperty().get();
        palettePane.getChildren().clear();
        for (int i = 0; i < palette.size(); i++) {
            BeadColor color = palette.colorAt(i);
            Rectangle swatch = new Rectangle(SWATCH_SIZE, SWATCH_SIZE);
            swatch.setFill(Color.rgb(color.r(), color.g(), color.b()));
            swatch.setStroke(Color.web("#3A3F4B"));
            swatch.setStrokeWidth(1);
            Tooltip.install(swatch, new Tooltip(color.code()));
            final int index = i;
            swatch.setOnMouseEntered(e -> {
                if (state.replaceFromIndexProperty().get() >= 0) {
                    canvas.setReplacePreview(state.replaceFromIndexProperty().get(), index);
                }
            });
            swatch.setOnMouseExited(e -> {
                if (state.replaceFromIndexProperty().get() >= 0) {
                    canvas.clearReplacePreview();
                }
            });
            swatch.setOnMouseClicked(e -> {
                if (state.replaceFromIndexProperty().get() >= 0) {
                    executeReplace(state.replaceFromIndexProperty().get(), index);
                    return;
                }
                state.selectedColorIndexProperty().set(index);
            });
            palettePane.getChildren().add(swatch);
        }
        state.selectedColorIndexProperty().addListener((obs, old, idx) -> refreshSwatchHighlight());
        refreshSwatchHighlight();
    }

    /** Highlights the selected palette swatch with the accent color. */
    private void refreshSwatchHighlight() {
        int selected = state.selectedColorIndexProperty().get();
        for (int i = 0; i < palettePane.getChildren().size(); i++) {
            Node node = palettePane.getChildren().get(i);
            if (node instanceof Rectangle swatch) {
                if (i == selected) {
                    swatch.setStroke(Color.web("#61AFEF"));
                    swatch.setStrokeWidth(2);
                } else {
                    swatch.setStroke(Color.web("#3A3F4B"));
                    swatch.setStrokeWidth(1);
                }
            }
        }
    }

    /** Wires the tool toggles, undo/redo buttons, shortcuts and edit stats. */
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

        // Grid edits refresh the stats table and history buttons.
        state.editCountProperty().addListener(obs -> {
            PatternProject p = state.currentProjectProperty().get();
            if (p != null) {
                updateCountTable(p);
                updatePatternInfo(p);
            }
            refreshHistoryButtons();
        });

        // Replace-mode hint follows the shared state (palette pick, canvas cancel, Esc).
        state.replaceFromIndexProperty().addListener((obs, old, idx) -> updateReplaceHint(idx.intValue() >= 0));

        Platform.runLater(() -> {
            Scene scene = importButton.getScene();
            if (scene == null) {
                return;
            }
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), this::undo);
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), this::redo);
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.ESCAPE), this::cancelReplaceMode);
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.P), () -> {
                        previewButton.setSelected(!previewButton.isSelected());
                        onTogglePreview();
                    });
        });
    }

    @FXML
    private void onUndo() {
        undo();
    }

    @FXML
    private void onRedo() {
        redo();
    }

    private void undo() {
        PatternProject p = state.currentProjectProperty().get();
        if (p == null) {
            return;
        }
        if (state.editHistory().undo(p.grid())) {
            state.editCountProperty().set(state.editCountProperty().get() + 1);
        }
    }

    private void redo() {
        PatternProject p = state.currentProjectProperty().get();
        if (p == null) {
            return;
        }
        if (state.editHistory().redo(p.grid())) {
            state.editCountProperty().set(state.editCountProperty().get() + 1);
        }
    }

    private void refreshHistoryButtons() {
        undoButton.setDisable(!state.editHistory().canUndo());
        redoButton.setDisable(!state.editHistory().canRedo());
    }

    @FXML
    private void onFitView() {
        canvas.fitToView();
    }

    @FXML
    private void onToggleTheme() {
        AppState.Theme next = state.themeProperty().get() == AppState.Theme.DARK
                ? AppState.Theme.LIGHT
                : AppState.Theme.DARK;
        state.themeProperty().set(next);
    }

    @FXML
    private void onTogglePreview() {
        canvas.setPreviewMode(previewButton.isSelected());
    }

    private void setupTheme() {
        applyThemeClass();
        themeButton.setText(state.themeProperty().get() == AppState.Theme.DARK ? "Dark" : "Light");
        state.themeProperty().addListener((obs, old, theme) -> {
            applyThemeClass();
            themeButton.setText(theme == AppState.Theme.DARK ? "Dark" : "Light");
        });
    }

    private void applyThemeClass() {
        root.getStyleClass().removeAll("theme-dark", "theme-light");
        root.getStyleClass().add(state.themeProperty().get() == AppState.Theme.DARK ? "theme-dark" : "theme-light");
    }

    @FXML
    private void onImportImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("GIF", "*.gif"),
                new FileChooser.ExtensionFilter("BMP", "*.bmp"));
        File file = chooser.showOpenDialog(importButton.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IOException("Unsupported or corrupted image");
            }
            Optional<BufferedImage> cropped = CropDialog.show(image, importButton.getScene().getWindow());
            if (cropped.isEmpty()) {
                return; // user cancelled the crop
            }
            sourceImage = cropped.get();
            regenerate();
        } catch (IOException e) {
            showError("Failed to load image", file.getName() + System.lineSeparator() + e.getMessage());
        }
    }

    /**
     * Runs the conversion pipeline on a background thread. A stale task is
     * cancelled first and results from superseded generations are discarded,
     * so rapid parameter changes always land on the latest settings.
     */
    private void regenerate() {
        if (sourceImage == null) {
            return;
        }
        if (conversionTask != null && conversionTask.isRunning()) {
            conversionTask.cancel();
        }
        int generation = ++conversionGeneration;

        BeadBoard board = state.boardProperty().get();
        int cols = board.columns(state.boardColumnsProperty().get());
        int rows = board.rows(state.boardRowsProperty().get());
        BeadBoard effectiveBoard = new BeadBoard(cols, rows, board.beadSizeMm(), board.subGridInterval());
        BeadPalette palette = state.paletteProperty().get();
        ImageDownsampler.Interpolation interpolation = state.interpolationProperty().get();
        BufferedImage source = sourceImage;

        Task<PatternProject> task = new Task<>() {
            @Override
            protected PatternProject call() {
                return BeadEngine.processImage(source, effectiveBoard, palette, interpolation);
            }
        };
        task.setOnRunning(e -> showOverlay());
        task.setOnSucceeded(e -> {
            if (generation != conversionGeneration) {
                return;
            }
            PatternProject project = task.getValue();
            state.currentProjectProperty().set(project);
            state.resetEditState();
            refreshHistoryButtons();
            updatePatternInfo(project);
            updateCountTable(project);
            hideOverlay();
        });
        task.setOnFailed(e -> {
            if (generation != conversionGeneration) {
                return;
            }
            hideOverlay();
            showError("Conversion failed", String.valueOf(task.getException()));
        });
        task.setOnCancelled(e -> {
            // Cancellation only happens when superseded by a newer conversion;
            // the newer task owns the overlay then.
        });
        conversionTask = task;
        Thread thread = new Thread(task, "bead-conversion");
        thread.setDaemon(true);
        thread.start();
    }

    private void setupExportControls() {
        formatCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ExportFormat format) {
                return switch (format) {
                    case PNG -> "PNG";
                    case PDF -> "PDF";
                    case TEXT -> "Text";
                };
            }

            @Override
            public ExportFormat fromString(String s) {
                return null;
            }
        });
        formatCombo.getItems().addAll(ExportFormat.values());
        formatCombo.setValue(ExportFormat.PNG);
        exportButton.disableProperty().bind(state.currentProjectProperty().isNull());
    }

    @FXML
    private void onExport() {
        PatternProject project = state.currentProjectProperty().get();
        ExportFormat format = formatCombo.getValue();
        if (project == null || format == null || exportTask != null && exportTask.isRunning()) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Pattern");
        switch (format) {
            case PNG -> {
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
                chooser.setInitialFileName("pattern.png");
            }
            case PDF -> {
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document", "*.pdf"));
                chooser.setInitialFileName("pattern.pdf");
            }
            case TEXT -> {
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text", "*.txt"));
                chooser.setInitialFileName("pattern.txt");
            }
        }
        File file = chooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file == null) {
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                switch (format) {
                    case PNG -> PatternExporter.writePng(project, file.toPath());
                    case PDF -> PatternExporter.writePdf(project, file.toPath());
                    case TEXT -> PatternExporter.writeText(project, file.toPath());
                }
                return null;
            }
        };
        task.setOnRunning(e -> showOverlay());
        task.setOnSucceeded(e -> {
            hideOverlay();
            showToast("Saved to " + file.getName());
        });
        task.setOnFailed(e -> {
            hideOverlay();
            showError("Export failed", String.valueOf(task.getException()));
        });
        exportTask = task;
        Thread thread = new Thread(task, "bead-export");
        thread.setDaemon(true);
        thread.start();
    }

    private void showOverlay() {
        loadingOverlay.setVisible(true);
        loadingOverlay.setManaged(true);
    }

    private void hideOverlay() {
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);
    }

    private void showToast(String message) {
        Label toast = new Label(message);
        toast.getStyleClass().add("toast");
        Node center = canvas.getParent();
        if (center instanceof javafx.scene.layout.StackPane stack) {
            stack.getChildren().add(toast);
            javafx.scene.layout.StackPane.setAlignment(toast, javafx.geometry.Pos.BOTTOM_RIGHT);
            javafx.scene.layout.StackPane.setMargin(toast, new javafx.geometry.Insets(0, 16, 16, 0));
            toast.setOpacity(0);
            javafx.animation.FadeTransition in = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), toast);
            in.setToValue(1);
            javafx.animation.PauseTransition hold = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.5));
            javafx.animation.FadeTransition out = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), toast);
            out.setToValue(0);
            out.setOnFinished(e -> stack.getChildren().remove(toast));
            in.setOnFinished(e -> hold.play());
            hold.setOnFinished(e -> out.play());
            in.play();
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updatePatternInfo(PatternProject project) {
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
        boardSizeLabel.setText(board.columns() + " x " + board.rows());
    }

    private void updateCountTable(PatternProject project) {
        int[] counts = project.colorCounts();
        ObservableList<BeadCountRow> rows = FXCollections.observableArrayList();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                rows.add(new BeadCountRow(i, project.palette().colorAt(i), counts[i]));
            }
        }
        rows.sort((a, b) -> Integer.compare(b.count(), a.count()));
        countTable.setItems(rows);
    }

    /**
     * Demo source image: a smooth gradient with a sinusoidal blue channel,
     * so the converted pattern shows off both color transitions and the palette.
     */
    private BufferedImage createDemoImage() {
        int w = 320;
        int h = 240;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = (int) (255.0 * x / w);
                int g = (int) (255.0 * y / h);
                int b = (int) (128.0 + 127.0 * Math.sin(x / 20.0));
                img.setRGB(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }
}
