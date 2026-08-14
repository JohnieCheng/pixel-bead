package com.johnie.pixelbead.ui;

import com.johnie.pixelbead.MainApplication;
import com.johnie.pixelbead.enums.ExportFormat;
import com.johnie.pixelbead.enums.Theme;
import com.johnie.pixelbead.ui.components.InteractiveCanvas;
import com.johnie.pixelbead.ui.coordinator.ConversionCoordinator;
import com.johnie.pixelbead.ui.coordinator.ExportCoordinator;
import com.johnie.pixelbead.ui.coordinator.ReplaceService;
import com.johnie.pixelbead.ui.panel.CountPanelController;
import com.johnie.pixelbead.ui.panel.PalettePanelController;
import com.johnie.pixelbead.ui.state.AppState;
import com.johnie.pixelbead.util.I18n;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

/**
 * Main window controller: wires the AppState to the FXML layout and drives
 * the engine conversion pipeline.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
public class MainController {

    private final AppState state = AppState.get();

    @FXML
    private Button importButton;
    @FXML
    private Button exportButton;
    @FXML
    private ComboBox<ExportFormat> formatCombo;
    @FXML
    private InteractiveCanvas canvas;
    @FXML
    private ToggleButton previewButton;
    @FXML
    private Button themeButton;
    @FXML
    private BorderPane root;
    @FXML
    private Label hoverLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label boardSizeLabel;
    @FXML
    private StackPane loadingOverlay;

    @FXML
    private Button languageButton;

    private ConversionCoordinator conversion;
    private ExportCoordinator export;
    private ReplaceService replace;

    private PalettePanelController paletteCtrl;
    private CountPanelController countCtrl;

    /**
     * Injects the included panel controllers (created via controller factory).
     */
    public void setPanels(PalettePanelController palette, CountPanelController count) {
        this.paletteCtrl = palette;
        this.countCtrl = count;
    }

    @FXML
    private void initialize() {
        languageButton.setText(state.languageProperty().get().toString());
        conversion = new ConversionCoordinator(loadingOverlay, importButton);
        export = new ExportCoordinator(canvas, loadingOverlay, exportButton, formatCombo);
        replace = new ReplaceService(canvas);
        paletteCtrl.attach(replace);
        countCtrl.attach(replace);

        conversion.setup();
        export.setup();
        setupTheme();
        setupShortcuts();
        setupStatusHint();

        canvas.projectProperty().bind(state.currentProjectProperty());
        hoverLabel.textProperty().bind(canvas.hoverInfoProperty());
        state.currentProjectProperty()
                .addListener((obs, old, project) -> {
                    if (project != null) {
                        boardSizeLabel.setText(project.board().columns() + " x " + project.board().rows());
                    }
                });

        conversion.startWithDemo();
    }

    private void setupShortcuts() {
        Platform.runLater(() -> {
            Scene scene = importButton.getScene();
            if (scene == null) {
                return;
            }
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), state::undo);
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), state::redo);
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.ESCAPE), () -> {
                        state.replaceFromIndexProperty().set(-1);
                        replace.clearPreview();
                    });
            scene.getAccelerators().put(
                    new KeyCodeCombination(KeyCode.P), () -> {
                        previewButton.setSelected(!previewButton.isSelected());
                        onTogglePreview();
                    });
        });
    }

    /**
     * Replace-mode hint follows the shared state (palette pick, canvas cancel, Esc).
     */
    private void setupStatusHint() {
        state.replaceFromIndexProperty().addListener((obs, old, idx) -> statusLabel.setText(
                idx.intValue() >= 0 ? I18n.get("status.replaceHint") : ""));
    }

    @FXML
    private void onFitView() {
        canvas.fitToView();
    }

    @FXML
    private void onToggleTheme() {
        Theme next = state.themeProperty().get() == Theme.DARK
                ? Theme.LIGHT
                : Theme.DARK;
        state.themeProperty().set(next);
    }

    @FXML
    private void onTogglePreview() {
        canvas.setPreviewMode(previewButton.isSelected());
    }

    private void setupTheme() {
        applyThemeClass();
        themeButton.setText(state.themeProperty().get() == Theme.DARK ? "Dark" : "Light");
        state.themeProperty().addListener((obs, old, theme) -> {
            applyThemeClass();
            themeButton.setText(theme == Theme.DARK ? "Dark" : "Light");
        });
    }

    private void applyThemeClass() {
        root.getStyleClass().removeAll("theme-dark", "theme-light");
        root.getStyleClass().add(state.themeProperty().get() == Theme.DARK ? "theme-dark" : "theme-light");
    }

    @FXML
    private void onImportImage() {
        conversion.importImage();
    }

    /**
     * One-click language toggle: swaps the bundle and rebuilds the scene so
     * every text refreshes immediately. Session-only, not persisted.
     */
    @FXML
    private void onToggleLanguage() {
        AppState.Language next = state.languageProperty().get() == AppState.Language.ZH
                ? AppState.Language.EN
                : AppState.Language.ZH;
        MainApplication.applyLanguage(next);
        MainApplication.reloadScene();
    }

    @FXML
    private void onExport() {
        export.export();
    }
}
