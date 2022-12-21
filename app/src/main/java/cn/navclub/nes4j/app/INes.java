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
    public static final ResourceBundle RESOURCE_BUNDLE;
    private static final String DEFAULT_CONFIG_PATH = "config/config.json";

    public static NESConfig config;
    public static EventBus eventBus;


    static {
        eventBus = new EventBus();
        System.loadLibrary("nes4j");
        System.setProperty("java.util.PropertyResourceBundle.encoding", "UTF-8");
        RESOURCE_BUNDLE = ResourceBundle.getBundle("cn.navclub.nes4j.app.assets.language.nes4j");
    }


    @Override
    public void start(Stage stage) {
        new GameHall(stage);
    }

    /**
     * 加载本地配置文件
     */
    private static NESConfig loadLocalConfig(String[] args) throws Exception {
        var map = StrUtil.args2Map(args);
        var pathStr = map.get("--config");
        if (!StrUtil.isBlank(pathStr)) {
            var exist = Files.exists(Path.of(pathStr));
            if (!exist) {
                throw new RuntimeException("Target config file " + pathStr + " not found.");
            }
        } else {
            pathStr = DEFAULT_CONFIG_PATH;
        }
        var path = Path.of(pathStr);
        final NESConfig config;
        if (Files.exists(path)) {
            var jsonStr = Files.readString(path);
            config = JsonUtil.parse(jsonStr, NESConfig.class);
        } else {
            config = new NESConfig();
        }
        config.setPath(path);
        return config;
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

    public static void main(String[] args) throws Exception {
        config = loadLocalConfig(args);
        launch(args);
    }
}