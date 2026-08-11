package com.johnie.pixelbead.ui.components;

import com.johnie.pixelbead.engine.model.BeadBoard;
import com.johnie.pixelbead.engine.model.BeadColor;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.ui.state.AppState;
import javafx.animation.AnimationTimer;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
public class InteractiveCanvas extends Canvas {

    private static final double MIN_SCALE = 2.0;
    private static final double MAX_SCALE = 64.0;
    private static final double ZOOM_STEP = 1.15;

    // Theme colors (kept in sync with css/style.css): bg, cellLine, subGridLine,
    // border, emptyHint, accent.
    private static final Color[] DARK = {
            Color.web("#1E2228"), Color.web("#2B303B"), Color.web("#3A3F4B"), Color.web("#565C6A"), Color.web("#9AA2B0"), Color.web("#61AFEF")
    };
    private static final Color[] LIGHT = {
            Color.web("#F5F6F8"), Color.web("#E3E6EA"), Color.web("#C9D1D9"), Color.web("#9AA4AF"), Color.web("#59636E"), Color.web("#3B82C4")
    };

    private Color bg = DARK[0];
    private Color cellLine = DARK[1];
    private Color subGridLine = DARK[2];
    private Color border = DARK[3];
    private Color emptyHint = DARK[4];
    private Color accent = DARK[5];

    private final ObjectProperty<PatternProject> project = new SimpleObjectProperty<>();
    private final ObjectProperty<String> hoverInfo = new SimpleObjectProperty<>("");

    private double scale = 12.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private boolean panning = false;
    private double lastX;
    private double lastY;
    private boolean spaceDown = false;
    private boolean strokeStarted = false;

    private final AppState state = AppState.get();

    /**
     * Palette index whose cells pulse-highlight when hovering the count table.
     */
    private final IntegerProperty highlightIndex = new SimpleIntegerProperty(-1);
    /**
     * Replacement preview: cells of fromIndex shown as toIndex while picking.
     */
    private final IntegerProperty previewFrom = new SimpleIntegerProperty(-1);
    private final IntegerProperty previewTo = new SimpleIntegerProperty(-1);
    private long animStart = -1;

