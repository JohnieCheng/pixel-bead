package com.johnie.pixelbead.ui.coordinator;

import com.johnie.pixelbead.engine.BeadEngine;
import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.quantizer.ImageDownsampler;
import com.johnie.pixelbead.ui.components.Toasts;
import com.johnie.pixelbead.ui.dialogs.CropDialog;
import com.johnie.pixelbead.ui.state.AppState;
import com.johnie.pixelbead.util.TaskUtil;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Owns the image-to-pattern pipeline: the background conversion task with
 * generation guarding, the import/crop flow, and the demo seed.
 * <p>
 * The panels write board/interpolation settings into AppState; this
 * coordinator listens for those changes and re-runs the conversion, so it
 * never needs references to the panel controls.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/11
 */
public final class ConversionCoordinator {

    private final AppState state = AppState.get();
    private final StackPane loadingOverlay;
    private final Button importButton;

    private BufferedImage sourceImage;
    private Task<PatternProject> conversionTask;
    /**
     * Incremented per conversion; stale results (older generation) are dropped.
     */
    private int conversionGeneration;

    public ConversionCoordinator(StackPane loadingOverlay, Button importButton) {
        this.loadingOverlay = loadingOverlay;
        this.importButton = importButton;
    }

    /**
     * Re-runs the conversion whenever the board or interpolation settings change.
     */
    public void setup() {
        state.boardProperty().addListener(obs -> regenerate());
        state.boardColumnsProperty().addListener(obs -> regenerate());
        state.boardRowsProperty().addListener(obs -> regenerate());
        state.interpolationProperty().addListener(obs -> regenerate());
        state.paletteProperty().addListener(obs -> regenerate());
        state.ditheringProperty().addListener(obs -> regenerate());
        state.ditheringStrengthProperty().addListener(obs -> regenerate());
        state.orphanCleanProperty().addListener(obs -> regenerate());
        state.mergeThresholdProperty().addListener(obs -> regenerate());
        state.mergeMinShareProperty().addListener(obs -> regenerate());
    }

    /**
     * Seeds the demo source image and runs the first conversion.
     */
    public void startWithDemo() {
        sourceImage = createDemoImage();
        regenerate();
    }

    public void importImage() {
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
                // User cancelled the crop.
                return;
            }
            sourceImage = cropped.get();
            regenerate();
        } catch (IOException e) {
            Toasts.showError("Failed to load image", file.getName() + System.lineSeparator() + e.getMessage());
        }
    }

    /**
     * Runs the conversion pipeline on a background thread. A stale task is
     * cancelled first and results from superseded generations are discarded,
     * so rapid parameter changes always land on the latest settings.
     */
    public void regenerate() {
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

        BeadEngine.ConversionOptions options = new BeadEngine.ConversionOptions(
                interpolation,
                state.ditheringProperty().get(),
                state.ditheringStrengthProperty().get(),
                state.orphanCleanProperty().get(),
                state.mergeThresholdProperty().get(),
                Math.max(0, Math.min(100, state.mergeMinShareProperty().get())));

        Task<PatternProject> task = TaskUtil.call(() ->
                BeadEngine.processImage(source, effectiveBoard, palette, options));
        task.setOnRunning(e -> showOverlay());
        task.setOnSucceeded(e -> {
            if (generation != conversionGeneration) {
                return;
            }
            PatternProject project = task.getValue();
            state.currentProjectProperty().set(project);
            state.resetEditState();
            hideOverlay();
        });
        task.setOnFailed(e -> {
            if (generation != conversionGeneration) {
                return;
            }
            hideOverlay();
            Toasts.showError("Conversion failed", String.valueOf(task.getException()));
        });
        conversionTask = task;
        Thread thread = new Thread(task, "bead-conversion");
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
