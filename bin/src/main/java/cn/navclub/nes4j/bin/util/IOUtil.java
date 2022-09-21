package cn.navclub.nes4j.bin.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class IOUtil {
    public static byte[] readFileAllByte(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
