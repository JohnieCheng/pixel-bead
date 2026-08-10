package com.johnie.pixelbead.ui.components;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadColor;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Infinite-viewport canvas for bead pattern display.
 * <p>
 * A single Canvas node is redrawn on demand: cells, fine grid lines, bold
 * sub-grid lines every {@code subGridInterval} cells and the outer border.
 * Scroll wheel zooms around the mouse position; Space+drag or middle-button
 * drag pans. Hover position is published as a status string.
 */
public class InteractiveCanvas extends Canvas {

    private static final double MIN_SCALE = 2.0;
    private static final double MAX_SCALE = 64.0;
    private static final double ZOOM_STEP = 1.15;

    // One Dark palette (matches css/style.css).
    private static final Color BACKGROUND = Color.web("#1E2228");
    private static final Color CELL_LINE = Color.web("#2B303B");
    private static final Color SUB_GRID_LINE = Color.web("#3A3F4B");
    private static final Color BORDER = Color.web("#565C6A");
    private static final Color EMPTY_HINT = Color.web("#9AA2B0");

    private final ObjectProperty<PatternProject> project = new SimpleObjectProperty<>();
    private final ObjectProperty<String> hoverInfo = new SimpleObjectProperty<>("");

    private double scale = 12.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private boolean panning = false;
    private double lastX;
    private double lastY;
    private boolean spaceDown = false;
    private boolean fitted = false;

    public InteractiveCanvas() {
        // Canvas is not Resizable; containers never stretch it. Bind our size
        // to the parent region so the drawing surface follows the layout.
        parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent instanceof Region region) {
                widthProperty().bind(region.widthProperty());
                heightProperty().bind(region.heightProperty());
            }
        });
        widthProperty().addListener(obs -> onSizeChanged());
        heightProperty().addListener(obs -> onSizeChanged());
        project.addListener(obs -> fitToView());
        setFocusTraversable(true);
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                spaceDown = true;
            }
        });
        setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                spaceDown = false;
            }
        });
        setOnScroll(this::handleScroll);
        setOnMousePressed(this::handlePressed);
        setOnMouseDragged(this::handleDragged);
        setOnMouseMoved(this::handleMoved);
        setOnMouseExited(e -> hoverInfo.set(""));
    }

    /** Fits once the first real size arrives; afterwards keep the user's zoom. */
    private void onSizeChanged() {
        if (!fitted && project.get() != null && getWidth() > 0 && getHeight() > 0) {
            fitToView();
        } else {
            redraw();
        }
    }

    public ObjectProperty<PatternProject> projectProperty() {
        return project;
    }

    public ObjectProperty<String> hoverInfoProperty() {
        return hoverInfo;
    }

    /** Test hook: current pixels-per-cell scale. */
    double getScaleForTest() {
        return scale;
    }

    /** Centers the pattern in the viewport at the largest fitting scale. */
    public void fitToView() {
        PatternProject p = project.get();
        if (p == null) {
            redraw();
            return;
        }
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        scale = Math.min(w / p.board().columns(), h / p.board().rows());
        offsetX = (w - p.board().columns() * scale) / 2.0;
        offsetY = (h - p.board().rows() * scale) / 2.0;
        fitted = true;
        redraw();
    }

    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, w, h);

        PatternProject p = project.get();
        if (p == null) {
            gc.setFill(EMPTY_HINT);
            gc.fillText("Import an image to generate a bead pattern", 24, 28);
            return;
        }

        BeadBoard board = p.board();
        BeadPalette palette = p.palette();
        int[][] grid = p.grid();
        int cols = board.columns();
        int rows = board.rows();

        // Only paint cells inside the viewport.
        int x0 = Math.max(0, (int) Math.floor((0.0 - offsetX) / scale));
        int x1 = Math.min(cols, (int) Math.ceil((w - offsetX) / scale) + 1);
        int y0 = Math.max(0, (int) Math.floor((0.0 - offsetY) / scale));
        int y1 = Math.min(rows, (int) Math.ceil((h - offsetY) / scale) + 1);

        // Bead cells.
        for (int y = y0; y < y1; y++) {
            double py = offsetY + y * scale;
            for (int x = x0; x < x1; x++) {
                int idx = grid[y][x];
                if (idx < 0) {
                    continue;
                }
                BeadColor c = palette.colorAt(idx);
                gc.setFill(Color.rgb(c.r(), c.g(), c.b()));
                gc.fillRect(offsetX + x * scale, py, scale, scale);
            }
        }

        // Fine grid lines.
        gc.setStroke(CELL_LINE);
        gc.setLineWidth(0.5);
        for (int x = x0; x <= x1; x++) {
            double px = offsetX + x * scale;
            gc.strokeLine(px, 0, px, h);
        }
        for (int y = y0; y <= y1; y++) {
            double py = offsetY + y * scale;
            gc.strokeLine(0, py, w, py);
        }

        // Bold sub-grid lines.
        int interval = board.subGridInterval();
        gc.setStroke(SUB_GRID_LINE);
        gc.setLineWidth(1.2);
        for (int x = 0; x <= cols; x += interval) {
            double px = offsetX + x * scale;
            gc.strokeLine(px, 0, px, h);
        }
        for (int y = 0; y <= rows; y += interval) {
            double py = offsetY + y * scale;
            gc.strokeLine(0, py, w, py);
        }

        // Outer border.
        gc.setStroke(BORDER);
        gc.setLineWidth(1.5);
        gc.strokeRect(offsetX, offsetY, cols * scale, rows * scale);
    }

    private void handleScroll(ScrollEvent e) {
        double mx = e.getX();
        double my = e.getY();
        double factor = e.getDeltaY() > 0 ? ZOOM_STEP : 1.0 / ZOOM_STEP;
        double newScale = Math.clamp(scale * factor, MIN_SCALE, MAX_SCALE);
        if (newScale == scale) {
            return;
        }
        // Keep the cell under the mouse fixed while zooming.
        offsetX = mx - (mx - offsetX) * (newScale / scale);
        offsetY = my - (my - offsetY) * (newScale / scale);
        scale = newScale;
        redraw();
    }

    private void handlePressed(MouseEvent e) {
        if (spaceDown || e.isMiddleButtonDown()) {
            panning = true;
            lastX = e.getX();
            lastY = e.getY();
        }
    }

    private void handleDragged(MouseEvent e) {
        if (panning) {
            offsetX += e.getX() - lastX;
            offsetY += e.getY() - lastY;
            lastX = e.getX();
            lastY = e.getY();
            redraw();
        }
    }

    private void handleMoved(MouseEvent e) {
        PatternProject p = project.get();
        if (p == null) {
            hoverInfo.set("");
            return;
        }
        int cellX = (int) Math.floor((e.getX() - offsetX) / scale);
        int cellY = (int) Math.floor((e.getY() - offsetY) / scale);
        if (cellX < 0 || cellY < 0 || cellX >= p.board().columns() || cellY >= p.board().rows()) {
            hoverInfo.set("");
            return;
        }
        int idx = p.grid()[cellY][cellX];
        StringBuilder info = new StringBuilder(String.format("X: %d  Y: %d", cellX, cellY));
        if (idx >= 0) {
            BeadColor c = p.palette().colorAt(idx);
            info.append("  ").append(c.code());
            if (!c.name().isEmpty()) {
                info.append(" ").append(c.name());
            }
        } else {
            info.append("  (empty)");
        }
        hoverInfo.set(info.toString());
    }
}
