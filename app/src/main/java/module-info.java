module cn.navclub.nes4j.app {
    requires static lombok;


    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires org.controlsfx.controls;

    requires cn.navclub.nes4j.bin;

    requires org.slf4j;
    requires ch.qos.logback.core;
    requires ch.qos.logback.classic;

    opens cn.navclub.nes4j.app;
    opens cn.navclub.nes4j.app.view;
}