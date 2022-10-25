package cn.navclub.nes4j.app.util;

import java.io.File;

public class StrUtil {
    /**
     *
     * 获取文件名称且移除后缀
     *
     */
    public static String getFileName(File file) {
        var name = file.getName();
        var index = name.indexOf(".");
        if (index > 0) {
            name = name.substring(0, index);
        }
        return name;
    }
}
