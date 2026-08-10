package com.johnie.pixelbead.ui.components;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Layout regression tests: the canvas must track its parent region's size
 * (Canvas is not Resizable, so containers never stretch it on their own).
 */
class InteractiveCanvasTest {

    @BeforeAll
    static void startFx() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        assertTrue(latch.await(10, TimeUnit.SECONDS), "JavaFX platform did not start");
    }

    private static PatternProject smallProject() throws IOException {
        BeadPalette palette = BeadPalette.loadResource("/palettes/mard_standard.json");
        BeadBoard board = BeadBoard.MINI_SMALL; // 29x29
        int[][] grid = new int[29][29];
        for (int[] row : grid) {
            Arrays.fill(row, -1);
        }
        grid[14][14] = 0;
        return new PatternProject(board, palette, grid);
    }

    @Test
    void canvasTracksParentSize() throws Exception {
        AtomicReference<Double> width = new AtomicReference<>(0.0);
        AtomicReference<Double> height = new AtomicReference<>(0.0);
        CountDownLatch latch = new CountDownLatch(1);

        runOnFx(() -> {
            StackPane pane = new StackPane();
            InteractiveCanvas canvas = new InteractiveCanvas();
            pane.getChildren().add(canvas);
            pane.resize(800, 600);
            pane.layout();
            width.set(canvas.getWidth());
            height.set(canvas.getHeight());
            latch.countDown();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "layout timed out");
        assertEquals(800, width.get(), 0.01);
        assertEquals(600, height.get(), 0.01);
    }

    @Test
    void projectFitsOnceSizeIsKnown() throws Exception {
        PatternProject project = smallProject();
        AtomicReference<Double> scale = new AtomicReference<>(0.0);
        CountDownLatch latch = new CountDownLatch(1);

        runOnFx(() -> {
            StackPane pane = new StackPane();
            InteractiveCanvas canvas = new InteractiveCanvas();
            pane.getChildren().add(canvas);
            pane.resize(800, 600);
            pane.layout();
            canvas.projectProperty().set(project);
            // fit happens on the size/project listeners; give the FX pulse a chance.
            Platform.runLater(() -> {
                scale.set(canvas.getScaleForTest());
                latch.countDown();
            });
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "fit timed out");
        // Largest fitting scale for 29x29 in 800x600 minus layout: 600/29 < 800/29 -> 600/29.
        assertTrue(scale.get() > 0, "canvas never fitted the project");
    }

    @Test
    void fitFollowsShrinkingParent() throws Exception {
        // Regression: shrinking the window must re-fit the pattern too.
        PatternProject project = smallProject();
        AtomicReference<Double> bigScale = new AtomicReference<>(0.0);
        AtomicReference<Double> smallScale = new AtomicReference<>(0.0);
        CountDownLatch latch = new CountDownLatch(1);

        runOnFx(() -> {
            StackPane pane = new StackPane();
            InteractiveCanvas canvas = new InteractiveCanvas();
            pane.getChildren().add(canvas);
            pane.resize(800, 600);
            pane.layout();
            canvas.projectProperty().set(project);
            Platform.runLater(() -> {
                bigScale.set(canvas.getScaleForTest());
                pane.resize(400, 300);
                pane.layout();
                Platform.runLater(() -> {
                    smallScale.set(canvas.getScaleForTest());
                    latch.countDown();
                });
            });
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "shrink fit timed out");
        assertTrue(bigScale.get() > smallScale.get(),
                "scale did not shrink with the parent: " + bigScale.get() + " -> " + smallScale.get());
    }

    /** Runs a task on the FX thread and waits for the given latch to be counted. */
    private static void runOnFx(Runnable task) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                task.run();
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS), "FX task timed out");
    }
}
