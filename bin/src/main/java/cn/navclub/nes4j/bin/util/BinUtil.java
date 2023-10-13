package cn.navclub.nes4j.bin.util;

import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;

import java.io.*;

/**
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class BinUtil {
    private static final LoggerDelegate log = LoggerFactory.logger(BinUtil.class);

    public static byte int8(int value) {
        return (byte) value;
    }

    /**
     * Byte transform binary string
     */
    public static String toBinStr(byte value) {
        var sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append((value & (1 << i)) != 0 ? 1 : 0);
        }
        return sb.reverse().toString();
    }

    /**
     * Byte array transform to int
     */
    public static int toInt(byte[] arr) {
        if (arr.length < 4) {
            throw new RuntimeException("Byte array required four length.");
        }
        return arr[0] | arr[1] << 8 | arr[2] << 16 | arr[3] << 24;
    }

    public static String toHexStr(byte b) {
        var hex = Integer.toHexString(b & 0xff);
        if (hex.length() == 1) {
            hex = String.format("0%s", hex);
        }
        return hex;
    }

    public static int uint8(int b) {
        return (b & 0xff);
    }


    public static int uint16(int i) {
        return i & 0xffff;
    }

    /**
     * {@link BinUtil#snapshot(File, int, byte[], int, int)} util method
     */
    public static void snapshot(String file, int row, byte[] src, int offset, int length) {
        snapshot(new File(file), row, src, offset, length);
    }

    /**
     * Output target byte array to target file
     *
     * @param file   Target output file
     * @param row    Each row show how many hex
     * @param src    Target byte array
     * @param offset Target byte array offset
     * @param length Target byte array output length
     */
    public static void snapshot(File file, int row, byte[] src, int offset, int length) {
        try (var buffer = new BufferedWriter(new FileWriter(file))) {
            var sb = new StringBuilder();
            length = Math.min(offset + length, src.length);
            for (int i = offset; i < length; i++) {
                sb.append("0x").append(toHexStr(src[i]));
                var j = (i - offset) + 1;
                if ((j % row == 0) || j == length) {
                    sb.append("\r\n");
                    buffer.write(sb.toString());
                    sb.delete(0, sb.length());
                } else {
                    sb.append(",");
                }
            }
            buffer.flush();
        } catch (Exception e) {
            log.fatal("Snapshot memory view fail.", e);
        }
    }
}
