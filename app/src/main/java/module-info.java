module cn.navclub.nes4j.app {

    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires org.controlsfx.controls;

    requires static lombok;

    requires org.slf4j;
    requires ch.qos.logback.classic;

    requires cn.navclub.nes4j.bin;

    opens cn.navclub.nes4j.app;
    opens cn.navclub.nes4j.app.view;

    exports cn.navclub.nes4j.app;
}