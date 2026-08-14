/**
 * JPMS module descriptor: dependencies, FXML reflection opens, exports.
 *
 * @author johnie
 * @version 2.0.0
 * @since 2026/08/10
 */
module com.johnie.pixelbead {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;
    requires org.apache.pdfbox;
    requires org.apache.fontbox;
    requires org.apache.commons.logging;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.fontawesome5;

    uses org.kordamp.ikonli.IkonProvider;

    opens com.johnie.pixelbead to javafx.fxml;
    opens com.johnie.pixelbead.ui to javafx.fxml;
    opens com.johnie.pixelbead.ui.panel to javafx.fxml;

    exports com.johnie.pixelbead;
    exports com.johnie.pixelbead.ui.components;
    exports com.johnie.pixelbead.ui.model;
    exports com.johnie.pixelbead.ui.panel;
    exports com.johnie.pixelbead.ui.state;
}