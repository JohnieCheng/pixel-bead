package com.johnie.pixelbead.ui.panel;

import com.johnie.pixelbead.engine.model.BeadColor;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.enums.PaletteChoice;
import com.johnie.pixelbead.ui.coordinator.ReplaceService;
import com.johnie.pixelbead.ui.state.AppState;
import com.johnie.pixelbead.util.I18n;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.IOException;

/**
 * Palette swatches: click selects the brush colour; while a replacement is
 * pending (AppState.replaceFromIndex) hovering previews and clicking executes.
 * The combo on top switches between bundled palette files.
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
    private ComboBox<PaletteChoice> paletteCombo;
    @FXML
    private FlowPane palettePane;

    public void attach(ReplaceService replace) {
        this.replace = replace;
    }

    @FXML
    private void initialize() {
        paletteCombo.setConverter(I18n.enumConverter());
        paletteCombo.getItems().addAll(PaletteChoice.values());
        paletteCombo.setValue(PaletteChoice.STANDARD_221);
        paletteCombo.valueProperty().addListener((obs, old, choice) -> {
            if (choice == null) {
                return;
            }
            try {
                state.paletteProperty().set(BeadPalette.loadResource(choice.resource()));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load palette " + choice.resource(), e);
            }
        });

        state.paletteProperty().addListener((obs, oldPalette, newPalette) -> {
            if (newPalette != null) {
                rebuildSwatches();
            }
        });
        rebuildSwatches();
    }

    /**
     * Rebuilds the swatch grid for the current palette.
     */
    private void rebuildSwatches() {
        BeadPalette palette = state.paletteProperty().get();
        palettePane.getChildren().clear();
        if (palette == null) {
            return;
        }
        for (int i = 0; i < palette.size(); i++) {
            BeadColor color = palette.colorAt(i);
            Rectangle swatch = new Rectangle(SWATCH_SIZE, SWATCH_SIZE);
            swatch.setFill(Color.rgb(color.r(), color.g(), color.b()));
            swatch.setArcWidth(4);
            swatch.setArcHeight(4);
            // Faint inset-like edge instead of a hard black border.
            swatch.setStroke(Color.rgb(0, 0, 0, 0.08));
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
        // The new palette may not contain the previously selected colour.
        state.selectedColorIndexProperty().set(0);
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
                    swatch.setStroke(Color.web("#6C5CE7"));
                    swatch.setStrokeWidth(2);
                } else {
                    swatch.setStroke(Color.rgb(0, 0, 0, 0.08));
                    swatch.setStrokeWidth(1);
                }
            }
        }
    }
}
