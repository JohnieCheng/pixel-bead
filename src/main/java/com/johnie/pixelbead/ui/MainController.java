package com.johnie.pixelbead.ui;

import com.johnie.pixelbead.engine.BeadEngine;
import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadColor;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;
import com.johnie.pixelbead.engine.renderer.PatternExporter;
import com.johnie.pixelbead.ui.components.InteractiveCanvas;
import com.johnie.pixelbead.ui.state.AppState;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
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
import java.util.List;

/**
 * Main window controller: wires the AppState to the FXML layout and drives
 * the engine conversion pipeline.
 */
public class MainController {

    /** One row of the bead count table. */
    public record BeadCountRow(BeadColor color, int count) {
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
    }

    private void setupPalettePane() {
        BeadPalette palette = state.paletteProperty().get();
        palettePane.getChildren().clear();
        for (BeadColor color : palette.colors()) {
            Rectangle swatch = new Rectangle(SWATCH_SIZE, SWATCH_SIZE);
            swatch.setFill(Color.rgb(color.r(), color.g(), color.b()));
            swatch.setStroke(Color.web("#cccccc"));
            Tooltip.install(swatch, new Tooltip(color.code()));
            palettePane.getChildren().add(swatch);
        }
    }

    @FXML
    private void onFitView() {
        canvas.fitToView();
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
            sourceImage = image;
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
            showInfo("Export complete", "Saved to " + file.getAbsolutePath());
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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
                rows.add(new BeadCountRow(project.palette().colorAt(i), counts[i]));
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
