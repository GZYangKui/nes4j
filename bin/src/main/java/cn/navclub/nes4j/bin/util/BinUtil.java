package cn.navclub.nes4j.bin.util;

public class BinUtil {

    public static byte int8(int value) {
        return (byte) value;
    }

    /**
     * 将字节数组转换为二进制
     */
    public static String toBinStr(byte value) {
        var sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append((value & (1 << i)) != 0 ? 1 : 0);
        }
        return sb.reverse().toString();
    }

    /**
     * 将字节数组转换为整形
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

    public static int uint8(byte b) {
        return (b & 0xff);
    }

    public static int uint16(int i) {
        return i & 0xffff;
    }
}
