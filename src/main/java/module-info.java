module com.hell.osdemo {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens com.hell.osdemo to javafx.fxml;
    exports com.hell.osdemo;
}