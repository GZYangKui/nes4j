package cn.navclub.nes4j.app.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class StrUtil {
    /**
     * 获取文件名称且移除后缀
     */
    public static String getFileName(File file) {
        var name = file.getName();
        var index = name.indexOf(".");
        if (index > 0) {
            name = name.substring(0, index);
        }
        return name;
    }


    public static boolean isBlank(String str) {
        return str == null || str.trim().equals("");
    }

    public static Map<String, String> args2Map(String[] args) {
        var map = new HashMap<String, String>();

        for (int i = 0; i < args.length; i += 2) {
            var key = args[i];
            var value = i >= args.length - 1 ? "" : args[i + 1];
            map.put(key, value);
        }
        return map;
    }
}
