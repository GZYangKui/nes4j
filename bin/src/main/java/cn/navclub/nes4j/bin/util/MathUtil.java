package cn.navclub.nes4j.bin.util;

/**
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class MathUtil {

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

    public static int u8add(int a, int b) {
        return (a + b) & 0xff;
    }

    public static int u8sbc(int a, int b) {
        return (a - b) & 0xff;
    }

    public record Mathematics(boolean carry, byte result, boolean overflow) {

    }
}
