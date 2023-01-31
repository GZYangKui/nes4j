package cn.navclub.nes4j.app.util;

import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OSUtil {
    private static final LoggerDelegate log = LoggerFactory.logger(OSUtil.class);

    public static Optional<File> chooseFile(Window owner, String describe, String... extension) {
        var chooser = new FileChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter(describe, "*.nes", "*.NES"));
        return Optional.ofNullable(chooser.showOpenDialog(owner));
    }


    public static String userHome() {
        return System.getProperty("user.home");
    }

    public static String userHome(String subdir) {
        var path = userHome();
        if (!path.endsWith(File.separator) && !(subdir.startsWith(File.separator) || subdir.startsWith("/"))) {
            path = path + File.separator;
        }
        return String.format("%s%s", path, subdir);
    }

    public static String workstation() {
        return workstation(null);
    }

    public static String workstation(String subdir) {
        final File file;
        if (StrUtil.isNotBlank(subdir)) {
            file = new File(String.format("%s%s%s", userHome("nes4j"), File.separator, subdir));
        } else {
            file = new File(userHome("nes4j"));
        }
        if (!file.exists()) {
            var ok = file.mkdirs();
            if (!ok) {
                log.warning("Create work path:{} fail.", file.getAbsolutePath());
            }
        }
        return file.getAbsolutePath() + File.separator;
    }

    public static Optional<File> mkdirAssort(String assort) {
        var path = workstation("rom");
        var str = String.format("%s%s", path, assort);
        var file = new File(str);
        var ok = file.mkdir();
        if (log.isDebugEnabled()) {
            log.debug("Create target assort:{} result:{}", file.getAbsolutePath(), ok);
        }
        if (!ok) {
            file = null;
        }
        return Optional.ofNullable(file);
    }

    /**
     * Batch move file to dst
     *
     * @param dst  Move dest
     * @param list Wait move file list
     * @return Success move to dest file list
     */
    public static List<File> copy(File dst, List<File> list) {
        var l0 = new ArrayList<File>();
        for (File file : list) {
            try {
                var path = Files.copy(file.toPath(), Path.of(dst.toString(), file.getName()), StandardCopyOption.COPY_ATTRIBUTES);
                if (log.isDebugEnabled()) {
                    log.debug("Success copy target file [{}] to [{}]", file.getAbsolutePath(), path);
                }
                l0.add(file);
            } catch (Exception e) {
                log.fatal("Copy file from [{}] to [{}] happen error.", e, file.toString(), dst.toString());
            }
        }
        return l0;
    }
}
