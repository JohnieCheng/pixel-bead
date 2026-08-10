package com.johnie.pixelbead;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * JavaFX application entry point.
 */
public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        setDockIcon();
        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("/fxml/main.fxml"));
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
            java.awt.Toolkit.getDefaultToolkit(); // ensure AWT is initialized
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
