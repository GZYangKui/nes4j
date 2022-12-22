package cn.navclub.nes4j.app;

import cn.navclub.nes4j.app.config.NESConfig;
import cn.navclub.nes4j.app.util.JsonUtil;
import cn.navclub.nes4j.app.util.StrUtil;
import cn.navclub.nes4j.app.view.GameHall;
import cn.navclub.nes4j.bin.eventbus.EventBus;
import javafx.application.Application;

import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

@Slf4j
public class INes extends Application {
    public final static EventBus eventBus;
    public static final ResourceBundle RESOURCE_BUNDLE;

    protected static NESConfig config;

    static {
        eventBus = new EventBus();
        System.setProperty("java.util.PropertyResourceBundle.encoding", "UTF-8");
        RESOURCE_BUNDLE = ResourceBundle.getBundle("cn.navclub.nes4j.app.assets.language.nes4j");
    }


    @Override
    public void start(Stage stage) {
        new GameHall(stage);
    }

    public static String localeValue(String key) {
        return localeValue(key, false);
    }

    public static String localeValue(String key, boolean titleCase) {
        var value = RESOURCE_BUNDLE.getString(key);
        if (titleCase) {
            var arr = value.getBytes();
            var tb = arr[0];
            if (tb >= 97 && tb <= 122) {
                arr[0] = (byte) (tb - 32);
            }
            value = new String(arr);
        }
        return value;
    }

    public static NESConfig config() {
        return INes.config;
    }
}
