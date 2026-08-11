package com.johnie.pixelbead.ui.coordinator;

import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.renderer.PatternExporter;
import com.johnie.pixelbead.ui.components.InteractiveCanvas;
import com.johnie.pixelbead.ui.components.Toasts;
import com.johnie.pixelbead.ui.state.AppState;
import com.johnie.pixelbead.util.TaskUtil;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Path;

/**
 * Owns the export flow: format selection, save dialog, background export task.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/11
 */
public final class ExportCoordinator {

    private final AppState state = AppState.get();
    private final InteractiveCanvas canvas;
    private final StackPane loadingOverlay;
    private final Button exportButton;
    private final ComboBox<ExportFormat> formatCombo;
    private Task<Void> exportTask;

    public ExportCoordinator(InteractiveCanvas canvas, StackPane loadingOverlay,
                             Button exportButton, ComboBox<ExportFormat> formatCombo) {
        this.canvas = canvas;
        this.loadingOverlay = loadingOverlay;
        this.exportButton = exportButton;
        this.formatCombo = formatCombo;
    }

    public void setup() {
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

    public void export() {
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

        Task<Void> task = TaskUtil.run(() -> writePattern(format, project, file.toPath()));
        task.setOnRunning(e -> showOverlay());
        task.setOnSucceeded(e -> {
            hideOverlay();
            Toasts.show(canvas, "Saved to " + file.getName());
        });
        task.setOnFailed(e -> {
            hideOverlay();
            Toasts.showError("Export failed", String.valueOf(task.getException()));
        });
        exportTask = task;
        Thread thread = new Thread(task, "bead-export");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Writes the pattern in the selected format (runs on the background thread).
     */
    private void writePattern(ExportFormat format, PatternProject project, Path file)
            throws Exception {
        switch (format) {
            case PNG -> PatternExporter.writePng(project, file);
            case PDF -> PatternExporter.writePdf(project, file);
            case TEXT -> PatternExporter.writeText(project, file);
        }
    }

    private void showOverlay() {
        loadingOverlay.setVisible(true);
        loadingOverlay.setManaged(true);
    }

    private void hideOverlay() {
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);
    }

    /**
     * Supported export formats.
     */
    public enum ExportFormat {
        PNG, PDF, TEXT
    }
}
