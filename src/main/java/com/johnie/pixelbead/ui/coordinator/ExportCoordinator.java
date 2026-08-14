package com.johnie.pixelbead.ui.coordinator;

import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.renderer.PatternExporter;
import com.johnie.pixelbead.enums.ExportFormat;
import com.johnie.pixelbead.ui.components.InteractiveCanvas;
import com.johnie.pixelbead.ui.components.Toasts;
import com.johnie.pixelbead.ui.state.AppState;
import com.johnie.pixelbead.util.I18n;
import com.johnie.pixelbead.util.TaskUtil;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;

/**
 * Owns the export flow: format selection, save dialog, background export task.
 *
 * @author johnie
 * @version 3.0.0
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
        formatCombo.setConverter(I18n.enumConverter());
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
            case CSV -> {
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (Spreadsheet)", "*.csv"));
                chooser.setInitialFileName("pattern.csv");
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
            Toasts.show(canvas, I18n.format("toast.savedTo", file.getName()));
        });
        task.setOnFailed(e -> {
            hideOverlay();
            Toasts.showError(I18n.get("error.exportFailed"), String.valueOf(task.getException()));
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
            case PDF -> {
                int tile = project.board().subGridInterval();
                if (tile >= 2) {
                    // Multi-board tiling: overview + one A4 page per sub-grid tile.
                    PatternExporter.writeTiledPdf(project, tile, file);
                } else {
                    PatternExporter.writePdf(project, file);
                }
            }
            case CSV -> PatternExporter.writeCsv(project, file);
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

}
