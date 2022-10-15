package cn.navclub.nes4j.bin.util;

public class ByteUtil {

    public static byte overflow(int value) {
        if (-128 > value || value > 255) {
            throw new IllegalArgumentException("Byte legal range in 0...255.");
        }
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

    /**
     * 将反码还原为原码
     */
    public static byte origin(int b) {
        if ((b & 0x80) == 0) {
            return (byte) b;
        }
        var find = false;
        for (int i = 0; i < 8; i++) {
            var bit = (b & 1 << i) != 0 ? 1 : 0;
            if (find) {
                if (bit == 0) {
                    b |= (1 << i);
                } else {
                    b &= (int) (0xff - Math.pow(2, i));
                }
            } else {
                find = bit == 1;
            }
        }
        return (byte) b;
    }

    /**
     * 判断某个字节是否负数
     */
    public static boolean negative(int b) {
        return (b & 0x80) != 0;
    }

    /**
     * 实现二进制减法 a-b=m
     */
    public static Mathematics subtract(int a, int b, boolean borrow) {
        byte value = 0;
        for (int i = 0; i < 8; i++) {
            var aa = (a & 1 << i) / (1 << i);
            var bb = (b & 1 << i) / (1 << i);
            aa = (borrow ? aa - 1 : aa);
            borrow = aa < bb;
            if (borrow) {
                aa = 2 + aa;
            }
            value |= (aa - bb) << i;
        }
        return new Mathematics(borrow, value, ((b ^ value) & (value ^ a) & 0x80) != 0);
    }

    /**
     * 实现二进制加法 a+b=m
     */
    public static Mathematics addition(int a, int b, boolean carry) {
        byte value = 0;
        var l = carry ? 1 : 0;
        for (int i = 0; i < 8; i++) {
            var aa = (a & 1 << i) / (1 << i);
            var bb = (b & 1 << i) / (1 << i);
            var c = aa + bb + l;
            //进位
            if (c >= 2) {
                l = c - 1;
                c = 0;
            } else {
                if (l > 0) {
                    l -= 1;
                }
            }
            value |= (c << i);
        }
        return new Mathematics(l > 0, value, ((b ^ value) & (value ^ a) & 0x80) != 0);
    }

    public static byte fixBit(byte b, int i) {
        return fixBit(b, i, (byte) 1, (byte) 0);
    }

    public static byte fixBit(byte b, int i, byte ifPresent, byte ifAbsent) {
        return (b & (1 << i)) != 0 ? ifPresent : 0;
    }

    public static int toInt16(byte[] arr) {
        return arr[1] << 8 | arr[0] & 0xff;
    }

    public record Mathematics(boolean carry, byte result, boolean overflow) {

    }
}
