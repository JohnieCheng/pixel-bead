package com.johnie.pixelbead;

import com.johnie.pixelbead.engine.model.BeadPalette;
import com.johnie.pixelbead.ui.MainController;
import com.johnie.pixelbead.ui.panel.CountPanelController;
import com.johnie.pixelbead.ui.panel.LeftPanelController;
import com.johnie.pixelbead.ui.panel.PalettePanelController;
import com.johnie.pixelbead.ui.state.AppState;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * JavaFX application entry point.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        setDockIcon();
        // Load the palette before any FXML controller initializes: included
        // panel controllers run before MainController.initialize.
        try {
            AppState.get().paletteProperty().set(BeadPalette.loadResource("/palettes/mard_standard.json"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load palette", e);
        }
        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("/fxml/main.fxml"));
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
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(MainApplication.class.getResource("/css/style.css").toExternalForm());
        stage.setTitle("Pixel Bead");
        stage.setScene(scene);
        stage.show();
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
