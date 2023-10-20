package cn.navclub.nes4j.app;

import cn.navclub.nes4j.app.config.NESConfig;
import cn.navclub.nes4j.app.util.JsonUtil;
import cn.navclub.nes4j.app.util.OSUtil;
import cn.navclub.nes4j.app.util.StrUtil;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import javafx.application.Application;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

public class Launcher {
    private static final LoggerDelegate log = LoggerFactory.logger(Launcher.class);
    private static final String DEFAULT_CONFIG_PATH = OSUtil.workstation() + "config.json";

    @Getter
    private static RandomAccessFile lockFile;

    public static void main(String[] args) throws Exception {
        if (!checkAccess()) {
            log.warning("Detected already exist application instance.");
            return;
        }
        //Register global catch thread exception
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> log.fatal("Catch target thread {} un-catch exception.", e, t.getName()));
        //Load application config
        INes.config = loadLocalConfig(args);
        //Launch application
        Application.launch(INes.class, args);
    }

    /**
     * Usage to system file lock ensure application single instance
     *
     * @return If current is single instance return {@code true} otherwise {@code false}
     * @throws IOException {@inheritDoc}
     */
    private static boolean checkAccess() throws IOException {
        lockFile = new RandomAccessFile(OSUtil.workstation() + "mutex", "rw");
        var fc = lockFile.getChannel();
        var lock = fc.tryLock();
        if (lock == null) {
            return false;
        }
        lockFile.writeBytes(Long.toString(OSUtil.pid()));
        return true;
    }


    /**
     * Load test environment config
     *
     * @param args Program args
     */
    protected static NESConfig loadLocalConfig(String[] args) throws Exception {
        var map = StrUtil.args2Map(args);
        var pathStr  = map.get("--config");
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
        
        var extraNes = map.get("--extra-nes");
        if (StrUtil.isNotBlank(extraNes)){
            config.setExtraNes(new File(extraNes));
        }
        config.setPath(path);
        return config;
    }
}
