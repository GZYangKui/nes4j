package cn.navclub.nes4j.bin.util;

public class ByteUtil {

    public static byte overflow(int value) {
        if (-128 > value || value > 255) {
            throw new IllegalArgumentException("Byte legal range in 0...255.");
        }
        final byte b;
        if (value > 127) {
            b = (byte) (value - 255);
        } else {
            b = (byte) value;
        }
        return b;
    }


    public static byte[] toByteArray(int[] arr) {
        var temp = new byte[arr.length];
        for (int i = 0; i < arr.length; i++) {
            temp[i] = overflow(arr[i]);
        }
        return temp;
    }

}
