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

    public static String toHexStr(int b) {
        var sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(toHexStr((byte) (b >> (24 - i * 8))));
        }
        return sb.toString();
    }

    public static int uint8(int b) {
        return (b & 0xff);
    }


    public static int uint16(int i) {
        return i & 0xffff;
    }

    /**
     * Snapshot target memory view
     */
    public static void snapshot(File file, int row, byte[] src, int offset) {
        var l = src.length;
        if (offset >= l) {
            throw new ArrayIndexOutOfBoundsException("Out of array length.");
        }
        try (var buffer = new BufferedWriter(new FileWriter(file))) {
            var h = ((l - offset) / 16) + ((l - offset) % 16 != 0 ? 1 : 0);
            var sb = new StringBuilder("|");
            for (int i = 0; i < h; i++) {
                var oft = (i * row + offset);
                var tmp = l - oft;
                var k = Math.min(tmp, row);
                sb.delete(1, sb.length());
                buffer.append(BinUtil.toHexStr(i * row)).append(" ");
                for (int j = 0; j < k; j++) {
                    var b = src[oft + j];
                    buffer.append(BinUtil.toHexStr(src[oft + j])).append(" ");
                    sb.append(toVisualChar(b));
                }
                sb.append("|");
                buffer.append(sb);
                buffer.append("\r\n");
            }
            buffer.flush();
        } catch (Exception e) {
            log.fatal("Snapshot memory view fail.", e);
        }
    }

    public static char toVisualChar(byte b) {
        if (b >= 0 && b < 32 || b == 127) {
            return '.';
        }
        return (char) b;
    }
}
