package cn.navclub.nes4j.app.util;

import java.nio.file.Files;
import java.nio.file.Path;

public class IOUtil {
    public static void writeStr(Path path, String text) {
        try {
            mkdirs(path, true);
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.writeString(path, text);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mkdirs(Path path, boolean file) {
        if (file) {
            path = path.getParent();
        }
        path.toFile().mkdirs();
    }

}
