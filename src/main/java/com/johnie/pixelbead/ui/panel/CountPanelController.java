package com.johnie.pixelbead.ui.panel;

import com.johnie.pixelbead.engine.model.BeadColor;
import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.engine.model.PatternProject;
import com.johnie.pixelbead.engine.quantizer.ColorDifference;
import com.johnie.pixelbead.enums.Theme;
import com.johnie.pixelbead.ui.coordinator.ReplaceService;
import com.johnie.pixelbead.ui.model.BeadCountRow;
import com.johnie.pixelbead.ui.state.AppState;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Bead count table: fills itself from AppState (project + edits), highlights
 * the colour on canvas when hovering a row, and hosts the replace picker.
 *
 * @author johnie
 * @version 3.0.0
 * @since 2026/08/11
 */
public class CountPanelController {

    private final AppState state = AppState.get();
    private ReplaceService replace;

    @FXML
    private TableView<BeadCountRow> countTable;
    @FXML
    private TableColumn<BeadCountRow, String> codeColumn;
    @FXML
    private TableColumn<BeadCountRow, BeadColor> swatchColumn;
    @FXML
    private TableColumn<BeadCountRow, Integer> countColumn;

    public void attach(ReplaceService replace) {
        this.replace = replace;
    }

    @FXML
    private void initialize() {
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

        countTable.setRowFactory(tv -> {
            TableRow<BeadCountRow> row = new TableRow<>();
            row.hoverProperty().addListener((obs, was, hover) -> {
                if (hover && !row.isEmpty()) {
                    replace.highlight(row.getItem().colorIndex());
                } else if (was) {
                    replace.clearHighlight();
                }
            });
            row.setOnContextMenuRequested(e -> {
                if (row.isEmpty()) {
                    return;
                }
                showReplacePicker(row.getItem(), e.getScreenX(), e.getScreenY());
            });
            return row;
        });

        // Fill on project change and on every edit.
        Runnable fill = this::updateCountTable;
        state.currentProjectProperty().addListener(obs -> fill.run());
        state.editCountProperty().addListener(obs -> fill.run());
        updateCountTable();
    }

    private void updateCountTable() {
        PatternProject project = state.currentProjectProperty().get();
        if (project == null) {
            countTable.setItems(FXCollections.observableArrayList());
            return;
        }
        int[] counts = project.colorCounts();
        ObservableList<BeadCountRow> rows = FXCollections.observableArrayList();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                rows.add(new BeadCountRow(i, project.palette().colorAt(i), counts[i]));
            }
        }
        rows.sort((a, b) -> Integer.compare(b.count(), a.count()));
        countTable.setItems(rows);
    }

    /**
     * Popup listing similar colours (ΔE2000) plus a palette picker entry.
     */
    private void showReplacePicker(BeadCountRow source, double screenX, double screenY) {
        PatternProject project = state.currentProjectProperty().get();
        if (project == null) {
            return;
        }
        BeadPalette palette = project.palette();
        int from = source.colorIndex();

        List<Integer> similar = IntStream.range(0, palette.size())
                .filter(i -> i != from)
                .boxed()
                .sorted(Comparator.comparingDouble(
                        i -> ColorDifference.de2000(palette.colorAt(from).lab(), palette.colorAt(i).lab())))
                .limit(8)
                .toList();

        VBox box = new VBox(4);
        // Popup content lives outside the main root: carry the theme classes
        // so -pixel-* variables resolve (same pattern as CropDialog).
        box.getStyleClass().addAll("root", "replace-picker",
                state.themeProperty().get() == Theme.DARK ? "theme-dark" : "theme-light");
        Label header = new Label(source.color().code() + " \u00b7 " + source.count() + " beads");
        header.getStyleClass().add("replace-picker-header");
        box.getChildren().add(header);

        Popup popup = new Popup();
        popup.setAutoHide(true);
        for (int idx : similar) {
            box.getChildren().add(buildPickerItem(popup, palette, from, idx));
        }
        box.getChildren().add(buildPickerItem(popup, palette, from, -1));

        popup.setOnHiding(e -> {
            replace.clearHighlight();
            replace.clearPreview();
        });
        popup.getContent().add(box);
        popup.show(countTable.getScene().getWindow(), screenX, screenY);
    }

    private HBox buildPickerItem(Popup popup, BeadPalette palette, int from, int target) {
        HBox item = new HBox(8);
        item.getStyleClass().add("replace-picker-item");
        String text;
        if (target >= 0) {
            BeadColor color = palette.colorAt(target);
            Rectangle swatch = new Rectangle(14, 14);
            swatch.setFill(Color.rgb(color.r(), color.g(), color.b()));
            swatch.setStroke(Color.web("#999999"));
            swatch.setStrokeWidth(0.5);
            item.getChildren().add(swatch);
            double dE = ColorDifference.de2000(palette.colorAt(from).lab(), color.lab());
            text = color.code() + "  \u0394E " + String.format("%.1f", dE);
        } else {
            text = "Pick from palette...";
        }
        item.getChildren().add(new Label(text));

        item.setOnMouseEntered(e -> {
            replace.clearHighlight();
            if (target >= 0) {
                replace.preview(from, target);
            }
        });
        item.setOnMouseExited(e -> replace.clearPreview());
        item.setOnMouseClicked(e -> {
            popup.hide();
            if (target >= 0) {
                replace.execute(from, target);
            } else {
                state.replaceFromIndexProperty().set(from);
            }
        });
        return item;
    }
}
