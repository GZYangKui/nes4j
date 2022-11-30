package cn.navclub.nes4j.bin.debug;

import cn.navclub.nes4j.bin.enums.AddressMode;
import cn.navclub.nes4j.bin.enums.CPUInstruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    case Immediate -> new Operand(AddressMode.Immediate, buffer[i], (byte) 0);
                    case Accumulator -> new Operand(AddressMode.Accumulator, (byte) 0, (byte) 0);
                    case Absolute, Absolute_X, Absolute_Y, Indirect -> new Operand(mode, buffer[i], buffer[i + 1]);
                    case ZeroPage, ZeroPage_X, ZeroPage_Y, Indirect_Y, Indirect_X -> new Operand(mode, buffer[i], (byte) 0);
                    default -> Operand.DEFAULT_OPERAND;
                };

                list.add(new OpenCode(0x8000 + i - 1, instance.getInstruction(), operator));
                i += instance.getBytes() - 1;
            } catch (Exception ignore) {

            }
        }
        return list;
    }
}