    /**
     * When true, grid lines and border are hidden for a finished-bead look.
     */
    private boolean previewMode = false;

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
        // Grid edits repaint in place; the viewport must not re-fit.
        state.editCountProperty().addListener(obs -> redraw());
        // Theme switch repaints the canvas with the matching palette.
        state.themeProperty().addListener((obs, old, theme) -> applyTheme(theme));
        // Pulse animation while any highlight/preview is active.
        AnimationTimer animator = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (highlightIndex.get() < 0 && previewFrom.get() < 0) {
                    stop();
                    animStart = -1;
                    // Repaint once more so the last highlighted frame is cleared.
                    redraw();
                    return;
                }
                if (animStart < 0) {
                    animStart = now;
                }
                redraw();
            }
        };
        highlightIndex.addListener(obs -> {
            if (highlightIndex.get() >= 0) {
                animator.start();
            }
        });
        previewFrom.addListener(obs -> {
            if (previewFrom.get() >= 0) {
                animator.start();
            }
        });
        applyTheme(state.themeProperty().get());
    }

    private void applyTheme(AppState.Theme theme) {
        Color[] palette = theme == AppState.Theme.LIGHT ? LIGHT : DARK;
        bg = palette[0];
        cellLine = palette[1];
        subGridLine = palette[2];
        border = palette[3];
        emptyHint = palette[4];
        accent = palette[5];
        redraw();
    }

    /**
     * Re-fits on every size change so the pattern follows window resizes live.
     */
    private void onSizeChanged() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        if (project.get() != null) {
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

    /**
     * Canvas reports its current size as its layout min/pref (Node default),
     * which would pin the parent container's minimum size to the viewport.
     * Report 0 so the window can shrink freely; the canvas size is bound to
     * the parent region instead of relying on layout.
     */
    @Override
    public double minWidth(double height) {
        return 0;
    }

    @Override
    public double minHeight(double width) {
        return 0;
    }

    @Override
    public double prefWidth(double height) {
        return 0;
    }

    @Override
    public double prefHeight(double width) {
        return 0;
    }

    /**
     * Test hook: current pixels-per-cell scale.
     */
    double getScaleForTest() {
        return scale;
    }

    /**
     * Centers the pattern in the viewport at the largest fitting scale.
     */
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
        redraw();
    }

    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(bg);
        gc.fillRect(0, 0, w, h);

        PatternProject p = project.get();
        if (p == null) {
            gc.setFill(emptyHint);
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

        // grid lines
        if (!previewMode) {
            gc.setStroke(cellLine);
            gc.setLineWidth(0.5);
            for (int x = x0; x <= x1; x++) {
                double px = offsetX + x * scale;
                gc.strokeLine(px, 0, px, h);
            }
            for (int y = y0; y <= y1; y++) {
                double py = offsetY + y * scale;
                gc.strokeLine(0, py, w, py);
            }
        }

        // Bold sub-grid lines.
        if (!previewMode) {
            int interval = board.subGridInterval();
            gc.setStroke(subGridLine);
            gc.setLineWidth(1.2);
            for (int x = 0; x <= cols; x += interval) {
                double px = offsetX + x * scale;
                gc.strokeLine(px, 0, px, h);
            }
            for (int y = 0; y <= rows; y += interval) {
                double py = offsetY + y * scale;
                gc.strokeLine(0, py, w, py);
            }
        }

        // outer border
        if (!previewMode) {
            gc.setStroke(border);
            gc.setLineWidth(1.5);
            gc.strokeRect(offsetX, offsetY, cols * scale, rows * scale);
        }

        drawHighlightOverlay(gc, p);
    }

    /**
     * Paints pulse-highlight and replacement preview overlays on top of the grid.
     */
    private void drawHighlightOverlay(GraphicsContext gc, PatternProject p) {
        int hl = highlightIndex.get();
        int pf = previewFrom.get();
        int pt = previewTo.get();
        if (hl < 0 && pf < 0) {
            return;
        }
        // 4s breathing cycle: brighten/fade once per four seconds.
        double pulse = 0.5 + 0.5 * Math.sin(Math.PI * 2 * (System.nanoTime() - animStart) / 4_000_000_000.0);
        BeadPalette palette = p.palette();
        int[][] grid = p.grid();
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                int idx = grid[y][x];
                if (idx < 0) {
                    continue;
                }
                double px = offsetX + x * scale;
                double py = offsetY + y * scale;
                if (idx == pf && pt >= 0 && pt < palette.size()) {
                    BeadColor target = palette.colorAt(pt);
                    gc.setFill(Color.color(target.r() / 255.0, target.g() / 255.0, target.b() / 255.0,
                            0.55 + 0.4 * pulse));
                    gc.fillRect(px, py, scale, scale);
                }
                if (idx == hl) {
                    gc.setStroke(accent);
                    gc.setLineWidth(1.5 + 1.0 * pulse);
                    gc.strokeRect(px - 1, py - 1, scale + 2, scale + 2);
                }
            }
        }
    }

    /**
     * Toggles finished-bead preview (grid lines hidden).
     */
    public void setPreviewMode(boolean preview) {
        if (previewMode != preview) {
            previewMode = preview;
            redraw();
        }
    }

    /**
     * Pulse-highlights every cell of the given palette index (-1 clears).
     */
    public void setHighlight(int paletteIndex) {
        highlightIndex.set(paletteIndex);
    }

    public void clearHighlight() {
        highlightIndex.set(-1);
    }

    /**
     * Previews replacing cells of fromIndex with toIndex (-1 to clear).
     */
    public void setReplacePreview(int fromIndex, int toIndex) {
        previewFrom.set(fromIndex);
        previewTo.set(toIndex);
    }

    public void clearReplacePreview() {
        previewFrom.set(-1);
        previewTo.set(-1);
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
        } else if (e.isPrimaryButtonDown()) {
            // New edit stroke: push the pre-edit snapshot on the first mutation.
            strokeStarted = false;
            applyTool(e);
        }
    }

    private void handleDragged(MouseEvent e) {
        if (panning) {
            offsetX += e.getX() - lastX;
            offsetY += e.getY() - lastY;
            lastX = e.getX();
            lastY = e.getY();
            redraw();
        } else if (e.isPrimaryButtonDown()) {
            applyTool(e);
        }
    }

    /**
     * Applies the active tool at the cell under the cursor.
     */
    private void applyTool(MouseEvent e) {
        if (previewFrom.get() >= 0) {
            // Replace-target picking: clicking the canvas cancels instead of
            // painting over the pattern.
            clearReplacePreview();
            state.replaceFromIndexProperty().set(-1);
            return;
        }
        PatternProject p = project.get();
        if (p == null) {
            return;
        }
        int cellX = (int) Math.floor((e.getX() - offsetX) / scale);
        int cellY = (int) Math.floor((e.getY() - offsetY) / scale);
        if (cellX < 0 || cellY < 0 || cellX >= p.board().columns() || cellY >= p.board().rows()) {
            return;
        }
        switch (state.activeToolProperty().get()) {
            case BRUSH -> {
                int idx = state.selectedColorIndexProperty().get();
                if (idx < 0 || idx >= p.palette().size()) {
                    return;
                }
                if (p.cell(cellX, cellY) != idx) {
                    beginStroke(p);
                    p.setCell(cellX, cellY, idx);
                    state.editCountProperty().set(state.editCountProperty().get() + 1);
                }
            }
            case ERASER -> {
                if (p.cell(cellX, cellY) != -1) {
                    beginStroke(p);
                    p.setCell(cellX, cellY, -1);
                    state.editCountProperty().set(state.editCountProperty().get() + 1);
                }
            }
            case EYEDROPPER -> {
                int idx = p.cell(cellX, cellY);
                if (idx >= 0) {
                    state.selectedColorIndexProperty().set(idx);
                }
            }
        }
    }

    private void beginStroke(PatternProject p) {
        if (!strokeStarted) {
            strokeStarted = true;
            state.editHistory().push(p.grid());
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
