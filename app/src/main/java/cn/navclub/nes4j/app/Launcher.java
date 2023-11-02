package cn.navclub.nes4j.app;

import cn.navclub.nes4j.app.config.NESConfig;
import cn.navclub.nes4j.app.util.OSUtil;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import javafx.application.Application;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Launcher {
    private static final LoggerDelegate log = LoggerFactory.logger(Launcher.class);

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
        //Default load the last extra game rom
        var length = args.length;
        if (length > 0) {
            NESConfig.getInstance().setExtraNes(new File(args[length - 1]));
        }
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
        lockFile = new RandomAccessFile(OSUtil.workstation("process") + "mutex", "rw");
        var fc = lockFile.getChannel();
        var lock = fc.tryLock();
        if (lock == null) {
            return false;
        }
        lockFile.writeBytes(Long.toString(OSUtil.pid()));
        return true;
    }
}
