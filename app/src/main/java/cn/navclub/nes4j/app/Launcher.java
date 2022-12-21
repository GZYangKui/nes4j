package cn.navclub.nes4j.app;

import cn.navclub.nes4j.app.config.NESConfig;
import cn.navclub.nes4j.app.util.JsonUtil;
import cn.navclub.nes4j.app.util.StrUtil;
import javafx.application.Application;

import java.nio.file.Files;
import java.nio.file.Path;

public class Launcher {
    private static final String DEFAULT_CONFIG_PATH = "config/config.json";

    public static void main(String[] args) throws Exception {
        INes.config = loadLocalConfig(args);
        Application.launch(INes.class, args);
    }


    /**
     * 加载本地配置文件
     *
     * @param args 程序启动参数
     */
    protected static NESConfig loadLocalConfig(String[] args) throws Exception {
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
}
