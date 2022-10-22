module cn.navclub.nes4j.app {
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires org.controlsfx.controls;

    requires cn.navclub.nes4j.bin;

    opens cn.navclub.nes4j.app;
    opens cn.navclub.nes4j.app.view;
}