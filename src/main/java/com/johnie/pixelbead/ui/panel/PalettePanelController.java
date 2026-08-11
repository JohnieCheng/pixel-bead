package com.johnie.pixelbead.ui.panel;

import com.johnie.pixelbead.engine.model.BeadColor;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.ui.coordinator.ReplaceService;
import com.johnie.pixelbead.ui.state.AppState;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Palette swatches: click selects the brush colour; while a replacement is
 * pending (AppState.replaceFromIndex) hovering previews and clicking executes.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/11
 */
public class PalettePanelController {

    private static final int SWATCH_SIZE = 20;

    private final AppState state = AppState.get();
    private ReplaceService replace;

    @FXML
    private FlowPane palettePane;

    public void attach(ReplaceService replace) {
        this.replace = replace;
    }

    @FXML
    private void initialize() {
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
                    replace.preview(state.replaceFromIndexProperty().get(), index);
                }
            });
            swatch.setOnMouseExited(e -> {
                if (state.replaceFromIndexProperty().get() >= 0) {
                    replace.clearPreview();
                }
            });
            swatch.setOnMouseClicked(e -> {
                if (state.replaceFromIndexProperty().get() >= 0) {
                    replace.execute(state.replaceFromIndexProperty().get(), index);
                    return;
                }
                state.selectedColorIndexProperty().set(index);
            });
            palettePane.getChildren().add(swatch);
        }
        state.selectedColorIndexProperty().addListener((obs, old, idx) -> refreshSwatchHighlight());
        refreshSwatchHighlight();
    }

    /**
     * Highlights the selected palette swatch with the accent color.
     */
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
}
