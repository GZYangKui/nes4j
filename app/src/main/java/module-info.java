import cn.navclub.nes4j.bin.apu.Player;

module cn.navclub.nes4j.app {
    requires static lombok;

    requires javafx.fxml;
    requires javafx.base;
    requires javafx.media;
    requires javafx.graphics;
    requires javafx.controls;
    requires cn.navclub.nes4j.bin;
    requires org.controlsfx.controls;

    requires org.slf4j;
    requires ch.qos.logback.core;
    requires ch.qos.logback.classic;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    exports cn.navclub.nes4j.app.audio to cn.navclub.nes4j.bin;

    opens cn.navclub.nes4j.app;
    opens cn.navclub.nes4j.app.view;

    opens cn.navclub.nes4j.app.event to javafx.fxml;
    opens cn.navclub.nes4j.app.control to javafx.fxml;
    opens cn.navclub.nes4j.app.dialog to javafx.fxml;

    opens cn.navclub.nes4j.app.config to com.fasterxml.jackson.databind;
    opens cn.navclub.nes4j.app.model to javafx.base, com.fasterxml.jackson.databind;
    opens cn.navclub.nes4j.app.assets;

    uses Player;
}