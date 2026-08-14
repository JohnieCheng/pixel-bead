package com.johnie.pixelbead;

import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.ui.MainController;
import com.johnie.pixelbead.ui.panel.CountPanelController;
import com.johnie.pixelbead.ui.panel.LeftPanelController;
import com.johnie.pixelbead.ui.panel.PalettePanelController;
import com.johnie.pixelbead.ui.state.AppState;
import com.johnie.pixelbead.util.I18n;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * JavaFX application entry point.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
public class MainApplication extends Application {
    /**
     * Persisted language wins; otherwise the system locale decides.
     */
    private static AppState.Language resolveLanguage(String persisted) {
        if (persisted != null) {
            try {
                return AppState.Language.valueOf(persisted.toUpperCase());
            } catch (IllegalArgumentException e) {
                // fall through to the locale check
            }
        }
        return Locale.getDefault().getLanguage().startsWith("zh")
                ? AppState.Language.ZH
                : AppState.Language.EN;
    }

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        setDockIcon();
        // Resolve the UI language from the system locale before any FXML is
        // built, so the UI texts and enum combos resolve against the right
        // bundle. The in-app toggle changes it for the current session only.
        applyLanguage(resolveLanguage(null));
        Parent root = buildRoot();
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(MainApplication.class.getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Pixel Bead");
        stage.show();
    }

    /**
     * Applies the language to the shared bundle and state.
     */
    public static void applyLanguage(AppState.Language language) {
        AppState.get().languageProperty().set(language);
        I18n.setBundle(ResourceBundle.getBundle("i18n.messages",
                language == AppState.Language.ZH ? Locale.CHINESE : Locale.ENGLISH));
    }

    /**
     * Rebuilds the scene root with the current bundle. Swapping the root on
     * the existing scene keeps the window geometry untouched, so the layout
     * (status bar included) cannot jump or fall off-screen.
     */
    public static void reloadScene() {
        Platform.runLater(() -> {
            if (primaryStage == null) {
                return;
            }
            try {
                Parent root = buildRoot();
                Scene scene = primaryStage.getScene();
                if (scene != null) {
                    scene.setRoot(root);
                } else {
                    Scene newScene = new Scene(root);
                    newScene.getStylesheets().add(
                            MainApplication.class.getResource("/css/style.css").toExternalForm());
                    primaryStage.setScene(newScene);
                }
            } catch (IOException e) {
                // Keep the old scene alive rather than crashing the app.
            }
        });
    }

    private static Parent buildRoot() throws IOException {
        // Load the palette before any FXML controller initializes: included
        // panel controllers run before MainController.initialize.
        try {
            AppState.get().paletteProperty().set(BeadPalette.loadResource("/palettes/mard_standard.json"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load palette", e);
        }
        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("/fxml/main.fxml"));
        loader.setResources(I18n.bundle());
        // Shared controller instances: included panel controllers are created
        // here so MainController can attach shared services to them.
        MainController main = new MainController();
        LeftPanelController left = new LeftPanelController();
        PalettePanelController palette = new PalettePanelController();
        CountPanelController count = new CountPanelController();
        main.setPanels(palette, count);
        loader.setControllerFactory(type -> {
            if (type == MainController.class) {
                return main;
            }
            if (type == LeftPanelController.class) {
                return left;
            }
            if (type == PalettePanelController.class) {
                return palette;
            }
            if (type == CountPanelController.class) {
                return count;
            }
            return null;
        });
        Parent root = loader.load();
        return root;
    }

    /**
     * Sets the macOS Dock icon while running as a bare JVM (dev runs). Uses
     * reflection because {@code com.apple.eawt} only exists in the macOS JDK;
     * silently skipped on other platforms. Packaged .app bundles get the icon
     * from Info.plist instead.
     */
    private static void setDockIcon() {
        try {
            if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
                return;
            }
            // Ensure AWT is initialized before touching the dock.
            java.awt.Toolkit.getDefaultToolkit();
            BufferedImage image;
            try (var in = MainApplication.class.getResourceAsStream("/icons/pixel-bead.png")) {
                image = ImageIO.read(in);
            }
            if (image == null) {
                return;
            }
            Class<?> appClass = Class.forName("com.apple.eawt.Application");
            Object application = appClass.getMethod("getApplication").invoke(null);
            appClass.getMethod("setDockIconImage", Image.class).invoke(application, image);
        } catch (Exception ignored) {
            // com.apple.eawt is not exported to our module without
            // --add-exports java.desktop/com.apple.eawt=com.johnie.pixelbead
            // (configured in the javafx-maven-plugin); keep the default icon.
        }
    }
}
