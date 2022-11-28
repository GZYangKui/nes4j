module cn.navclub.nes4j.app {
    requires static lombok;

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

    opens cn.navclub.nes4j.app;
    opens cn.navclub.nes4j.app.view;

    opens cn.navclub.nes4j.app.config to com.fasterxml.jackson.databind;
    opens cn.navclub.nes4j.app.model to javafx.base, com.fasterxml.jackson.databind;

    uses cn.navclub.nes4j.bin.Player;
    provides cn.navclub.nes4j.bin.Player with cn.navclub.nes4j.app.audio.NativePlayer;
}