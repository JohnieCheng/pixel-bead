package com.johnie.pixelbead.ui.dialogs;

import com.johnie.pixelbead.ui.state.AppState;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Modal crop dialog: lets the user select a rectangular region of the source
 * image before conversion. The selection can be dragged, resized via the four
 * corner handles, or locked to a 1:1 aspect ratio.
 */
public final class CropDialog {

    private static final double MAX_W = 880;
    private static final double MAX_H = 540;
    private static final double HANDLE = 8;
    private static final Color SELECT_FILL = Color.rgb(97, 175, 239, 0.18);
    private static final Color SELECT_STROKE = Color.rgb(97, 175, 239);
    private static final Color DIM = Color.rgb(0, 0, 0, 0.35);

    private enum Mode {
        NONE, CREATE, MOVE, NW, NE, SW, SE, TOP, BOTTOM, LEFT, RIGHT
    }

    private CropDialog() {
    }

    /**
     * Shows the crop dialog; returns the cropped sub-image, or empty when
     * the user cancels.
     */
    public static Optional<BufferedImage> show(BufferedImage source, Window owner) throws IOException {
        double srcW = source.getWidth();
        double srcH = source.getHeight();
        double scale = Math.min(MAX_W / srcW, MAX_H / srcH);
        if (scale > 1) {
            scale = 1; // never upscale the preview
        }
        double dispW = srcW * scale;
        double dispH = srcH * scale;

        // Preview only: downscale large sources to the display size so the
        // temp PNG encodes/decodes in milliseconds. The crop coordinates are
        // mapped back to the full-resolution source by ratio, so precision is
        // unaffected.
        BufferedImage preview;
        if (scale < 1) {
            preview = new BufferedImage((int) Math.round(dispW), (int) Math.round(dispH), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = preview.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, preview.getWidth(), preview.getHeight(), null);
            g.dispose();
        } else {
            preview = source;
        }

        File tmp = File.createTempFile("pixelbead-crop", ".png");
        tmp.deleteOnExit();
        ImageIO.write(preview, "png", tmp);
        Image fxImage = new Image(tmp.toURI().toString());

        ImageView imageView = new ImageView(fxImage);
        imageView.setLayoutX(0);
        imageView.setLayoutY(0);
        // Fit the preview to the computed display size; without this the
        // ImageView renders at full pixel size and overflows the canvas.
        imageView.setFitWidth(dispW);
        imageView.setFitHeight(dispH);
        imageView.setPreserveRatio(true);

        Pane canvas = new Pane();
        canvas.setPrefSize(dispW, dispH);
        canvas.setMinSize(dispW, dispH);
        canvas.setMaxSize(dispW, dispH);
        canvas.getChildren().add(imageView);

        // Selection state in preview coordinates.
        double[] sel = {0, 0, dispW, dispH};

        Rectangle[] dims = new Rectangle[4];
        for (int i = 0; i < 4; i++) {
            dims[i] = new Rectangle(0, 0, Color.TRANSPARENT);
            dims[i].setFill(DIM);
            canvas.getChildren().add(dims[i]);
        }
        Rectangle selection = new Rectangle(0, 0, dispW, dispH);
        selection.setFill(SELECT_FILL);
        selection.setStroke(SELECT_STROKE);
        selection.setStrokeWidth(1.5);
        canvas.getChildren().add(selection);

        Rectangle[] handles = new Rectangle[4];
        for (int i = 0; i < 4; i++) {
            handles[i] = new Rectangle(HANDLE, HANDLE);
            handles[i].setFill(Color.WHITE);
            handles[i].setStroke(SELECT_STROKE);
            handles[i].setStrokeWidth(1);
            canvas.getChildren().add(handles[i]);
        }

        Runnable layout = () -> {
            double x = sel[0];
            double y = sel[1];
            double w = sel[2];
            double h = sel[3];
            dims[0].setX(0);
            dims[0].setY(0);
            dims[0].setWidth(dispW);
            dims[0].setHeight(y);
            dims[1].setX(0);
            dims[1].setY(y);
            dims[1].setWidth(x);
            dims[1].setHeight(h);
            dims[2].setX(x + w);
            dims[2].setY(y);
            dims[2].setWidth(dispW - x - w);
            dims[2].setHeight(h);
            dims[3].setX(0);
            dims[3].setY(y + h);
            dims[3].setWidth(dispW);
            dims[3].setHeight(dispH - y - h);
            selection.setX(x);
            selection.setY(y);
            selection.setWidth(w);
            selection.setHeight(h);
            double hw = HANDLE / 2;
            handles[0].setX(x - hw);
            handles[0].setY(y - hw);
            handles[1].setX(x + w - hw);
            handles[1].setY(y - hw);
            handles[2].setX(x - hw);
            handles[2].setY(y + h - hw);
            handles[3].setX(x + w - hw);
            handles[3].setY(y + h - hw);
        };
        layout.run();

        ToggleButton lock = new ToggleButton("Lock 1:1");
        // Locking immediately snaps the selection to the largest centred
        // square inside the canvas; dragging afterwards keeps the ratio.
        lock.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                double size = Math.min(dispW, dispH);
                sel[0] = (dispW - size) / 2;
                sel[1] = (dispH - size) / 2;
                sel[2] = size;
                sel[3] = size;
                layout.run();
            }
        });
        Button cropButton = new Button("Crop");
        cropButton.setDefaultButton(true);
        Button cancelButton = new Button("Cancel");
        HBox buttons = new HBox(8, lock, new Region(), cropButton, cancelButton);
        buttons.setPadding(new Insets(10, 12, 12, 12));
        HBox.setHgrow(buttons.getChildren().get(1), Priority.ALWAYS);

        VBox root = new VBox(canvas, buttons);
        root.setFillWidth(false);
        // Carry the theme CSS classes so -pixel-* variables resolve in this
        // dialog's own scene (it is not a child of the main window's root).
        AppState state = AppState.get();
        root.getStyleClass().addAll("root", "crop-dialog",
                state.themeProperty().get() == AppState.Theme.DARK ? "theme-dark" : "theme-light");

        Mode[] mode = {Mode.NONE};
        double[] anchor = {0, 0};
        double[] press = {0, 0};

        java.util.function.BiFunction<Double, Double, Mode> hitTest = (mx, my) -> {
            double x = sel[0];
            double y = sel[1];
            double w = sel[2];
            double h = sel[3];
            double hw = HANDLE;
            if (mx >= x - hw && mx <= x + hw && my >= y - hw && my <= y + hw) {
                return Mode.NW;
            }
            if (mx >= x + w - hw && mx <= x + w + hw && my >= y - hw && my <= y + hw) {
                return Mode.NE;
            }
            if (mx >= x - hw && mx <= x + hw && my >= y + h - hw && my <= y + h + hw) {
                return Mode.SW;
            }
            if (mx >= x + w - hw && mx <= x + w + hw && my >= y + h - hw && my <= y + h + hw) {
                return Mode.SE;
            }
            if (Math.abs(my - y) <= hw / 2 && mx >= x && mx <= x + w) {
                return Mode.TOP;
            }
            if (Math.abs(my - (y + h)) <= hw / 2 && mx >= x && mx <= x + w) {
                return Mode.BOTTOM;
            }
            if (Math.abs(mx - x) <= hw / 2 && my >= y && my <= y + h) {
                return Mode.LEFT;
            }
            if (Math.abs(mx - (x + w)) <= hw / 2 && my >= y && my <= y + h) {
                return Mode.RIGHT;
            }
            if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
                return Mode.MOVE;
            }
            return Mode.CREATE;
        };

        java.util.function.Function<Mode, javafx.scene.Cursor> cursorFor = m -> switch (m) {
            case NW, SE -> javafx.scene.Cursor.NW_RESIZE;
            case NE, SW -> javafx.scene.Cursor.NE_RESIZE;
            case TOP, BOTTOM -> javafx.scene.Cursor.N_RESIZE;
            case LEFT, RIGHT -> javafx.scene.Cursor.E_RESIZE;
            case MOVE -> javafx.scene.Cursor.MOVE;
            default -> javafx.scene.Cursor.DEFAULT;
        };

        canvas.setOnMouseMoved(e ->
                canvas.setCursor(cursorFor.apply(hitTest.apply(e.getX(), e.getY()))));

        canvas.setOnMousePressed(e -> {
            double mx = e.getX();
            double my = e.getY();
            double x = sel[0];
            double y = sel[1];
            double w = sel[2];
            double h = sel[3];
            Mode hit = hitTest.apply(mx, my);
            mode[0] = hit;
            canvas.setCursor(cursorFor.apply(hit));
            switch (hit) {
                case NW -> {
                    anchor[0] = x + w;
                    anchor[1] = y + h;
                }
                case NE -> {
                    anchor[0] = x;
                    anchor[1] = y + h;
                }
                case SW -> {
                    anchor[0] = x + w;
                    anchor[1] = y;
                }
                case SE -> {
                    anchor[0] = x;
                    anchor[1] = y;
                }
                case TOP -> {
                    anchor[0] = x;
                    anchor[1] = y + h;
                }
                case BOTTOM -> {
                    anchor[0] = x;
                    anchor[1] = y;
                }
                case LEFT -> {
                    anchor[0] = x + w;
                    anchor[1] = y;
                }
                case RIGHT -> {
                    anchor[0] = x;
                    anchor[1] = y;
                }
                case MOVE -> {
                    press[0] = mx - x;
                    press[1] = my - y;
                }
                case CREATE -> {
                    anchor[0] = mx;
                    anchor[1] = my;
                    sel[0] = mx;
                    sel[1] = my;
                    sel[2] = 0;
                    sel[3] = 0;
                    layout.run();
                }
                default -> {
                }
            }
            e.consume();
        });

        canvas.setOnMouseDragged(e -> {
            if (mode[0] == Mode.NONE) {
                return;
            }
            canvas.setCursor(cursorFor.apply(mode[0]));
            double mx = Math.max(0, Math.min(dispW, e.getX()));
            double my = Math.max(0, Math.min(dispH, e.getY()));
            boolean locked = lock.isSelected();
            double aX = anchor[0];
            double aY = anchor[1];
            switch (mode[0]) {
                case MOVE -> {
                    double x = mx - press[0];
                    double y = my - press[1];
                    x = Math.max(0, Math.min(dispW - sel[2], x));
                    y = Math.max(0, Math.min(dispH - sel[3], y));
                    sel[0] = x;
                    sel[1] = y;
                }
                case TOP -> {
                    double maxH = locked
                            ? Math.min(dispH - sel[1], dispW)
                            : dispH - sel[1];
                    double newH = Math.max(1, Math.min(maxH, anchor[1] - my));
                    if (locked) {
                        double cx = sel[0] + sel[2] / 2;
                        sel[2] = newH;
                        sel[0] = Math.max(0, Math.min(dispW - sel[2], cx - sel[2] / 2));
                    }
                    sel[3] = newH;
                }
                case BOTTOM -> {
                    double maxH = locked
                            ? Math.min(dispH - anchor[1], dispW)
                            : dispH - anchor[1];
                    double newH = Math.max(1, Math.min(maxH, my - anchor[1]));
                    if (locked) {
                        double cx = sel[0] + sel[2] / 2;
                        sel[2] = newH;
                        sel[0] = Math.max(0, Math.min(dispW - sel[2], cx - sel[2] / 2));
                    }
                    sel[3] = newH;
                }
                case LEFT -> {
                    double maxW = locked
                            ? Math.min(dispW, dispH)
                            : dispW;
                    double newW = Math.max(1, Math.min(maxW, anchor[0] - mx));
                    if (locked) {
                        double cy = sel[1] + sel[3] / 2;
                        sel[3] = newW;
                        sel[1] = Math.max(0, Math.min(dispH - sel[3], cy - sel[3] / 2));
                    }
                    sel[0] = anchor[0] - newW;
                    sel[2] = newW;
                }
                case RIGHT -> {
                    double maxW = locked
                            ? Math.min(dispW - anchor[0], dispH)
                            : dispW - anchor[0];
                    double newW = Math.max(1, Math.min(maxW, mx - anchor[0]));
                    if (locked) {
                        double cy = sel[1] + sel[3] / 2;
                        sel[3] = newW;
                        sel[1] = Math.max(0, Math.min(dispH - sel[3], cy - sel[3] / 2));
                    }
                    sel[2] = newW;
                }
                case CREATE, NW, NE, SW, SE -> {
                    double dx = mx - aX;
                    double dy = my - aY;
                    if (locked) {
                        double size = Math.max(Math.abs(dx), Math.abs(dy));
                        dx = Math.copySign(size, dx);
                        dy = Math.copySign(size, dy);
                    }
                    double x = Math.min(aX, aX + dx);
                    double y = Math.min(aY, aY + dy);
                    double w = Math.abs(dx);
                    double h = Math.abs(dy);
                    x = Math.max(0, x);
                    y = Math.max(0, y);
                    double maxW = dispW - x;
                    double maxH = dispH - y;
                    if (locked) {
                        // Keep the square but never exceed the canvas bounds:
                        // shrink both sides once either hits its limit.
                        double size = Math.min(w, Math.min(maxW, maxH));
                        w = size;
                        h = size;
                    } else {
                        w = Math.min(maxW, w);
                        h = Math.min(maxH, h);
                    }
                    if (w < 1) {
                        w = 1;
                    }
                    if (h < 1) {
                        h = 1;
                    }
                    sel[0] = x;
                    sel[1] = y;
                    sel[2] = w;
                    sel[3] = h;
                }
                default -> {
                }
            }
            layout.run();
            e.consume();
        });

        canvas.setOnMouseReleased(e -> {
            mode[0] = Mode.NONE;
            canvas.setCursor(cursorFor.apply(hitTest.apply(e.getX(), e.getY())));
        });

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Crop Image");
        Scene scene = new Scene(root);
        scene.getStylesheets().add(CropDialog.class.getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setResizable(false);

        BufferedImage[] result = new BufferedImage[1];
        cropButton.setOnAction(e -> {
            int x0 = (int) Math.round(sel[0] / dispW * srcW);
            int y0 = (int) Math.round(sel[1] / dispH * srcH);
            int w0 = (int) Math.round(sel[2] / dispW * srcW);
            int h0 = (int) Math.round(sel[3] / dispH * srcH);
            x0 = Math.max(0, Math.min((int) srcW - 1, x0));
            y0 = Math.max(0, Math.min((int) srcH - 1, y0));
            w0 = Math.max(1, Math.min((int) srcW - x0, w0));
            h0 = Math.max(1, Math.min((int) srcH - y0, h0));
            result[0] = source.getSubimage(x0, y0, w0, h0);
            stage.close();
        });
        cancelButton.setOnAction(e -> stage.close());

        stage.showAndWait();
        return Optional.ofNullable(result[0]);
    }
}
