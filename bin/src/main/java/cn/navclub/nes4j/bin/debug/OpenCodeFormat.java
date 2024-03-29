package cn.navclub.nes4j.bin.debug;

import cn.navclub.nes4j.bin.config.AddressMode;
import cn.navclub.nes4j.bin.config.Instruction;
import cn.navclub.nes4j.bin.core.CPU;

import java.util.ArrayList;
import java.util.List;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;

/**
 * 6502 open code format utils
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class OpenCodeFormat {

    public static List<OpenCode> formatOpenCode(byte[] buffer) {
        var list = new ArrayList<OpenCode>();
        for (int i = 0; i < buffer.length; ) {
            var b = buffer[i];

            i += 1;

            var instance = CPU.IS6502Get(b);
            if (instance != null) {
                var mode = instance.addrMode();

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
                list.add(new OpenCode(index, instance.instruction(), operator));
                i += (instance.size() - 1);
            } else {
                list.add(new OpenCode(0x8000 + i - 1, null, null));

            }
        }
        return list;
    }
}
