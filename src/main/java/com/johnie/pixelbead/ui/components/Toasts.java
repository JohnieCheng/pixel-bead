package com.johnie.pixelbead.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Non-modal toast and modal error helpers shared across coordinators.
 *
 * @author johnie
 * @version 3.0.0
 * @since 2026/08/11
 */
public final class Toasts {

    private Toasts() {
    }

    /**
     * Fades a toast in at the bottom-right of the stack that holds the anchor.
     */
    public static void show(Node anchor, String message) {
        Node parent = anchor.getParent();
        if (!(parent instanceof StackPane stack)) {
            return;
        }
        Label toast = new Label(message);
        toast.getStyleClass().add("toast");
        stack.getChildren().add(toast);
        StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toast, new Insets(0, 16, 16, 0));
        toast.setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(200), toast);
        in.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.seconds(2.5));
        FadeTransition out = new FadeTransition(Duration.millis(300), toast);
        out.setToValue(0);
        out.setOnFinished(e -> stack.getChildren().remove(toast));
        in.setOnFinished(e -> hold.play());
        hold.setOnFinished(e -> out.play());
        in.play();
    }

    /**
     * Modal error dialog.
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
