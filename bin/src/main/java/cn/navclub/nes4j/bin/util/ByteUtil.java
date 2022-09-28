package cn.navclub.nes4j.bin.util;

public class ByteUtil {

    public static byte overflow(int value) {
        if (-128 > value || value > 255) {
            throw new IllegalArgumentException("Byte legal range in 0...255.");
        }
//        final byte b;
//        if (value > 127) {
//            b = (byte) (value - 255);
//        } else {
//            b = (byte) value;
//        }
        return (byte) value;
    }


    public static byte[] toByteArray(int[] arr) {
        var temp = new byte[arr.length];
        for (int i = 0; i < arr.length; i++) {
            temp[i] = overflow(arr[i]);
        }
        return temp;
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
}
