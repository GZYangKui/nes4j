package cn.navclub.nes4j.bin.debug;

import cn.navclub.nes4j.bin.enums.CPUInstruction;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * 6502操作码格式化
 *
 */
public class OpenCodeFormat {
    public static List<OpenCode> formatOpenCode(byte[] buffer) {
        var list = new ArrayList<OpenCode>();
        for (int i = 0; i < buffer.length; ) {
            var b = buffer[i];
            try {
                i += 1;
                var instance = CPUInstruction.getInstance(b);
                var mode = instance.getAddressMode();
                var operator = switch (mode) {
                    case Accumulator -> "register a";
                    case Immediate -> String.format("#$%s", toHexStr(buffer[i]));
                    case Absolute -> String.format("$%s,$%s", toHexStr(buffer[i]), toHexStr(buffer[i + 1]));
                    default -> "";
                };

                list.add(new OpenCode(0x8000 + i - 1, instance.getInstruction(), operator));
                i += instance.getBytes() - 1;
            } catch (Exception ignore) {

            }
        }
        return list;
    }

    private static String toHexStr(byte b) {
        var hex = Integer.toHexString(b & 0xff);
        if (hex.length() == 1) {
            hex = String.format("0%s", hex);
        }
        return hex;
    }
}
