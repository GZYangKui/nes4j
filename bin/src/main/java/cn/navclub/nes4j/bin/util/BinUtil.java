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


    public static int u8add(int a, int b) {
        return uint8(a + b);
    }

    public static int u8sbc(int a, int b) {
        return (a - b) & 0xff;
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

    public static int uint8(byte b) {
        return (b & 0xff);
    }

    public static int uint8(int b) {
        return b & 0xff;
    }


    public static int uint16(int i) {
        return i & 0xffff;
    }

    public static void snapshot(File file, int row, byte[] src, int offset) {
        try (var os = new FileOutputStream(file)) {
            snapshot(os, row, src, offset, src.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Snapshot target memory view
     */
    public static void snapshot(OutputStream outputStream, int row, byte[] src, int offset, int length) throws IOException {
        if (offset + length > src.length) {
            throw new ArrayIndexOutOfBoundsException("Out of array length.");
        }
        var endSymbol = offset + length;
        var buffer = new OutputStreamWriter(outputStream);
        var h = length / 16 + ((length) % 16 != 0 ? 1 : 0);
        var sb = new StringBuilder("|");
        for (int i = 0; i < h; i++) {
            var oft = (i * row + offset);
            var tmp = endSymbol - oft;
            var k = Math.min(tmp, row);
            sb.delete(1, sb.length());
            buffer.append(BinUtil.toHexStr(offset + i * row)).append(" ");
            for (int j = 0; j < 16; j++) {
                final byte value;
                if (j < k) {
                    value = src[oft + j];
                    buffer.append(BinUtil.toHexStr(value)).append(" ");
                } else {
                    value = 0;
                    buffer.append(". ");
                }
                sb.append(toVisualChar(value));
            }
            sb.append("|");
            buffer.append(sb);
            buffer.append("\r\n");
        }
        buffer.flush();
    }

    public static char toVisualChar(byte b) {
        if (!(b > 31 && b < 127)) {
            return '.';
        }
        return (char) b;
    }
}
