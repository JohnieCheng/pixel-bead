package com.johnie.pixelbead;

import javafx.application.Application;

/**
 * Non-modular entry point used by the packaged app (jpackage).
 *
 * @author johnie
 * @version 3.0.0
 * @since 2026/08/10
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(MainApplication.class, args);
    }
}
