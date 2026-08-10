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

    opens com.johnie.pixelbead to javafx.fxml;
    opens com.johnie.pixelbead.ui to javafx.fxml;
    exports com.johnie.pixelbead;
    exports com.johnie.pixelbead.ui.components;
}