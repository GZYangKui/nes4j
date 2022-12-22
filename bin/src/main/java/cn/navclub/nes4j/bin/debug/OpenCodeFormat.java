package cn.navclub.nes4j.bin.debug;

import cn.navclub.nes4j.bin.config.AddressMode;
import cn.navclub.nes4j.bin.config.Instruction;

import java.util.ArrayList;
import java.util.List;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;

/**
 * 6502操作码格式化
 */
public class OpenCodeFormat {

    public static List<OpenCode> formatOpenCode(byte[] buffer) {
        var list = new ArrayList<OpenCode>();
        for (int i = 0; i < buffer.length; ) {
            var b = buffer[i];
            try {
                i += 1;

                var instance = Instruction.getInstance(b);
                var mode = instance.getAddressMode();

                var operator = switch (mode) {
                    case Immediate -> new Operand(AddressMode.Immediate, buffer[i], int8(0));
                    case Accumulator -> new Operand(AddressMode.Accumulator, (byte) 0, int8(0));
                    case Absolute,
                            Absolute_X,
                            Absolute_Y,
                            Indirect -> new Operand(mode, buffer[i], buffer[i + 1]);

                    case ZeroPage,
                            ZeroPage_X,
                            ZeroPage_Y,
                            Indirect_Y,
                            Indirect_X,
                            Relative -> new Operand(mode, buffer[i], int8(0));
                    default -> Operand.DEFAULT_OPERAND;
                };
                var index = 0x8000 + i - 1;
                list.add(new OpenCode(index, instance.getInstruction(), operator));
                i += (instance.getSize() - 1);
            } catch (Exception ignore) {
            }
        }
        return list;
    }
}
