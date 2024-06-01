package cn.navclub.nes4j.bin.util.internal;

import cn.navclub.nes4j.bin.NesConsole;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Assembly custom script utils
 * <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class ScriptUtil {
    private final static Pattern STR_TEMPLATE = Pattern.compile("\\\\\\{\\w*\\.\\w*}");

    /**
     * Eval assembly code string template value
     *
     * @param text    String template value
     * @param console Nes instance
     * @return String eval value
     */
    public static String evalTStr(String text, NesConsole console) {
        var offset = 0;
        var cpu = console.getCpu();
        var sb = new StringBuilder(text);
        var matcher = STR_TEMPLATE.matcher(text);
        while (matcher.find()) {
            var i = matcher.start();
            var j = matcher.end();
            var expr = text.substring(i + 2, j - 1);
            var context = switch (expr) {
                case "c.a" -> cpu.getRa();
                case "c.x" -> cpu.getRx();
                case "c.y" -> cpu.getRy();
                default -> null;
            };
            if (Objects.isNull(context)) {
                continue;
            }
            var str = context.toString();
            sb.replace(i + offset, j + offset, str);
            offset += (str.length() - (j - i));
        }
        return sb.toString();
    }
}